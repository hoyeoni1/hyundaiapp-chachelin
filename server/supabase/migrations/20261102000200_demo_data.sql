-- Synthetic demonstration data only. Never represents verified real charging stations/traffic.
-- Explicitly separate from the schema migration; do not run for a live-only deployment.
begin;
select pg_advisory_xact_lock(20261102,1);
do $seed$
begin
if exists(select 1 from pleos_private.migration_versions where version='20261102000200') then return; end if;
insert into pleos_private.stations(id,name,operator,latitude,longitude,max_kw,source) values
  ('st-001','판교 테크노밸리 E-pit (시연)','시연 데이터',37.4020,127.1086,350,'SEED'),
  ('st-002','판교역 공영주차장 (시연)','시연 데이터',37.3947,127.1112,100,'SEED'),
  ('st-003','시흥동 주민센터 (시연)','시연 데이터',37.4127,127.0947,50,'SEED'),
  ('st-004','대왕판교IC 휴게소 (시연)','시연 데이터',37.4180,127.1020,200,'SEED'),
  ('st-005','시연 충전소 05','가상 충전소',37.4100,127.1000,100,'SEED'),
  ('st-006','시연 충전소 06','가상 충전소',37.4080,127.1020,150,'SEED'),
  ('st-007','시연 충전소 07','가상 충전소',37.4060,127.1040,50,'SEED'),
  ('st-008','시연 충전소 08','가상 충전소',37.4140,127.1060,200,'SEED'),
  ('st-009','시연 충전소 09','가상 충전소',37.4160,127.1080,100,'SEED'),
  ('st-010','시연 충전소 10','가상 충전소',37.4000,127.1100,150,'SEED')
on conflict(id) do nothing;

insert into pleos_private.app_installations(id,app,vehicle_hash,data_source,consent_version)
select md5('pleos-demo-charge-owner-'||g)::uuid,'CHACHELIN',encode(sha256(convert_to('pleos-demo-charge-owner-'||g,'UTF8')),'hex'),'SEED','demo-v1'
from generate_series(1,30) g on conflict(id) do nothing;

insert into pleos_private.charge_sessions(installation_id,id,station_id,started_at,ended_at,soc_start,soc_end,avg_kw,result)
select md5('pleos-demo-charge-owner-'||((g-1)%30+1))::uuid,md5('pleos-demo-charge-session-'||g)::uuid,
  'st-'||lpad(((g-1)%10+1)::text,3,'0'),
  now()-interval '90 minutes'-(g%28)*interval '1 day',now()-interval '60 minutes'-(g%28)*interval '1 day',
  20,case when g%7=0 then 20 else 60 end,case when g%7=0 then null else 30+(g%60) end,
  case when g%7=0 then 'FAILED' else 'SUCCESS' end
from generate_series(1,300) g on conflict(installation_id,id) do nothing;

insert into pleos_private.reviews(installation_id,session_id,rating,tags)
select md5('pleos-demo-charge-owner-'||((g-1)%30+1))::uuid,md5('pleos-demo-charge-session-'||g)::uuid,
  3+g%3,array[case when g%2=0 then 'FAST' else 'CLEAN' end]
from generate_series(1,300) g where g%7<>0 on conflict(installation_id,session_id) do nothing;

insert into pleos_private.app_installations(id,app,vehicle_hash,data_source,consent_version)
select md5('pleos-demo-cruise-owner-'||g)::uuid,'CRUISEBREAKER',encode(sha256(convert_to('pleos-demo-cruise-owner-'||g,'UTF8')),'hex'),'SEED','demo-v1'
from generate_series(1,200) g on conflict(id) do nothing;

declare p record; begin
  for p in select * from (values
    (37.4176::float8,127.0991::float8,'경부고속도로 판교JC 합류 (시연)'),
    (37.4362::float8,127.0640::float8,'경부고속도로 서초IC 부근 (시연)'),
    (37.4630::float8,127.0420::float8,'경부고속도로 양재IC 진출 (시연)'),
    (37.4163::float8,127.0947::float8,'시연 지점 (고정 위치 앞 약 400m)')
  ) as points(lat,lon,name) loop
    perform pleos_private.ensure_segment(pleos_private.geohash_encode(p.lat,p.lon));
    update pleos_private.segments set road_name=p.name where geohash=pleos_private.geohash_encode(p.lat,p.lon) and road_name is null;
  end loop;
end;

with cells as (
  select row_number() over() idx,* from (values
    (37.4176::float8,127.0991::float8),(37.4362::float8,127.0640::float8),
    (37.4630::float8,127.0420::float8),(37.4163::float8,127.0947::float8)
  ) p(lat,lon)
)
insert into pleos_private.cruise_passes(installation_id,segment_id,heading8,pass_date)
select md5('pleos-demo-cruise-owner-'||((g-1)%200+1))::uuid,pleos_private.geohash_encode(c.lat,c.lon),0,
  (now() at time zone 'Asia/Seoul')::date-1-((g-1)/200)
from cells c cross join generate_series(1,500) g
on conflict(installation_id,segment_id,heading8,pass_date) do nothing;

with cells as (
  select * from (values
    (37.4176::float8,127.0991::float8,100),(37.4362::float8,127.0640::float8,60),
    (37.4630::float8,127.0420::float8,40),(37.4163::float8,127.0947::float8,200)
  ) p(lat,lon,events)
)
insert into pleos_private.cruise_events(installation_id,segment_id,heading8,occurred_hour)
select md5('pleos-demo-cruise-owner-'||((g-1)%200+1))::uuid,pleos_private.geohash_encode(c.lat,c.lon),0,
  (((now() at time zone 'Asia/Seoul')::date-1-((g-1)/200))::timestamp+interval '12 hours') at time zone 'Asia/Seoul'
from cells c cross join lateral generate_series(1,c.events) g
on conflict(installation_id,segment_id,heading8,occurred_hour) do nothing;

insert into pleos_private.migration_versions(version,description)
values('20261102000200','Synthetic demo: 10 stations, 300 charge sessions, 400 disengages, 2000 passes') on conflict(version) do nothing;
end $seed$;
notify pgrst,'reload schema';
commit;
