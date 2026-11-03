-- Chachelin score with the 30-day failure penalty (PRD 5.5, decided 2026-11-03 by 장호연).
--   score = average stars x (1 - 30-day failure rate)
--   30-day failure rate = FAILED / (SUCCESS + FAILED) in the last 30 days; retracted ("실패 아님") rows are excluded
--   fewer than 5 sessions in 30 days -> no penalty; fewer than 5 reviews -> score null ("평가 중")
-- Replaces only public.pleos_nearby_stations (same signature, so grants and pleos_station_detail keep working).
-- Safe to re-run. Run as project postgres via SQL Editor/CLI after 20261102000100.
begin;
select pg_advisory_xact_lock(20261102, 1);

create or replace function public.pleos_nearby_stations(p_lat double precision,p_lon double precision,p_radius_m double precision default 10000,p_demo boolean default false)
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
      (select count(*) from eligible c where c.station_id=s.id and c.result in ('SUCCESS','FAILED') and c.ended_at>=now()-interval '30 days') recent_sessions_30d,
      (select count(*) from eligible c where c.station_id=s.id and c.result='FAILED' and c.ended_at>=now()-interval '30 days') recent_failures_30d,
      (select count(*) from eligible c join pleos_private.reviews r on (r.installation_id,r.session_id)=(c.installation_id,c.id) where c.station_id=s.id) review_count,
      (select avg(r.rating) from eligible c join pleos_private.reviews r on (r.installation_id,r.session_id)=(c.installation_id,c.id) where c.station_id=s.id) avg_rating
    from pleos_private.stations s where s.active and (p_demo or s.source<>'SEED')
      and pleos_private.distance_m(p_lat,p_lon,s.latitude,s.longitude)<=p_radius_m
  ), rated as (
    select *,
      case when recent_sessions_30d>=5 then recent_failures_30d::numeric/recent_sessions_30d else 0 end failure_rate_30d
    from scores
  ), scored as (
    select *,case when review_count>=5 then avg_rating*(1-failure_rate_30d) else null end chachelin_score from rated
  ) select jsonb_agg(to_jsonb(r) order by r.chachelin_score desc nulls last,r.id) from (
    select * from scored order by chachelin_score desc nulls last,id limit 200
  ) r),'[]');
end $$;

revoke all on function public.pleos_nearby_stations(double precision,double precision,double precision,boolean) from public,anon,authenticated;
grant execute on function public.pleos_nearby_stations(double precision,double precision,double precision,boolean) to authenticated;
insert into pleos_private.migration_versions(version,description)
values('20261103000100','Chachelin score: average stars x (1 - 30-day failure rate), no penalty under 5 sessions') on conflict(version) do nothing;
notify pgrst,'reload schema';
commit;
