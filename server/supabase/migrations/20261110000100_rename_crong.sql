-- Rename the cruise app code CRUISEBREAKER -> CRONG (app renamed to 크롱/CRONG, decided 2026-11-10 by 장호연).
-- Updates existing installations (seed + real), swaps the app CHECK, and recreates the 4 functions that name the app.
-- Same signatures, so existing grants stay. Safe to re-run. Run as project postgres after 20261109000100.
-- Apply together with the CRONG app release: an old app that still sends 'CRUISEBREAKER' is rejected after this.
begin;
select pg_advisory_xact_lock(20261102, 1);

alter table pleos_private.app_installations drop constraint if exists app_installations_app_check;
update pleos_private.app_installations set app='CRONG' where app='CRUISEBREAKER';
alter table pleos_private.app_installations add constraint app_installations_app_check check (app in ('CHACHELIN','CRONG'));

create or replace function public.pleos_register_installation(p_app text,p_vehicle_id uuid,p_consent_version text,p_simulated boolean default false)
returns uuid language plpgsql security definer set search_path='' as $$
declare v_owner uuid:=pleos_private.require_user(); v_id uuid; v_hash text;
begin
  if p_app is null or p_app not in ('CHACHELIN','CRONG') or p_vehicle_id is null
    or p_consent_version is null or length(p_consent_version) not between 1 and 100 or p_simulated is null then
    raise exception 'Invalid registration' using errcode='22023';
  end if;
  v_hash:=encode(sha256(convert_to(p_app||':'||p_vehicle_id::text,'UTF8')),'hex');
  insert into pleos_private.app_installations(owner_user_id,app,vehicle_hash,consent_version,data_source)
    values(v_owner,p_app,v_hash,p_consent_version,case when p_simulated then 'SIMULATED' else 'LIVE' end)
    on conflict(owner_user_id,app,vehicle_hash) do nothing;
  select id into v_id from pleos_private.app_installations
    where owner_user_id=v_owner and app=p_app and vehicle_hash=v_hash;
  perform pleos_private.lock_installation(v_id,p_app);
  if exists(select 1 from pleos_private.app_installations where id=v_id and
    (consent_version<>p_consent_version or data_source<>case when p_simulated then 'SIMULATED' else 'LIVE' end)) then
    raise exception 'Registration payload conflict; use a new identity after consent changes' using errcode='22023';
  end if;
  return v_id;
end $$;

create or replace function public.pleos_upload_pass(p_installation_id uuid,p_segment_id text,p_heading8 smallint,p_date date)
returns void language plpgsql security definer set search_path='' as $$
begin
  perform pleos_private.lock_installation(p_installation_id,'CRONG');
  if p_date>(now() at time zone 'Asia/Seoul')::date then raise exception 'Future pass date' using errcode='22023'; end if;
  perform pleos_private.ensure_segment(p_segment_id);
  insert into pleos_private.cruise_passes(installation_id,segment_id,heading8,pass_date)
    values(p_installation_id,p_segment_id,p_heading8,p_date) on conflict do nothing;
end $$;

create or replace function public.pleos_upload_disengage(p_installation_id uuid,p_segment_id text,p_heading8 smallint,p_hour timestamptz)
returns void language plpgsql security definer set search_path='' as $$
begin
  perform pleos_private.lock_installation(p_installation_id,'CRONG');
  if p_hour>now()+interval '5 minutes' then raise exception 'Future disengage timestamp' using errcode='22023'; end if;
  perform pleos_private.ensure_segment(p_segment_id);
  insert into pleos_private.cruise_events(installation_id,segment_id,heading8,occurred_hour)
    values(p_installation_id,p_segment_id,p_heading8,p_hour) on conflict do nothing;
  insert into pleos_private.cruise_passes(installation_id,segment_id,heading8,pass_date)
    values(p_installation_id,p_segment_id,p_heading8,(p_hour at time zone 'Asia/Seoul')::date) on conflict do nothing;
end $$;

create or replace function public.pleos_nearby_risk(p_lat double precision,p_lon double precision,p_radius_m double precision default 10000,p_demo boolean default false)
returns jsonb language plpgsql security definer set search_path='' as $$
declare today date:=(now() at time zone 'Asia/Seoul')::date;
begin
  perform pleos_private.require_user();
  if p_lat is null or p_lon is null or p_radius_m is null or p_demo is null or p_lat not between -89 and 89
    or p_lon not between -179 and 179 or p_radius_m<=0 or p_radius_m>50000 then raise exception 'Invalid search' using errcode='22023'; end if;
  return coalesce((with owners as (
    select id from pleos_private.app_installations where state='ACTIVE' and app='CRONG'
      and ((p_demo and data_source in ('SIMULATED','SEED')) or (not p_demo and data_source='LIVE'))
  ), keys as (
    select distinct c.segment_id,c.heading8 from pleos_private.cruise_passes c join owners o on o.id=c.installation_id
      where c.pass_date between today-27 and today
  ), nearby as (
    select k.*,s.center_lat,s.center_lon,s.road_name,pleos_private.neighbors(k.segment_id) around from keys k
      join pleos_private.segments s on s.geohash=k.segment_id
      where pleos_private.distance_m(p_lat,p_lon,s.center_lat,s.center_lon)<=p_radius_m
  ), totals as (
    select n.segment_id,n.heading8,n.center_lat,n.center_lon,n.road_name,
      (select count(distinct (e.installation_id,e.event_date)) from pleos_private.cruise_events e join owners o on o.id=e.installation_id
        where e.segment_id=any(n.around) and e.heading8=n.heading8 and e.event_date between today-27 and today) disengages,
      (select count(distinct (p.installation_id,p.pass_date)) from pleos_private.cruise_passes p join owners o on o.id=p.installation_id
        where p.segment_id=any(n.around) and p.heading8=n.heading8 and p.pass_date between today-27 and today) pass_vehicles
    from nearby n
  ), ratios as (select *,case when pass_vehicles=0 then 0 else disengages::numeric/pass_vehicles end rate from totals)
  select jsonb_agg(to_jsonb(r) order by r.rate desc,r.segment_id,r.heading8) from
    (select *,pass_vehicles>=20 and rate>=0.30 is_risk from ratios order by rate desc,segment_id,heading8 limit 200) r),'[]');
end $$;

insert into pleos_private.migration_versions(version,description)
values('20261110000100','Rename cruise app code CRUISEBREAKER to CRONG') on conflict(version) do nothing;
notify pgrst,'reload schema';
commit;
