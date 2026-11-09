-- Synthetic demo reviews only. Spread station averages while preserving st-004's
-- high-stars / low-score example and all existing session counts and dates.
begin;
select pg_advisory_xact_lock(20261102,1);

do $seed$
declare
  updated_rows integer;
begin
  if exists(select 1 from pleos_private.migration_versions where version='20261109000100') then return; end if;
  if not exists(select 1 from pleos_private.migration_versions where version='20261103000200') then return; end if;

  with targets(station_id,average_rating) as (values
    ('st-001',4.2::numeric), ('st-002',4.1), ('st-003',3.9),
    ('st-005',3.8), ('st-006',3.6), ('st-007',3.5),
    ('st-008',3.4), ('st-009',3.3), ('st-010',3.2)
  ), original_sessions as (
    select md5('pleos-demo-charge-owner-'||((g-1)%30+1))::uuid installation_id,
      md5('pleos-demo-charge-session-'||g)::uuid session_id
    from generate_series(1,300) g
  ), ranked as (
    select r.installation_id,r.session_id,t.average_rating,
      row_number() over(partition by c.station_id order by r.session_id) position,
      count(*) over(partition by c.station_id) review_count
    from pleos_private.reviews r
    join original_sessions s using(installation_id,session_id)
    join pleos_private.charge_sessions c on c.installation_id=r.installation_id and c.id=r.session_id
    join pleos_private.app_installations i on i.id=r.installation_id
    join pleos_private.stations station on station.id=c.station_id
    join targets t on t.station_id=c.station_id
    where i.data_source='SEED' and i.owner_user_id is null and i.consent_version='demo-v1'
      and station.source='SEED' and c.result='SUCCESS'
  )
  update pleos_private.reviews r
  set rating=(floor(s.average_rating) + case
    when s.position <= round(s.average_rating*s.review_count)-floor(s.average_rating)*s.review_count
    then 1 else 0 end)::smallint
  from ranked s where r.installation_id=s.installation_id and r.session_id=s.session_id;

  get diagnostics updated_rows = row_count;
  if updated_rows <> 233 then
    raise exception 'Original demo reviews are incomplete: expected 233 updates, found %. Changes rolled back.', updated_rows;
  end if;

  insert into pleos_private.migration_versions(version,description)
  values('20261109000100','Synthetic demo: station review averages 3.2 to 4.3, preserving st-004 ranking reversal');
end $seed$;
commit;
