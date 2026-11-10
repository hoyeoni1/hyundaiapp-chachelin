-- Manual maintenance, NOT a migration. Run as project postgres before a demonstration.
-- Only the deterministic, ownerless SEED installations from migrations 20261102000200 and 20261103000200
-- (the second adds charge owners 31..42 at st-004; counts below grow only once it is applied).
-- Re-running on the same KST date is idempotent. LIVE/SIMULATED records are untouched.
begin;
select pg_advisory_xact_lock(20261102,1);

create temporary table demo_refresh_expected on commit drop as
select case when has_failure_station then 42 else 30 end charge_owners,
  case when has_failure_station then 325 else 300 end charges
from (select exists(select 1 from pleos_private.migration_versions where version='20261103000200') has_failure_station) v;

create temporary table demo_refresh_owners on commit drop as
select i.id, i.app from pleos_private.app_installations i
join (
  select md5('pleos-demo-charge-owner-'||g)::uuid id, 'CHACHELIN' app from generate_series(1,(select charge_owners from demo_refresh_expected)) g
  union all
  select md5('pleos-demo-cruise-owner-'||g)::uuid, 'CRONG' from generate_series(1,200) g
) expected on expected.id=i.id and expected.app=i.app
where i.data_source='SEED' and i.owner_user_id is null and i.consent_version='demo-v1' and i.state='ACTIVE';

do $check$
begin
  if (select count(*) from demo_refresh_owners) <> 200+(select charge_owners from demo_refresh_expected)
    or (select count(*) from pleos_private.charge_sessions c join demo_refresh_owners o on o.id=c.installation_id) <> (select charges from demo_refresh_expected)
    or (select count(*) from pleos_private.cruise_passes p join demo_refresh_owners o on o.id=p.installation_id) <> 2000
    or (select count(*) from pleos_private.cruise_events e join demo_refresh_owners o on o.id=e.installation_id) <> 400 then
    raise exception 'Demo seed differs from expected owners / charges (230/300, or 242/325 with 20261103000200) / 2000 passes / 400 events. No refresh applied.';
  end if;
end $check$;

-- Rebuild only the selected rows inside this transaction: in-place PK date shifts
-- can collide with another date belonging to the same installation.
create temporary table demo_refresh_passes on commit drop as
select p.* from pleos_private.cruise_passes p join demo_refresh_owners o on o.id=p.installation_id;
create temporary table demo_refresh_events on commit drop as
select e.installation_id,e.segment_id,e.heading8,e.occurred_hour,e.received_at
from pleos_private.cruise_events e join demo_refresh_owners o on o.id=e.installation_id;
create temporary table demo_refresh_offset on commit drop as
select (now() at time zone 'Asia/Seoul')::date-1-max(pass_date) days from demo_refresh_passes;

delete from pleos_private.cruise_events where installation_id in (select id from demo_refresh_owners);
delete from pleos_private.cruise_passes where installation_id in (select id from demo_refresh_owners);
insert into pleos_private.cruise_passes(installation_id,segment_id,heading8,pass_date,received_at)
select installation_id,segment_id,heading8,pass_date+d.days,received_at
from demo_refresh_passes cross join demo_refresh_offset d;
insert into pleos_private.cruise_events(installation_id,segment_id,heading8,occurred_hour,received_at)
select installation_id,segment_id,heading8,occurred_hour+d.days*interval '1 day',received_at
from demo_refresh_events cross join demo_refresh_offset d;

-- Latest seed charge ends yesterday at noon KST, safely before the current time.
-- Keep durations, relative age, IDs, measurements, reviews and owners unchanged.
with offset_value as (
  select (((now() at time zone 'Asia/Seoul')::date-1)::timestamp+interval '12 hours') at time zone 'Asia/Seoul'
    - max(c.ended_at) shift
  from pleos_private.charge_sessions c join demo_refresh_owners o on o.id=c.installation_id
)
update pleos_private.charge_sessions c
set started_at=c.started_at+d.shift, ended_at=c.ended_at+d.shift
from offset_value d where c.installation_id in (select id from demo_refresh_owners);

update pleos_private.reviews r set created_at=c.ended_at
from pleos_private.charge_sessions c
where r.installation_id=c.installation_id and r.session_id=c.id
  and c.installation_id in (select id from demo_refresh_owners);
commit;
