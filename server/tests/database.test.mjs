import { PGlite } from '@electric-sql/pglite';
import { readFile } from 'node:fs/promises';
import test from 'node:test';
import assert from 'node:assert/strict';

const migration = await readFile(new URL('../supabase/migrations/20261102000100_initial_schema.sql', import.meta.url), 'utf8');
const A = '10000000-0000-4000-8000-000000000001';
const B = '10000000-0000-4000-8000-000000000002';
const S = '20000000-0000-4000-8000-000000000001';
const S2 = '20000000-0000-4000-8000-000000000002';
const V = '30000000-0000-4000-8000-000000000001';
let db;
const scalar = async (sql, args=[]) => Object.values((await db.query(sql,args)).rows[0])[0];
const asUser = async (id, fn, role='authenticated') => {
  await db.exec(`set role ${role}`);
  await db.query(`select set_config('request.jwt.claim.sub',$1,false)`,[id ?? '']);
  try { return await fn(); } finally { await db.exec('reset role'); }
};
const register = (app='CHACHELIN',vehicle=V) => scalar(`select public.pleos_register_installation($1,$2,'test-v1',true)`,[app,vehicle]);
const charge = (id,session=S,result='SUCCESS',kw=30) => db.query(`select public.pleos_upload_charge($1,$2,'st-001',
  now()-interval '1 hour',now()-interval '30 minutes',40::smallint,60::smallint,$3,$4,120)`,[id,session,kw,result]);

test.before(async () => {
  db=new PGlite();
  await db.exec(`create role anon; create role authenticated; create schema auth;
    create table auth.users(id uuid primary key); insert into auth.users values ('${A}'),('${B}');
    create function auth.uid() returns uuid language sql stable as $$ select nullif(current_setting('request.jwt.claim.sub',true),'')::uuid $$;
    grant usage on schema auth to anon,authenticated; grant execute on function auth.uid() to anon,authenticated;`);
  await db.exec(migration);
  await db.exec(`insert into pleos_private.stations(id,name,operator,latitude,longitude,max_kw,source)
    values('st-001','Test station','Test operator',37.4127,127.0947,100,'SEED')`);
});
test.after(async () => { await db?.close(); });

test('migration applies once, is transactional, and raw tables are all protected', async () => {
  assert.equal(await scalar(`select count(*) from pleos_private.migration_versions`),1);
  assert.equal(await scalar(`select count(*) from pg_class c join pg_namespace n on n.oid=c.relnamespace where n.nspname='pleos_private' and relkind='r' and not relrowsecurity`),0);
  await assert.rejects(db.exec(migration));
  await db.exec('rollback');
  assert.equal(await scalar(`select count(*) from pleos_private.migration_versions`),1);
});
test('unauthenticated users and PUBLIC cannot call client RPCs or read raw data', async () => {
  await asUser(null,async()=>{
    await assert.rejects(register());
    await assert.rejects(db.query(`select * from pleos_private.stations`));
  },'anon');
  await asUser(null,async()=>await assert.rejects(register()));
  assert.equal(await scalar(`select count(*) from pg_proc p join pg_namespace n on n.oid=p.pronamespace,
    lateral aclexplode(coalesce(p.proacl,acldefault('f',p.proowner))) a
    where n.nspname in ('pleos_private','public') and (n.nspname='pleos_private' or p.proname like 'pleos_%') and a.grantee=0`),0);
});
test('registration is idempotent and bound to JWT owner and app', async () => {
  const id=await asUser(A,register);
  assert.equal(await asUser(A,register),id);
  assert.notEqual(await asUser(B,register),id);
  await asUser(B,async()=>await assert.rejects(charge(id)));
  const cruise=await asUser(A,()=>register('CRUISEBREAKER'));
  await asUser(A,async()=>await assert.rejects(charge(cruise)));
  await asUser(A,async()=>await assert.rejects(db.query(`select * from pleos_private.app_installations`)));
});
test('charge upload deduplicates a lost-response retry and rejects changed payloads', async () => {
  const id=await asUser(A,register);
  await asUser(A,async()=>{
    // Use stable timestamps as a real retry does.
    const args=[id,S,'st-001','2026-10-31T00:00:00Z','2026-10-31T00:30:00Z',40,60,30,'SUCCESS',120];
    const sql=`select public.pleos_upload_charge($1,$2,$3,$4,$5,$6::smallint,$7::smallint,$8,$9,$10)`;
    await db.query(sql,args); await db.query(sql,args);
    await assert.rejects(db.query(sql,args.map((v,k)=>k===7?31:v)));
    assert.equal((await scalar(`select public.pleos_my_charges($1)`,[id])).length,1);
  });
});
test('one review per successful own session; duplicates normalized and failed-session reviews denied', async () => {
  const id=await asUser(A,register);
  await asUser(A,async()=>{
    await db.query(`select public.pleos_submit_review($1,$2,5::smallint,array['FAST','FAST'])`,[id,S]);
    await db.query(`select public.pleos_submit_review($1,$2,5::smallint,array['FAST'])`,[id,S]);
    await assert.rejects(db.query(`select public.pleos_submit_review($1,$2,0::smallint,array['FAST'])`,[id,S]));
    await charge(id,S2,'FAILED',null);
    await assert.rejects(db.query(`select public.pleos_submit_review($1,$2,5::smallint,'{}'::text[])`,[id,S2]));
  });
  assert.equal(await scalar(`select count(*) from pleos_private.reviews where installation_id=$1`,[id]),1);
});
test('failure retraction is idempotent when absent and blocks a late upload', async () => {
  const id=await asUser(A,register);
  const session='20000000-0000-4000-8000-000000000003';
  await asUser(A,async()=>{
    await db.query(`select public.pleos_retract_failure($1,$2)`,[id,S2]);
    await db.query(`select public.pleos_retract_failure($1,$2)`,[id,session]);
    await db.query(`select public.pleos_retract_failure($1,$2)`,[id,session]);
    await charge(id,session,'FAILED',null);
    await assert.rejects(db.query(`select public.pleos_retract_failure($1,$2)`,[id,S]));
    assert.equal((await scalar(`select public.pleos_my_charges($1)`,[id])).length,1);
  });
  assert.equal(await scalar(`select count(*) from pleos_private.charge_sessions where installation_id=$1 and id=$2`,[id,session]),0);
});
test('data bounds, invalid tags, simulated/live separation and future dates are enforced', async () => {
  const cruise=await asUser(A,()=>register('CRUISEBREAKER'));
  await asUser(A,async()=>{
    await assert.rejects(db.query(`select public.pleos_upload_pass($1,'wydku4g',8::smallint,current_date)`,[cruise]));
    await assert.rejects(db.query(`select public.pleos_upload_pass($1,'invalid',0::smallint,current_date)`,[cruise]));
    await assert.rejects(db.query(`select public.pleos_upload_pass($1,'wydku4g',0::smallint,current_date+10)`,[cruise]));
    await assert.rejects(db.query(`select public.pleos_upload_disengage($1,'wydku4g',0::smallint,'2026-10-30T00:00:01Z')`,[cruise]));
    await assert.rejects(db.query(`select public.pleos_nearby_risk(37,127,'NaN'::float8,true)`));
    assert.deepEqual(await scalar(`select public.pleos_nearby_stations(37.4127,127.0947,10000,false)`),[]);
  });
});
test('geohash encoding, center and eight neighbors match independent grid vectors', async () => {
  assert.equal(await scalar(`select pleos_private.geohash_encode(37.4127,127.0947)`),'wydku4g');
  const neighbors=await scalar(`select pleos_private.neighbors('wydku4g')`);
  assert.equal(new Set(neighbors).size,9);
  assert.equal(neighbors[0],'wydku4g');
  for(const h of neighbors){
    const b=await scalar(`select pleos_private.geohash_bounds($1)`,[h]);
    assert.equal(await scalar(`select pleos_private.geohash_encode($1,$2)`,[(b[0]+b[1])/2,(b[2]+b[3])/2]),h);
  }
});
test('disengage atomically supplies pass; KST dates and retry deduplication are correct', async () => {
  const id=await asUser(A,()=>register('CRUISEBREAKER'));
  await asUser(A,async()=>{
    const sql=`select public.pleos_upload_disengage($1,'wydku4g',0::smallint,'2026-10-30T15:00:00Z')`;
    await db.query(sql,[id]); await db.query(sql,[id]);
  });
  assert.equal(await scalar(`select event_date::text from pleos_private.cruise_events where installation_id=$1`,[id]),'2026-10-31');
  assert.equal(await scalar(`select count(*) from pleos_private.cruise_passes where installation_id=$1 and pass_date='2026-10-31'`,[id]),1);
});
test('9-cell risk counts installation-days, not rows; direction and day-window excluded', async () => {
  const hash='wydku4g';
  const around=await scalar(`select pleos_private.neighbors($1)`,[hash]);
  for(const h of around) await db.query(`select pleos_private.ensure_segment($1)`,[h]);
  const today=await scalar(`select (now() at time zone 'Asia/Seoul')::date::text`);
  // 20 distinct seed installations, three adjacent pass rows each; six disengage days.
  for(let k=0;k<20;k++){
    const id=await scalar(`insert into pleos_private.app_installations(app,vehicle_hash,data_source,consent_version)
      values('CRUISEBREAKER',encode(sha256(convert_to($1,'UTF8')),'hex'),'SEED','seed-v1') returning id`,[`risk-test-${k}`]);
    for(const h of around.slice(0,3)) await db.query(`insert into pleos_private.cruise_passes(installation_id,segment_id,heading8,pass_date) values($1,$2,2,$3)`,[id,h,today]);
    if(k<6) for(const h of around.slice(0,2)) await db.query(`insert into pleos_private.cruise_events(installation_id,segment_id,heading8,occurred_hour)
      values($1,$2,2,($3::date::timestamp at time zone 'Asia/Seoul')+interval '1 hour')`,[id,h,today]);
  }
  const result=await asUser(A,()=>scalar(`select public.pleos_nearby_risk(37.4127,127.0947,1000,true)`));
  const r=result.find(x=>x.segment_id===hash&&x.heading8===2);
  assert.equal(r.pass_vehicles,20); assert.equal(r.disengages,6); assert.equal(Number(r.rate),0.3); assert.equal(r.is_risk,true);
  assert.ok(!JSON.stringify(r).includes('installation_id'));
  assert.deepEqual(await asUser(A,()=>scalar(`select public.pleos_nearby_risk(37.4127,127.0947,1000,false)`)),[]);
});
test('withdraw cascades reviews, deletes records and receipts, rejects late writes and re-registration', async () => {
  const id=await asUser(A,register);
  const other=await asUser(B,register);
  await asUser(A,async()=>{
    await db.query(`select public.pleos_withdraw_installation($1)`,[id]);
    await db.query(`select public.pleos_withdraw_installation($1)`,[id]);
    await assert.rejects(charge(id)); await assert.rejects(register());
  });
  for(const table of ['charge_sessions','reviews','charge_retractions']) assert.equal(await scalar(`select count(*) from pleos_private.${table} where installation_id=$1`,[id]),0);
  assert.equal(await scalar(`select state from pleos_private.app_installations where id=$1`,[other]),'ACTIVE');
  await asUser(B,async()=>await assert.rejects(db.query(`select public.pleos_withdraw_installation($1)`,[id])));
});

test('RLS still isolates owners if a future read-only table grant is added', async () => {
  await db.exec(`grant usage on schema pleos_private to authenticated;
    grant select on pleos_private.app_installations,pleos_private.charge_sessions,pleos_private.reviews to authenticated;`);
  try {
    const b=await asUser(B,()=>scalar(`select count(*) from pleos_private.app_installations`));
    assert.equal(b,1);
    assert.equal(await asUser(B,()=>scalar(`select count(*) from pleos_private.charge_sessions`)),0);
  } finally {
    await db.exec(`revoke usage on schema pleos_private from authenticated;
      revoke select on pleos_private.app_installations,pleos_private.charge_sessions,pleos_private.reviews from authenticated;`);
  }
});

test('history keyset cursor retains records with equal timestamps', async () => {
  const vehicle='30000000-0000-4000-8000-000000000002';
  const id=await asUser(A,()=>register('CHACHELIN',vehicle));
  await asUser(A,async()=>{
    for(let k=1;k<=3;k++) await db.query(`select public.pleos_upload_charge($1,$2,'st-001','2026-10-30T00:00:00Z','2026-10-30T00:30:00Z',20::smallint,40::smallint,40,'SUCCESS',120)`,
      [id,`40000000-0000-4000-8000-00000000000${k}`]);
    const first=await scalar(`select public.pleos_my_charges($1,2)`,[id]);
    const last=first.at(-1);
    const second=await scalar(`select public.pleos_my_charges($1,2,$2,$3)`,[id,last.ended_at,last.id]);
    assert.equal(new Set([...first,...second].map(x=>x.id)).size,3);
  });
});

test('risk window includes KST today-27 and excludes today-28, and directions do not mix', async () => {
  const id=await asUser(A,()=>register('CRUISEBREAKER','30000000-0000-4000-8000-000000000003'));
  await asUser(A,async()=>{
    await db.query(`select public.pleos_upload_pass($1,'wydku4g',5::smallint,(now() at time zone 'Asia/Seoul')::date-27)`,[id]);
    await db.query(`select public.pleos_upload_pass($1,'wydku4g',5::smallint,(now() at time zone 'Asia/Seoul')::date-28)`,[id]);
    await db.query(`select public.pleos_upload_pass($1,'wydku4g',6::smallint,(now() at time zone 'Asia/Seoul')::date-27)`,[id]);
  });
  const result=await asUser(A,()=>scalar(`select public.pleos_nearby_risk(37.4127,127.0947,1000,true)`));
  for(const dir of [5,6]) assert.equal(result.find(x=>x.segment_id==='wydku4g'&&x.heading8===dir).pass_vehicles,1);
});

test('seed migration is replayable: exactly 10 demo stations, 300 sessions, 400 events, 2000 passes', async () => {
  // Separate empty DB for deterministic seed counts without other tests' synthetic rows.
  const seeded=new PGlite();
  try {
    await seeded.exec(`create role anon;create role authenticated;create schema auth;create table auth.users(id uuid primary key);
      create function auth.uid() returns uuid language sql stable as $$select nullif(current_setting('request.jwt.claim.sub',true),'')::uuid$$;`);
    await seeded.exec(migration);
    const seed=await readFile(new URL('../supabase/migrations/20261102000200_demo_data.sql',import.meta.url),'utf8');
    await seeded.exec(seed); await seeded.exec(seed);
    for(const [table,count] of [['stations',10],['charge_sessions',300],['cruise_events',400],['cruise_passes',2000]]) {
      assert.equal((await seeded.query(`select count(*)::int n from pleos_private.${table}`)).rows[0].n,count);
    }
    await seeded.exec(`insert into auth.users values('${A}'); select set_config('request.jwt.claim.sub','${A}',false);`);
    const rows=(await seeded.query(`select public.pleos_nearby_stations(37.4127,127.0947,10000,true) data`)).rows[0].data;
    assert.equal(rows.length,10); assert.equal(rows.reduce((n,s)=>n+Number(s.verified_charges),0),258);
    assert.ok(rows.every(s=>Number(s.chachelin_score)===Number(s.avg_rating)));
    const detail=(await seeded.query(`select public.pleos_station_detail('st-003',true) data`)).rows[0].data;
    assert.equal(detail.rating_counts.length,5);
    assert.equal(detail.rating_counts.reduce((a,b)=>a+b,0),Number(detail.station.review_count));
    assert.ok(detail.recent_failure_hour_ms.every(t=>t%3600000===0));
    const risk=(await seeded.query(`select public.pleos_nearby_risk(37.4127,127.0947,10000,true) data`)).rows[0].data;
    assert.equal(risk.length,4);assert.ok(risk.some(r=>r.is_risk));
    await seeded.exec(`update pleos_private.charge_sessions set started_at=started_at-interval '60 days', ended_at=ended_at-interval '60 days';
      update pleos_private.cruise_events set occurred_hour=occurred_hour-interval '60 days';
      update pleos_private.cruise_passes set pass_date=pass_date-60;`);
    const refresh=await readFile(new URL('../supabase/refresh-demo-dates.sql',import.meta.url),'utf8');
    // Client SIMULATED history must not move when the synthetic seed is refreshed.
    await seeded.exec(`insert into pleos_private.app_installations(id,owner_user_id,app,vehicle_hash,data_source,consent_version)
      values('${V}','${A}','CHACHELIN',repeat('a',64),'SIMULATED','test');
      insert into pleos_private.charge_sessions(installation_id,id,station_id,started_at,ended_at,soc_start,soc_end,result)
      values('${V}','${S}','st-001','2026-01-01T00:00Z','2026-01-01T00:30Z',20,40,'SUCCESS');`);
    const before=(await seeded.query(`select ended_at::text t from pleos_private.charge_sessions where installation_id='${V}'`)).rows[0].t;
    await seeded.exec(refresh);
    const refreshed=(await seeded.query(`select public.pleos_nearby_risk(37.4127,127.0947,10000,true) data`)).rows[0].data;
    assert.deepEqual(refreshed,risk);
    const failures=(await seeded.query(`select public.pleos_station_detail('st-008',true) data`)).rows[0].data;
    assert.ok(failures.recent_failure_hour_ms.length>0);
    const times=(await seeded.query(`select id,ended_at::text from pleos_private.charge_sessions order by installation_id,id`)).rows;
    await seeded.exec(refresh);
    assert.deepEqual((await seeded.query(`select id,ended_at::text from pleos_private.charge_sessions order by installation_id,id`)).rows,times);
    assert.equal((await seeded.query(`select ended_at::text t from pleos_private.charge_sessions where installation_id='${V}'`)).rows[0].t,before);
    for(const [table,count] of [['cruise_events',400],['cruise_passes',2000]])
      assert.equal((await seeded.query(`select count(*)::int n from pleos_private.${table}`)).rows[0].n,count);
    const status=(await seeded.query(await readFile(new URL('../supabase/demo-status.sql',import.meta.url),'utf8'))).rows[0].demo_status;
    assert.equal(status.seed_passes_28d,2000); assert.equal(status.seed_events_28d,400); assert.ok(status.seed_failures_7d>0);
    // A partial/customized seed fails closed.
    await seeded.exec(`delete from pleos_private.cruise_events where ctid=(select ctid from pleos_private.cruise_events limit 1)`);
    await assert.rejects(seeded.exec(refresh),/Demo seed differs/);
    await seeded.exec('rollback');
    await seeded.exec(await readFile(new URL('../supabase/preflight.sql',import.meta.url),'utf8'));
    await seeded.exec(await readFile(new URL('../supabase/verify.sql',import.meta.url),'utf8'));
  } finally { await seeded.close(); }
});

const freshDb = async () => {
  const fresh=new PGlite();
  await fresh.exec(`create role anon;create role authenticated;create schema auth;create table auth.users(id uuid primary key);
    create function auth.uid() returns uuid language sql stable as $$select nullif(current_setting('request.jwt.claim.sub',true),'')::uuid$$;`);
  await fresh.exec(migration);
  return fresh;
};
const sql = name => readFile(new URL(`../supabase/${name}`,import.meta.url),'utf8');
const nearby = async conn => (await conn.query(`select public.pleos_nearby_stations(37.4127,127.0947,10000,true) data`)).rows[0].data;

test('demo rating variation separates ten averages and preserves ranking reversal and client reviews', async () => {
  const fresh = await freshDb();
  try {
    const variation = await sql('migrations/20261109000100_demo_rating_variation.sql');
    await fresh.exec(variation);
    assert.equal((await fresh.query(`select count(*)::int n from pleos_private.migration_versions where version='20261109000100'`)).rows[0].n, 0);
    await fresh.exec(await sql('migrations/20261102000200_demo_data.sql'));
    await fresh.exec(await sql('migrations/20261103000100_score_failure_penalty.sql'));
    await fresh.exec(await sql('migrations/20261103000200_demo_failure_station.sql'));
    await fresh.exec(`insert into auth.users values('${A}'); select set_config('request.jwt.claim.sub','${A}',false);
      insert into pleos_private.app_installations(id,owner_user_id,app,vehicle_hash,data_source,consent_version)
      values('${V}','${A}','CHACHELIN',repeat('a',64),'SIMULATED','test');
      insert into pleos_private.charge_sessions(installation_id,id,station_id,started_at,ended_at,soc_start,soc_end,result)
      values('${V}','${S}','st-001',now()-interval '30 minutes',now(),20,40,'SUCCESS');
      insert into pleos_private.reviews(installation_id,session_id,rating,tags) values('${V}','${S}',5,array['CLEAN']);`);
    const sessionDigest = async () => (await fresh.query(`select md5(string_agg(row_to_json(c)::text,'' order by installation_id,id)) digest from pleos_private.charge_sessions c`)).rows[0].digest;
    const originalSessions = await sessionDigest();
    await fresh.exec(`begin; delete from pleos_private.reviews where session_id=md5('pleos-demo-charge-session-1')::uuid;`);
    await assert.rejects(fresh.exec(variation), /Original demo reviews are incomplete/);
    await fresh.exec('rollback');
    await fresh.exec(variation);
    const rows = await nearby(fresh);
    assert.equal(rows.length, 10);
    assert.equal(new Set(rows.map(row => Number(row.avg_rating).toFixed(1))).size, 10);
    assert.ok(Math.max(...rows.map(row => Number(row.avg_rating))) - Math.min(...rows.map(row => Number(row.avg_rating))) > 1);
    assert.equal(rows.at(-1).id, 'st-004');
    assert.equal(rows.find(row => Number(row.avg_rating) === Math.max(...rows.map(item => Number(item.avg_rating)))).id, 'st-004');
    assert.equal(await sessionDigest(), originalSessions);
    assert.equal((await fresh.query(`select rating from pleos_private.reviews where installation_id='${V}'`)).rows[0].rating, 5);
    assert.equal((await fresh.query(`select count(*)::int n from pleos_private.reviews r join pleos_private.app_installations i on i.id=r.installation_id where i.data_source='SEED'`)).rows[0].n, 268);
    await fresh.exec(variation);
    assert.deepEqual(await nearby(fresh), rows);
    await fresh.exec(await sql('refresh-demo-dates.sql'));
    assert.deepEqual((await nearby(fresh)).map(row => Number(row.avg_rating)), rows.map(row => Number(row.avg_rating)));
  } finally { await fresh.close(); }
});

test('score = stars x (1 - 30-day failure rate); under 5 sessions no penalty; retracted and old rows excluded', async () => {
  const fresh=await freshDb();
  try {
    await fresh.exec(await sql('migrations/20261103000100_score_failure_penalty.sql'));
    await fresh.exec(await sql('migrations/20261103000100_score_failure_penalty.sql')); // re-runnable
    await fresh.exec(`insert into auth.users values('${A}'); select set_config('request.jwt.claim.sub','${A}',false);
      insert into pleos_private.stations(id,name,operator,latitude,longitude,max_kw,source) values
        ('st-a','A','t',37.4127,127.0947,100,'SEED'),('st-b','B','t',37.4127,127.0947,100,'SEED'),('st-c','C','t',37.4127,127.0947,100,'SEED');
      insert into pleos_private.app_installations(id,app,vehicle_hash,data_source,consent_version)
        values('${V}','CHACHELIN',repeat('b',64),'SEED','test');`);
    // st-a: 6 rated charges (4 stars) + 2 failures + 1 retracted failure + 1 failure 40 days ago -> rate 2/8
    // st-b: 5 rated charges (5 stars), 0 recent sessions besides them, 0 failures -> 5
    // st-c: 4 rated charges only -> null ("평가 중")
    let n=0; const add = (station,result,days,rating=null,state='VALID') => { n++; const id=`00000000-0000-4000-8000-${String(n).padStart(12,'0')}`;
      return `insert into pleos_private.charge_sessions(installation_id,id,station_id,started_at,ended_at,soc_start,soc_end,avg_kw,result,record_state)
          values('${V}','${id}','${station}',now()-interval '${days} days 1 hour',now()-interval '${days} days',20,${result==='SUCCESS'?60:20},${result==='SUCCESS'?50:'null'},'${result}','${state}');`
        + (rating ? `insert into pleos_private.reviews(installation_id,session_id,rating) values('${V}','${id}',${rating});` : ''); };
    const rows=[];
    for (let i=0;i<6;i++) rows.push(add('st-a','SUCCESS',i+1,4));
    rows.push(add('st-a','FAILED',2),add('st-a','FAILED',3),add('st-a','FAILED',4,null,'RETRACTED'),add('st-a','FAILED',40));
    for (let i=0;i<5;i++) rows.push(add('st-b','SUCCESS',i+1,5));
    for (let i=0;i<4;i++) rows.push(add('st-c','SUCCESS',i+1,3));
    await fresh.exec(rows.join('\n'));
    const byId=Object.fromEntries((await nearby(fresh)).map(s=>[s.id,s]));
    assert.equal(Number(byId['st-a'].recent_sessions_30d),8); assert.equal(Number(byId['st-a'].recent_failures_30d),2);
    assert.ok(Math.abs(Number(byId['st-a'].chachelin_score)-4*(1-2/8))<1e-9);
    assert.equal(Number(byId['st-b'].chachelin_score),5);
    assert.equal(byId['st-c'].chachelin_score,null);
    assert.deepEqual((await nearby(fresh)).map(s=>s.id),['st-b','st-a','st-c']);
    // Under 5 sessions in 30 days: no penalty even with a failure.
    await fresh.exec(`delete from pleos_private.charge_sessions where station_id='st-a' and result='SUCCESS' and ended_at<now()-interval '3 days';`);
    const a=(await nearby(fresh)).find(s=>s.id==='st-a');
    assert.equal(Number(a.recent_sessions_30d),4); assert.equal(Number(a.failure_rate_30d),0);
    assert.equal(a.chachelin_score,null); // reviews dropped under 5 as well
  } finally { await fresh.close(); }
});

test('failure-heavy demo station ranks first by stars but last by score, and date refresh keeps it', async () => {
  const fresh=await freshDb();
  try {
    const demo=await sql('migrations/20261102000200_demo_data.sql');
    const failure=await sql('migrations/20261103000200_demo_failure_station.sql');
    await fresh.exec(failure); // before the base seed: skipped
    assert.equal((await fresh.query(`select count(*)::int n from pleos_private.migration_versions where version='20261103000200'`)).rows[0].n,0);
    await fresh.exec(demo);
    await fresh.exec(await sql('migrations/20261103000100_score_failure_penalty.sql'));
    await fresh.exec(failure); await fresh.exec(failure);
    assert.equal((await fresh.query(`select count(*)::int n from pleos_private.charge_sessions`)).rows[0].n,325);
    await fresh.exec(`insert into auth.users values('${A}'); select set_config('request.jwt.claim.sub','${A}',false);`);
    const rows=await nearby(fresh);
    const st4=rows.find(s=>s.id==='st-004');
    assert.equal(Math.max(...rows.map(s=>Number(s.avg_rating))),Number(st4.avg_rating));
    assert.equal(rows.at(-1).id,'st-004');
    assert.ok(Math.abs(Number(st4.chachelin_score)-Number(st4.avg_rating)*(1-20/55))<1e-9);
    assert.ok(Number(st4.recent_failures_7d)>0);
    // Refresh after aging: counts accepted with the extra owners, ranking unchanged.
    await fresh.exec(`update pleos_private.charge_sessions set started_at=started_at-interval '60 days', ended_at=ended_at-interval '60 days';`);
    await fresh.exec(await sql('refresh-demo-dates.sql'));
    assert.deepEqual((await nearby(fresh)).map(s=>s.id),rows.map(s=>s.id));
  } finally { await fresh.close(); }
});

test('11-07 demo names rename only seed labels, re-runnable, skipped before the base seed', async () => {
  const fresh=await freshDb();
  try {
    const names=await sql('migrations/20261107000100_demo_names.sql');
    await fresh.exec(names); // before the base seed: skipped
    assert.equal((await fresh.query(`select count(*)::int n from pleos_private.migration_versions where version='20261107000100'`)).rows[0].n,0);
    await fresh.exec(await sql('migrations/20261102000200_demo_data.sql'));
    await fresh.exec(names); await fresh.exec(names);
    const seg=(await fresh.query(`select road_name from pleos_private.segments where geohash=pleos_private.geohash_encode(37.4163,127.0947)`)).rows[0];
    assert.equal(seg.road_name,'판교IC 진입로');
    const st=(await fresh.query(`select id,name,operator from pleos_private.stations order by id`)).rows;
    assert.equal(st.find(s=>s.id==='st-005').name,'삼평동 공영주차장 (시연)');
    assert.ok(st.every(s=>s.operator==='시연 데이터'));
    assert.ok(st.every(s=>s.name.endsWith('(시연)')));
    assert.equal(st.find(s=>s.id==='st-003').name,'시흥동 주민센터 (시연)'); // untouched
    assert.equal((await fresh.query(`select count(*)::int n from pleos_private.charge_sessions`)).rows[0].n,300);
  } finally { await fresh.close(); }
});

test('clear-rehearsal-data deletes SIMULATED records only and keeps installations', async () => {
  const fresh=await freshDb();
  try {
    await fresh.exec(await sql('migrations/20261102000200_demo_data.sql'));
    const sim='20000000-0000-4000-8000-000000000002', live='20000000-0000-4000-8000-000000000003';
    await fresh.exec(`insert into auth.users values('${A}');
      insert into pleos_private.app_installations(id,owner_user_id,app,vehicle_hash,data_source,consent_version) values
        ('${sim}','${A}','CHACHELIN',repeat('a',64),'SIMULATED','v1'),('${live}','${A}','CHACHELIN',repeat('b',64),'LIVE','v1');
      insert into pleos_private.charge_sessions(installation_id,id,station_id,started_at,ended_at,soc_start,soc_end,avg_kw,result) values
        ('${sim}',gen_random_uuid(),'st-003',now()-interval '1 hour',now(),40,80,50,'SUCCESS'),
        ('${sim}',gen_random_uuid(),'st-003',now()-interval '1 hour',now(),40,40,null,'FAILED'),
        ('${live}',gen_random_uuid(),'st-003',now()-interval '1 hour',now(),40,80,50,'SUCCESS');`);
    await fresh.exec(`insert into pleos_private.reviews(installation_id,session_id,rating,tags)
      select installation_id,id,5,array['FAST'] from pleos_private.charge_sessions where installation_id='${sim}' and result='SUCCESS'`);
    const clear=await sql('clear-rehearsal-data.sql');
    await fresh.exec(clear); await fresh.exec(clear); // re-runnable
    const count=async w=>(await fresh.query(`select count(*)::int n from pleos_private.charge_sessions c join pleos_private.app_installations i on i.id=c.installation_id where ${w}`)).rows[0].n;
    assert.equal(await count(`i.data_source='SIMULATED'`),0);
    assert.equal(await count(`i.data_source='LIVE'`),1);
    assert.equal(await count(`i.data_source='SEED'`),300);
    assert.equal((await fresh.query(`select count(*)::int n from pleos_private.reviews where installation_id='${sim}'`)).rows[0].n,0);
    assert.equal((await fresh.query(`select count(*)::int n from pleos_private.app_installations where id='${sim}'`)).rows[0].n,1);
  } finally { await fresh.close(); }
});
