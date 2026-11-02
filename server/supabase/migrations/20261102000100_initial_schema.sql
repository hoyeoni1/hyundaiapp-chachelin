-- Additive initial migration. Run as project postgres via SQL Editor/CLI.
-- No existing table is dropped or overwritten. Everything rolls back on error.
begin;
select pg_advisory_xact_lock(20261102, 1);
create schema if not exists pleos_private;
revoke all on schema pleos_private from public, anon, authenticated;

create table pleos_private.migration_versions (
  version text primary key,
  applied_at timestamptz not null default now(),
  description text not null
);

create table pleos_private.app_installations (
  id uuid primary key default gen_random_uuid(),
  owner_user_id uuid references auth.users(id),
  app text not null check (app in ('CHACHELIN','CRUISEBREAKER')),
  vehicle_hash text not null check (vehicle_hash ~ '^[0-9a-f]{64}$'),
  state text not null default 'ACTIVE' check (state in ('ACTIVE','REVOKED')),
  data_source text not null check (data_source in ('LIVE','SIMULATED','SEED')),
  consent_version text not null check (length(consent_version) between 1 and 100),
  consented_at timestamptz not null default now(),
  revoked_at timestamptz,
  check ((data_source = 'SEED') = (owner_user_id is null)),
  check ((state = 'REVOKED') = (revoked_at is not null)),
  unique (owner_user_id, app, vehicle_hash)
);

create table pleos_private.stations (
  id text primary key check (length(id) between 1 and 100),
  name text not null,
  operator text not null,
  latitude double precision not null check (latitude between -90 and 90),
  longitude double precision not null check (longitude between -180 and 180),
  max_kw numeric not null check (max_kw > 0 and max_kw < 10000),
  external_id text unique,
  source text not null check (source in ('PUBLIC_API','SEED')),
  active boolean not null default true,
  updated_at timestamptz not null default now()
);

create table pleos_private.charge_sessions (
  installation_id uuid not null references pleos_private.app_installations(id),
  id uuid not null,
  station_id text references pleos_private.stations(id),
  started_at timestamptz not null,
  ended_at timestamptz not null,
  soc_start smallint not null check (soc_start between 0 and 100),
  soc_end smallint not null check (soc_end between 0 and 100),
  avg_kw numeric check (avg_kw > 0 and avg_kw < 10000),
  result text not null check (result in ('SUCCESS','FAILED','NOT_AT_STATION')),
  record_state text not null default 'VALID' check (record_state in ('VALID','RETRACTED')),
  simulation_time_scale integer not null default 1 check (simulation_time_scale between 1 and 1000),
  received_at timestamptz not null default now(),
  primary key (installation_id,id),
  check (ended_at >= started_at and isfinite(started_at) and isfinite(ended_at)),
  check ((result = 'NOT_AT_STATION') = (station_id is null)),
  check (record_state = 'VALID' or result = 'FAILED')
);
create index charge_station_time on pleos_private.charge_sessions (station_id,ended_at) where record_state='VALID';
create index charge_owner_time on pleos_private.charge_sessions (installation_id,ended_at desc);

-- Minimal receipt for retract-before-upload races; contains no charge measurements.
create table pleos_private.charge_retractions (
  installation_id uuid not null references pleos_private.app_installations(id),
  session_id uuid not null,
  created_at timestamptz not null default now(),
  primary key (installation_id,session_id)
);

create table pleos_private.reviews (
  installation_id uuid not null,
  session_id uuid not null,
  rating smallint not null check (rating between 1 and 5),
  tags text[] not null default '{}',
  created_at timestamptz not null default now(),
  primary key (installation_id,session_id),
  foreign key (installation_id,session_id) references pleos_private.charge_sessions(installation_id,id) on delete cascade,
  check (cardinality(tags) <= 6 and array_position(tags,null) is null
    and tags <@ array['FAST','SLOW','NO_WAIT','WAIT','BROKEN_SLOT','CLEAN']::text[])
);

create table pleos_private.segments (
  geohash text primary key check (geohash ~ '^[0123456789bcdefghjkmnpqrstuvwxyz]{7}$'),
  center_lat double precision not null check (center_lat between -90 and 90),
  center_lon double precision not null check (center_lon between -180 and 180),
  road_name text
);
create table pleos_private.cruise_events (
  installation_id uuid not null references pleos_private.app_installations(id),
  segment_id text not null references pleos_private.segments(geohash),
  heading8 smallint not null check (heading8 between 0 and 7),
  occurred_hour timestamptz not null check (isfinite(occurred_hour)
    and occurred_hour = date_trunc('hour',occurred_hour at time zone 'UTC') at time zone 'UTC'),
  event_date date generated always as ((occurred_hour at time zone 'Asia/Seoul')::date) stored,
  received_at timestamptz not null default now(),
  primary key (installation_id,segment_id,heading8,occurred_hour)
);
create table pleos_private.cruise_passes (
  installation_id uuid not null references pleos_private.app_installations(id),
  segment_id text not null references pleos_private.segments(geohash),
  heading8 smallint not null check (heading8 between 0 and 7),
  pass_date date not null check (isfinite(pass_date)),
  received_at timestamptz not null default now(),
  primary key (installation_id,segment_id,heading8,pass_date)
);
create index cruise_event_aggregation on pleos_private.cruise_events(segment_id,heading8,event_date,installation_id);
create index cruise_pass_aggregation on pleos_private.cruise_passes(segment_id,heading8,pass_date,installation_id);

-- Defense in depth: raw tables are not in the exposed public schema and have no client grants.
alter table pleos_private.app_installations enable row level security;
alter table pleos_private.stations enable row level security;
alter table pleos_private.charge_sessions enable row level security;
alter table pleos_private.charge_retractions enable row level security;
alter table pleos_private.reviews enable row level security;
alter table pleos_private.segments enable row level security;
alter table pleos_private.cruise_events enable row level security;
alter table pleos_private.cruise_passes enable row level security;
alter table pleos_private.migration_versions enable row level security;
create policy installation_owner on pleos_private.app_installations for select to authenticated using (owner_user_id=(select auth.uid()));
create policy charge_owner on pleos_private.charge_sessions for select to authenticated using (
  exists(select 1 from pleos_private.app_installations i where i.id=installation_id and i.owner_user_id=(select auth.uid())));
create policy review_owner on pleos_private.reviews for select to authenticated using (
  exists(select 1 from pleos_private.app_installations i where i.id=installation_id and i.owner_user_id=(select auth.uid())));
create policy event_owner on pleos_private.cruise_events for select to authenticated using (
  exists(select 1 from pleos_private.app_installations i where i.id=installation_id and i.owner_user_id=(select auth.uid())));
create policy pass_owner on pleos_private.cruise_passes for select to authenticated using (
  exists(select 1 from pleos_private.app_installations i where i.id=installation_id and i.owner_user_id=(select auth.uid())));
revoke all on all tables in schema pleos_private from public,anon,authenticated;

create function pleos_private.require_user() returns uuid language plpgsql stable set search_path='' as $$
begin
  if auth.uid() is null then raise exception 'Authentication required' using errcode='42501'; end if;
  return auth.uid();
end $$;

-- All writes and withdrawal serialize on this row. Call only within the same transaction.
create function pleos_private.lock_installation(p_id uuid,p_app text default null,p_allow_revoked boolean default false)
returns pleos_private.app_installations language plpgsql set search_path='' as $$
declare i pleos_private.app_installations;
begin
  select * into i from pleos_private.app_installations
    where id=p_id and owner_user_id=pleos_private.require_user() for update;
  if not found or (p_app is not null and i.app<>p_app) then
    raise exception 'Installation not owned by caller or wrong app' using errcode='42501';
  end if;
  if i.state<>'ACTIVE' and not p_allow_revoked then raise exception 'Installation revoked' using errcode='42501'; end if;
  return i;
end $$;

create function public.pleos_register_installation(p_app text,p_vehicle_id uuid,p_consent_version text,p_simulated boolean default false)
returns uuid language plpgsql security definer set search_path='' as $$
declare v_owner uuid:=pleos_private.require_user(); v_id uuid; v_hash text;
begin
  if p_app is null or p_app not in ('CHACHELIN','CRUISEBREAKER') or p_vehicle_id is null
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

create function public.pleos_upload_charge(p_installation_id uuid,p_id uuid,p_station_id text,
  p_started_at timestamptz,p_ended_at timestamptz,p_soc_start smallint,p_soc_end smallint,
  p_avg_kw numeric,p_result text,p_time_scale integer default 1)
returns void language plpgsql security definer set search_path='' as $$
declare i pleos_private.app_installations; old pleos_private.charge_sessions;
begin
  i:=pleos_private.lock_installation(p_installation_id,'CHACHELIN');
  if exists(select 1 from pleos_private.charge_retractions where installation_id=p_installation_id and session_id=p_id) then return; end if;
  if i.data_source='LIVE' and p_time_scale<>1 then raise exception 'Live record cannot be time scaled' using errcode='22023'; end if;
  if p_ended_at>now()+interval '5 minutes' then raise exception 'Future charge timestamp' using errcode='22023'; end if;
  if i.data_source='LIVE' and exists(select 1 from pleos_private.stations where id=p_station_id and source='SEED') then
    raise exception 'Live record cannot target a seed station' using errcode='22023';
  end if;
  select * into old from pleos_private.charge_sessions where installation_id=p_installation_id and id=p_id;
  if found then
    if row(old.station_id,old.started_at,old.ended_at,old.soc_start,old.soc_end,old.avg_kw,old.result,old.simulation_time_scale)
      is distinct from row(p_station_id,p_started_at,p_ended_at,p_soc_start,p_soc_end,p_avg_kw,p_result,p_time_scale) then
      raise exception 'Session payload conflict' using errcode='22023';
    end if;
    return; -- A retracted record stays retracted even if the original upload arrives late.
  end if;
  insert into pleos_private.charge_sessions(installation_id,id,station_id,started_at,ended_at,soc_start,soc_end,avg_kw,result,simulation_time_scale)
    values(p_installation_id,p_id,p_station_id,p_started_at,p_ended_at,p_soc_start,p_soc_end,p_avg_kw,p_result,p_time_scale);
end $$;

create function public.pleos_submit_review(p_installation_id uuid,p_session_id uuid,p_rating smallint,p_tags text[])
returns void language plpgsql security definer set search_path='' as $$
declare v_tags text[]; old pleos_private.reviews;
begin
  perform pleos_private.lock_installation(p_installation_id,'CHACHELIN');
  if not exists(select 1 from pleos_private.charge_sessions where installation_id=p_installation_id
    and id=p_session_id and result='SUCCESS' and record_state='VALID') then
    raise exception 'A valid successful charge owned by caller is required' using errcode='22023';
  end if;
  if p_tags is null or array_position(p_tags,null) is not null then raise exception 'Invalid tags' using errcode='22023'; end if;
  select coalesce(array_agg(t order by t),'{}') into v_tags from (select distinct unnest(p_tags) t) a;
  select * into old from pleos_private.reviews where installation_id=p_installation_id and session_id=p_session_id;
  if found then
    if old.rating is distinct from p_rating or old.tags is distinct from v_tags then
      raise exception 'Review payload conflict' using errcode='22023';
    end if;
    return;
  end if;
  insert into pleos_private.reviews(installation_id,session_id,rating,tags) values(p_installation_id,p_session_id,p_rating,v_tags);
end $$;

create function public.pleos_retract_failure(p_installation_id uuid,p_session_id uuid)
returns void language plpgsql security definer set search_path='' as $$
begin
  perform pleos_private.lock_installation(p_installation_id,'CHACHELIN');
  if exists(select 1 from pleos_private.charge_sessions where installation_id=p_installation_id and id=p_session_id and result<>'FAILED') then
    raise exception 'Only failure records may be retracted' using errcode='22023';
  end if;
  insert into pleos_private.charge_retractions(installation_id,session_id) values(p_installation_id,p_session_id) on conflict do nothing;
  update pleos_private.charge_sessions set record_state='RETRACTED' where installation_id=p_installation_id and id=p_session_id;
end $$;

create function public.pleos_withdraw_installation(p_installation_id uuid)
returns void language plpgsql security definer set search_path='' as $$
begin
  perform pleos_private.lock_installation(p_installation_id,null,true);
  update pleos_private.app_installations set state='REVOKED',revoked_at=coalesce(revoked_at,now()) where id=p_installation_id;
  delete from pleos_private.charge_sessions where installation_id=p_installation_id;
  delete from pleos_private.charge_retractions where installation_id=p_installation_id;
  delete from pleos_private.cruise_events where installation_id=p_installation_id;
  delete from pleos_private.cruise_passes where installation_id=p_installation_id;
end $$;

-- Geohash functions use the same lon-first base32 algorithm as Android SegmentKey.
create function pleos_private.geohash_bounds(p_hash text) returns double precision[]
language plpgsql immutable strict set search_path='' as $$
declare s double precision:=-90; n double precision:=90; w double precision:=-180; e double precision:=180;
  lon boolean:=true; c text; d integer; bit integer; mid double precision;
begin
  if p_hash !~ '^[0123456789bcdefghjkmnpqrstuvwxyz]{7}$' then raise exception 'Invalid geohash' using errcode='22023'; end if;
  for c in select unnest(string_to_array(p_hash,null)) loop
    d:=strpos('0123456789bcdefghjkmnpqrstuvwxyz',c)-1;
    for bit in reverse 4..0 loop
      if lon then mid:=(w+e)/2; if (d & (1<<bit))<>0 then w:=mid; else e:=mid; end if;
      else mid:=(s+n)/2; if (d & (1<<bit))<>0 then s:=mid; else n:=mid; end if; end if;
      lon:=not lon;
    end loop;
  end loop;
  return array[s,n,w,e];
end $$;
create function pleos_private.geohash_encode(p_lat double precision,p_lon double precision) returns text
language plpgsql immutable strict set search_path='' as $$
declare s double precision:=-90; n double precision:=90; w double precision:=-180; e double precision:=180;
  lon boolean:=true; k integer; ch integer:=0; bits integer:=0; mid double precision; result text:='';
begin
  if p_lat not between -90 and 90 or p_lon not between -180 and 180 then raise exception 'Invalid coordinate' using errcode='22023'; end if;
  for k in 1..35 loop
    ch:=ch<<1;
    if lon then mid:=(w+e)/2; if p_lon>=mid then ch:=ch|1; w:=mid; else e:=mid; end if;
    else mid:=(s+n)/2; if p_lat>=mid then ch:=ch|1; s:=mid; else n:=mid; end if; end if;
    lon:=not lon; bits:=bits+1;
    if bits=5 then result:=result||substr('0123456789bcdefghjkmnpqrstuvwxyz',ch+1,1); ch:=0; bits:=0; end if;
  end loop;
  return result;
end $$;
create function pleos_private.neighbors(p_hash text) returns text[] language plpgsql immutable strict set search_path='' as $$
declare b double precision[]:=pleos_private.geohash_bounds(p_hash); dy integer; dx integer; result text[]:=array[p_hash];
begin
  for dy in reverse 1..-1 loop for dx in -1..1 loop
    if dx<>0 or dy<>0 then result:=array_append(result,pleos_private.geohash_encode((b[1]+b[2])/2+dy*(b[2]-b[1]),(b[3]+b[4])/2+dx*(b[4]-b[3]))); end if;
  end loop; end loop;
  return result;
end $$;
create function pleos_private.ensure_segment(p_hash text) returns void language plpgsql set search_path='' as $$
declare b double precision[]:=pleos_private.geohash_bounds(p_hash);
begin
  insert into pleos_private.segments(geohash,center_lat,center_lon) values(p_hash,(b[1]+b[2])/2,(b[3]+b[4])/2) on conflict do nothing;
end $$;

create function public.pleos_upload_pass(p_installation_id uuid,p_segment_id text,p_heading8 smallint,p_date date)
returns void language plpgsql security definer set search_path='' as $$
begin
  perform pleos_private.lock_installation(p_installation_id,'CRUISEBREAKER');
  if p_date>(now() at time zone 'Asia/Seoul')::date then raise exception 'Future pass date' using errcode='22023'; end if;
  perform pleos_private.ensure_segment(p_segment_id);
  insert into pleos_private.cruise_passes(installation_id,segment_id,heading8,pass_date)
    values(p_installation_id,p_segment_id,p_heading8,p_date) on conflict do nothing;
end $$;
create function public.pleos_upload_disengage(p_installation_id uuid,p_segment_id text,p_heading8 smallint,p_hour timestamptz)
returns void language plpgsql security definer set search_path='' as $$
begin
  perform pleos_private.lock_installation(p_installation_id,'CRUISEBREAKER');
  if p_hour>now()+interval '5 minutes' then raise exception 'Future disengage timestamp' using errcode='22023'; end if;
  perform pleos_private.ensure_segment(p_segment_id);
  insert into pleos_private.cruise_events(installation_id,segment_id,heading8,occurred_hour)
    values(p_installation_id,p_segment_id,p_heading8,p_hour) on conflict do nothing;
  insert into pleos_private.cruise_passes(installation_id,segment_id,heading8,pass_date)
    values(p_installation_id,p_segment_id,p_heading8,(p_hour at time zone 'Asia/Seoul')::date) on conflict do nothing;
end $$;

create function public.pleos_my_charges(p_installation_id uuid,p_limit integer default 100,p_before timestamptz default null,p_before_id uuid default null)
returns jsonb language plpgsql security definer set search_path='' as $$
begin
  perform pleos_private.lock_installation(p_installation_id,'CHACHELIN');
  if p_limit is null or p_limit not between 1 and 200 then raise exception 'Invalid limit' using errcode='22023'; end if;
  if (p_before is null)<>(p_before_id is null) then raise exception 'Both cursor fields are required' using errcode='22023'; end if;
  return coalesce((select jsonb_agg(to_jsonb(r) order by r.ended_at desc,r.id desc) from
    (select id,station_id,started_at,ended_at,soc_start,soc_end,avg_kw,result from pleos_private.charge_sessions
      where installation_id=p_installation_id and record_state='VALID' and (p_before is null or (ended_at,id)<(p_before,p_before_id))
      order by ended_at desc,id desc limit p_limit) r),'[]');
end $$;

create function pleos_private.distance_m(a double precision,b double precision,c double precision,d double precision)
returns double precision language sql immutable strict set search_path='' as $$
  select 6371000.0*2*asin(sqrt(least(1.0,power(sin(radians(c-a)/2),2)+cos(radians(a))*cos(radians(c))*power(sin(radians(d-b)/2),2))))
$$;

-- Scoring deliberately uses average stars only; failure penalty is not approved.
create function public.pleos_nearby_stations(p_lat double precision,p_lon double precision,p_radius_m double precision default 10000,p_demo boolean default false)
returns jsonb language plpgsql security definer set search_path='' as $$
begin
  perform pleos_private.require_user();
  if p_lat is null or p_lon is null or p_radius_m is null or p_demo is null or p_lat not between -90 and 90
    or p_lon not between -180 and 180 or p_radius_m<=0 or p_radius_m>50000 then raise exception 'Invalid search' using errcode='22023'; end if;
  return coalesce((with eligible as (
    select c.* from pleos_private.charge_sessions c join pleos_private.app_installations i on i.id=c.installation_id
      where c.record_state='VALID' and i.state='ACTIVE' and ((p_demo and i.data_source in ('SIMULATED','SEED')) or (not p_demo and i.data_source='LIVE'))
  ), scores as (
    select s.id,s.name,s.operator,s.latitude,s.longitude,s.max_kw,s.source,
      (select count(*) from eligible c where c.station_id=s.id and c.result='SUCCESS') verified_charges,
      (select avg(c.avg_kw) from eligible c where c.station_id=s.id and c.result='SUCCESS') avg_kw,
      (select count(*) from eligible c where c.station_id=s.id and c.result='FAILED' and c.ended_at>=now()-interval '7 days') recent_failures_7d,
      (select count(*) from eligible c join pleos_private.reviews r on (r.installation_id,r.session_id)=(c.installation_id,c.id) where c.station_id=s.id) review_count,
      (select avg(r.rating) from eligible c join pleos_private.reviews r on (r.installation_id,r.session_id)=(c.installation_id,c.id) where c.station_id=s.id) avg_rating
    from pleos_private.stations s where s.active and (p_demo or s.source<>'SEED')
      and pleos_private.distance_m(p_lat,p_lon,s.latitude,s.longitude)<=p_radius_m
  ) select jsonb_agg(to_jsonb(r) order by r.chachelin_score desc nulls last,r.id) from (
    select *,case when review_count>=5 then avg_rating else null end chachelin_score from scores order by avg_rating desc nulls last,id limit 200
  ) r),'[]');
end $$;

create function public.pleos_station_detail(p_station_id text,p_demo boolean default false)
returns jsonb language plpgsql security definer set search_path='' as $$
declare s pleos_private.stations; score jsonb; ratings jsonb; tags jsonb; failures jsonb;
begin
  perform pleos_private.require_user();
  if p_station_id is null or p_demo is null then raise exception 'Invalid detail request' using errcode='22023'; end if;
  select * into s from pleos_private.stations where id=p_station_id and active and (p_demo or source<>'SEED');
  if not found then return null; end if;
  select value into score from jsonb_array_elements(public.pleos_nearby_stations(s.latitude,s.longitude,1,p_demo)) where value->>'id'=s.id;
  with eligible as (
    select r.* from pleos_private.reviews r join pleos_private.charge_sessions c
      on (c.installation_id,c.id)=(r.installation_id,r.session_id)
      join pleos_private.app_installations i on i.id=c.installation_id
    where c.station_id=s.id and c.record_state='VALID' and c.result='SUCCESS' and i.state='ACTIVE'
      and ((p_demo and i.data_source in ('SEED','SIMULATED')) or (not p_demo and i.data_source='LIVE'))
  ) select jsonb_agg((select count(*) from eligible where rating=g) order by g) into ratings from generate_series(1,5) g;
  with counts as (
    select tag,count(*) n from pleos_private.reviews r join pleos_private.charge_sessions c
      on (c.installation_id,c.id)=(r.installation_id,r.session_id)
      join pleos_private.app_installations i on i.id=c.installation_id cross join lateral unnest(r.tags) tag
    where c.station_id=s.id and c.record_state='VALID' and c.result='SUCCESS' and i.state='ACTIVE'
      and ((p_demo and i.data_source in ('SEED','SIMULATED')) or (not p_demo and i.data_source='LIVE'))
    group by tag order by n desc,tag limit 3
  ) select coalesce(jsonb_agg(tag order by n desc,tag),'[]') into tags from counts;
  -- Public detail returns hour buckets, never another installation's precise session time.
  select coalesce(jsonb_agg(hour_ms order by hour_ms desc),'[]') into failures from (
    select distinct (floor(extract(epoch from c.ended_at)/3600)*3600000)::bigint hour_ms
    from pleos_private.charge_sessions c join pleos_private.app_installations i on i.id=c.installation_id
    where c.station_id=s.id and c.record_state='VALID' and c.result='FAILED' and c.ended_at>=now()-interval '7 days' and i.state='ACTIVE'
      and ((p_demo and i.data_source in ('SEED','SIMULATED')) or (not p_demo and i.data_source='LIVE'))
    order by hour_ms desc limit 100
  ) x;
  return jsonb_build_object('station',score,'rating_counts',ratings,'top_tags',tags,'recent_failure_hour_ms',failures);
end $$;

create function public.pleos_nearby_risk(p_lat double precision,p_lon double precision,p_radius_m double precision default 10000,p_demo boolean default false)
returns jsonb language plpgsql security definer set search_path='' as $$
declare today date:=(now() at time zone 'Asia/Seoul')::date;
begin
  perform pleos_private.require_user();
  if p_lat is null or p_lon is null or p_radius_m is null or p_demo is null or p_lat not between -89 and 89
    or p_lon not between -179 and 179 or p_radius_m<=0 or p_radius_m>50000 then raise exception 'Invalid search' using errcode='22023'; end if;
  return coalesce((with owners as (
    select id from pleos_private.app_installations where state='ACTIVE' and app='CRUISEBREAKER'
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

-- Restrict every function explicitly. Helpers never become client-callable.
revoke all on all functions in schema pleos_private from public,anon,authenticated;
do $$ declare f record; begin
  for f in select p.oid::regprocedure signature from pg_proc p join pg_namespace n on n.oid=p.pronamespace
    where n.nspname='public' and p.proname like 'pleos\_%' escape '\' loop
    execute format('revoke all on function %s from public,anon,authenticated',f.signature);
    execute format('grant execute on function %s to authenticated',f.signature);
  end loop;
end $$;
insert into pleos_private.migration_versions(version,description) values('20261102000100','Initial private schema and authenticated RPCs; no failure penalty');
notify pgrst,'reload schema';
commit;
