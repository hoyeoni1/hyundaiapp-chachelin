-- Synthetic demo data only: one station with high stars but frequent failures (PRD 5.5 seed note, 2026-11-03).
-- st-004 gets 10 extra 5-star charges and 15 extra failures from 12 extra seed owners (owner 31..42).
-- Without the penalty it would rank first by stars (about 4.29); with it, last (about 4.29 x (1 - 20/55) = 2.73).
-- Requires the base demo seed 20261102000200; skipped on live-only deployments and when already applied.
-- refresh-demo-dates.sql knows these rows (owner/charge counts change only after this version exists).
begin;
select pg_advisory_xact_lock(20261102,1);
do $seed$
begin
if exists(select 1 from pleos_private.migration_versions where version='20261103000200') then return; end if;
if not exists(select 1 from pleos_private.migration_versions where version='20261102000200') then return; end if;

insert into pleos_private.app_installations(id,app,vehicle_hash,data_source,consent_version)
select md5('pleos-demo-charge-owner-'||g)::uuid,'CHACHELIN',encode(sha256(convert_to('pleos-demo-charge-owner-'||g,'UTF8')),'hex'),'SEED','demo-v1'
from generate_series(31,42) g on conflict(id) do nothing;

-- g 1..10: 5-star charges, g 11..25: failures. Spread over the last 28 days; several failures fall inside 7 days.
insert into pleos_private.charge_sessions(installation_id,id,station_id,started_at,ended_at,soc_start,soc_end,avg_kw,result)
select md5('pleos-demo-charge-owner-'||(31+(g-1)%12))::uuid,md5('pleos-demo-charge-session-'||(300+g))::uuid,'st-004',
  now()-interval '90 minutes'-((g*3)%28)*interval '1 day',now()-interval '60 minutes'-((g*3)%28)*interval '1 day',
  20,case when g<=10 then 60 else 20 end,case when g<=10 then 80+g else null end,
  case when g<=10 then 'SUCCESS' else 'FAILED' end
from generate_series(1,25) g on conflict(installation_id,id) do nothing;

insert into pleos_private.reviews(installation_id,session_id,rating,tags)
select md5('pleos-demo-charge-owner-'||(31+(g-1)%12))::uuid,md5('pleos-demo-charge-session-'||(300+g))::uuid,5,array['FAST']
from generate_series(1,10) g on conflict(installation_id,session_id) do nothing;

insert into pleos_private.migration_versions(version,description)
values('20261103000200','Synthetic demo: st-004 high stars with frequent failures (12 owners, 10 charges, 15 failures)') on conflict(version) do nothing;
end $seed$;
notify pgrst,'reload schema';
commit;
