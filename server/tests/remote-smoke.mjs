// Explicit remote integration test: node tests/remote-smoke.mjs
// Uses the public key and real anonymous JWTs. Cleans up only accounts tagged with this run UUID.
import { readFile, mkdir, writeFile, unlink } from 'node:fs/promises';
import { randomUUID } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { join } from 'node:path';
import assert from 'node:assert/strict';

const config=Object.fromEntries((await readFile(new URL('../.env.local',import.meta.url),'utf8')).split(/\r?\n/).filter(x=>x&&!x.startsWith('#')).map(x=>[x.slice(0,x.indexOf('=')),x.slice(x.indexOf('=')+1)]));
const run=randomUUID(), users=[], installations=[];
const artifactDir=new URL('../artifacts/',import.meta.url);
await mkdir(artifactDir,{recursive:true});
const registry=new URL(`remote-smoke-${run}.json`,artifactDir);
const remember=()=>writeFile(registry,JSON.stringify({run,users,installations}));
async function request(path,body,session){
  const response=await fetch(config.SUPABASE_URL+path,{method:'POST',headers:{apikey:config.SUPABASE_PUBLISHABLE_KEY,'Content-Type':'application/json',...(session?{Authorization:`Bearer ${session.access_token}`}:{})},body:JSON.stringify(body),signal:AbortSignal.timeout(15000)});
  const raw=await response.text();let data;try{data=JSON.parse(raw)}catch{data=raw}
  return {status:response.status,data};
}
async function rpc(name,body,session){const r=await request('/rest/v1/rpc/'+name,body,session);assert.ok([200,204].includes(r.status),`${name}: HTTP ${r.status} (${r.data?.code??'no code'})`);return r.data;}
async function signup(){const r=await request('/auth/v1/signup',{data:{pleos_test_run:run}});assert.equal(r.status,200,`anonymous signup HTTP ${r.status}`);users.push(r.data.user.id);await remember();return r.data;}
try {
  const a=await signup(), b=await signup();
  console.log('PASS anonymous sign-in: two independent users');
  const register=(app,s)=>rpc('pleos_register_installation',{p_app:app,p_vehicle_id:randomUUID(),p_consent_version:'integration-test-only',p_simulated:true},s);
  const ch=await register('CHACHELIN',a);installations.push(ch);await remember();
  const cr=await register('CRONG',a);installations.push(cr);await remember();
  const query={p_lat:37.4127,p_lon:127.0947,p_radius_m:10000,p_demo:true};
  const before=(await rpc('pleos_nearby_stations',query,a)).find(x=>x.id==='st-003');
  const payload={p_installation_id:ch,p_id:randomUUID(),p_station_id:'st-003',p_started_at:new Date(Date.now()-3600000).toISOString(),p_ended_at:new Date(Date.now()-1800000).toISOString(),p_soc_start:40,p_soc_end:60,p_avg_kw:30,p_result:'SUCCESS',p_time_scale:120};
  await rpc('pleos_upload_charge',payload,a);await rpc('pleos_upload_charge',payload,a);
  assert.equal((await rpc('pleos_my_charges',{p_installation_id:ch},a)).length,1);
  await rpc('pleos_submit_review',{p_installation_id:ch,p_session_id:payload.p_id,p_rating:5,p_tags:['FAST']},a);
  const after=(await rpc('pleos_nearby_stations',query,b)).find(x=>x.id==='st-003');
  assert.equal(Number(after.verified_charges),Number(before.verified_charges)+1);
  assert.equal(Number(after.review_count),Number(before.review_count)+1);
  console.log('PASS charge upload/retry/review and another user sees updated aggregate');
  assert.equal((await request('/rest/v1/rpc/pleos_my_charges',{p_installation_id:ch},b)).status,403);
  assert.ok([401,403].includes((await request('/rest/v1/rpc/pleos_nearby_stations',query)).status));
  console.log('PASS owner isolation and unauthenticated RPC rejection');
  const date=new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Seoul',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
  await rpc('pleos_upload_pass',{p_installation_id:cr,p_segment_id:'wydku4g',p_heading8:0,p_date:date},a);
  const hour=new Date(Math.floor(Date.now()/3600000)*3600000).toISOString();
  await rpc('pleos_upload_disengage',{p_installation_id:cr,p_segment_id:'wydku4g',p_heading8:0,p_hour:hour},a);
  assert.ok((await rpc('pleos_nearby_risk',query,b)).some(x=>x.segment_id==='wydku4g'));
  console.log('PASS cruise upload and shared risk query');
  const refreshed=await request('/auth/v1/token?grant_type=refresh_token',{refresh_token:a.refresh_token});
  assert.equal(refreshed.status,200);Object.assign(a,refreshed.data);
  assert.equal((await rpc('pleos_my_charges',{p_installation_id:ch},a)).length,1);
  console.log('PASS refresh token preserves the same owner');
  await rpc('pleos_withdraw_installation',{p_installation_id:ch},a);
  await rpc('pleos_withdraw_installation',{p_installation_id:ch},a);
  assert.equal((await request('/rest/v1/rpc/pleos_upload_charge',payload,a)).status,403);
  const end=(await rpc('pleos_nearby_stations',query,b)).find(x=>x.id==='st-003');
  assert.equal(Number(end.verified_charges),Number(before.verified_charges));
  console.log('PASS withdrawal/retry blocks late writes and removes contribution');
} finally {
  // Bound to known UUIDs AND the marker placed by this test; no pre-existing accounts are touched.
  if(users.length){
    const ids=users.map(x=>`'${x}'::uuid`).join(',');
    const match=`owner_user_id in (select id from auth.users where id in (${ids}) and is_anonymous=true and raw_user_meta_data->>'pleos_test_run'='${run}')`;
    const owned=`select id from pleos_private.app_installations where ${match}`;
    const sql=`begin;
      delete from pleos_private.charge_sessions where installation_id in (${owned});
      delete from pleos_private.charge_retractions where installation_id in (${owned});
      delete from pleos_private.cruise_events where installation_id in (${owned});
      delete from pleos_private.cruise_passes where installation_id in (${owned});
      delete from pleos_private.app_installations where ${match};
      delete from auth.users where id in (${ids}) and is_anonymous=true and raw_user_meta_data->>'pleos_test_run'='${run}';
      commit;`;
    const cleanup=new URL(`remote-cleanup-${run}.sql`,artifactDir);await writeFile(cleanup,sql);
    execFileSync(join(process.env.LOCALAPPDATA,'hyundai-dev/supabase/supabase.exe'),['db','query','--linked','--project-ref','qbdikckaqroxbpacvtqo','--file',cleanup.pathname.replace(/^\/([A-Za-z]:)/,'$1')],{stdio:['ignore','pipe','pipe']});
    await unlink(cleanup);console.log('PASS cleanup: only this run\'s temporary users and records removed');
  }
  await unlink(registry);
}
