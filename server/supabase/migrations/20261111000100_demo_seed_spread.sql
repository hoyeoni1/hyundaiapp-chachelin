-- Demonstration data only. Preserve the two existing charging scenarios and the banner cell.
begin;
select pg_advisory_xact_lock(20261102,1);

do $seed$
declare
  station record;
  segment record;
  affected integer;
  cell text;
  boundary_cell text := pleos_private.geohash_encode(37.4218,127.0960);
  yesterday date := (now() at time zone 'Asia/Seoul')::date-1;
  charge_anchor timestamptz := (yesterday::timestamp+interval '12 hours') at time zone 'Asia/Seoul';
begin
  if exists(select 1 from pleos_private.migration_versions where version='20261111000100') then return; end if;
  if not exists(select 1 from pleos_private.migration_versions where version='20261102000200') then return; end if;
  if (select count(*) from pleos_private.migration_versions
      where version in ('20261103000200','20261107000100','20261109000100','20261110000100')) <> 4 then
    raise exception 'Apply the previous demo migrations before spreading the seed.';
  end if;

  -- Replace only the original synthetic sessions for the selected stations.
  -- st-003 and st-004 keep their existing sessions and reviews.
  for station in select * from (values
    ('st-001',128,0,4.8::numeric,68),
    ('st-002',64,0,3.9,52),
    ('st-005',12,8,4.3,62),
    ('st-006',3,0,4.7,75),
    ('st-007',32,0,4.3,43),
    ('st-008',20,0,3.5,81),
    ('st-009',16,0,3.1,56),
    ('st-010',8,0,3.7,71)
  ) as targets(id,successes,failures,stars,kw) loop
    if not exists(select 1 from pleos_private.stations where id=station.id and source='SEED' and active) then
      raise exception 'Missing active seed station %', station.id;
    end if;
    delete from pleos_private.charge_sessions c
    using pleos_private.app_installations i, generate_series(1,300) as seed(n)
    where c.installation_id=i.id and c.station_id=station.id
      and i.app='CHACHELIN' and i.data_source='SEED' and i.owner_user_id is null
      and i.consent_version='demo-v1' and i.state='ACTIVE'
      and i.id=md5('pleos-demo-charge-owner-'||((seed.n-1)%30+1))::uuid
      and c.id=md5('pleos-demo-charge-session-'||seed.n)::uuid;
    get diagnostics affected = row_count;
    if affected <> 30 then
      raise exception 'Expected 30 original sessions at %, found %', station.id, affected;
    end if;

    insert into pleos_private.charge_sessions
      (installation_id,id,station_id,started_at,ended_at,soc_start,soc_end,avg_kw,result)
    select md5('pleos-demo-charge-owner-'||((n-1)%30+1))::uuid,
      md5('pleos-demo-spread:'||station.id||':'||n)::uuid,station.id,
      charge_anchor-(n%6)*interval '1 day'-interval '30 minutes',
      charge_anchor-(n%6)*interval '1 day',
      20,case when n<=station.successes then 60 else 20 end,
      case when n<=station.successes then station.kw end,
      case when n<=station.successes then 'SUCCESS' else 'FAILED' end
    from generate_series(1,station.successes+station.failures) as seed(n);

    insert into pleos_private.reviews(installation_id,session_id,rating,tags,created_at)
    select c.installation_id,c.id,
      (floor(station.stars)+case when n<=round((station.stars-floor(station.stars))*station.successes) then 1 else 0 end)::smallint,
      array[case when n%2=0 then 'FAST' else 'CLEAN' end],c.ended_at
    from generate_series(1,station.successes) as seed(n)
    join pleos_private.charge_sessions c
      on c.installation_id=md5('pleos-demo-charge-owner-'||((n-1)%30+1))::uuid
      and c.id=md5('pleos-demo-spread:'||station.id||':'||n)::uuid;
  end loop;

  -- Add synthetic disengagements only for existing pass-days without a disengagement.
  for segment in select * from (values
    (37.4176::float8,127.0991::float8,125,'판교 방면 차로 감소 (시연)'),
    (37.4362,127.0640,110,'서초 방면 차로 감소 (시연)'),
    (37.4630,127.0420,115,'양재 방면 합류부 (시연)'),
    (37.4163,127.0947,0,'판교JC 합류부')
  ) as targets(lat,lon,extra_events,road_name) loop
    cell := pleos_private.geohash_encode(segment.lat,segment.lon);
    if not exists(select 1 from pleos_private.cruise_passes p
      join pleos_private.app_installations i on i.id=p.installation_id
      where p.segment_id=cell and p.heading8=0 and i.app='CRONG' and i.data_source='SEED'
        and i.owner_user_id is null and i.consent_version='demo-v1' and i.state='ACTIVE') then
      raise exception 'Missing original seed segment %', cell;
    end if;
    update pleos_private.segments set road_name=segment.road_name where geohash=cell;
    insert into pleos_private.cruise_events(installation_id,segment_id,heading8,occurred_hour)
    select p.installation_id,p.segment_id,p.heading8,
      (p.pass_date::timestamp+interval '12 hours') at time zone 'Asia/Seoul'
    from pleos_private.cruise_passes p join pleos_private.app_installations i on i.id=p.installation_id
    where p.segment_id=cell and p.heading8=0 and i.app='CRONG' and i.data_source='SEED'
      and i.owner_user_id is null and i.consent_version='demo-v1' and i.state='ACTIVE'
      and not exists(select 1 from pleos_private.cruise_events e
        where (e.installation_id,e.segment_id,e.heading8,e.event_date)
            =(p.installation_id,p.segment_id,p.heading8,p.pass_date))
    order by p.pass_date desc,p.installation_id
    limit segment.extra_events;
    get diagnostics affected = row_count;
    if affected <> segment.extra_events then
      raise exception 'Insufficient original pass-days at seed segment %', cell;
    end if;
  end loop;

  -- Independent demo cell. A new northbound disengagement contributes both one pass-day and one event-day.
  if exists(select 1 from pleos_private.cruise_passes
    where segment_id=any(pleos_private.neighbors(boundary_cell)) and heading8=0
      and pass_date between yesterday-26 and yesterday+1) then
    raise exception 'Boundary demo cell overlaps existing recent northbound observations';
  end if;
  perform pleos_private.ensure_segment(boundary_cell);
  update pleos_private.segments set road_name='판교 북측 차로 감소 (경계 시연)' where geohash=boundary_cell;
  insert into pleos_private.cruise_passes(installation_id,segment_id,heading8,pass_date)
  select md5('pleos-demo-cruise-owner-'||n)::uuid,boundary_cell,0,yesterday
  from generate_series(1,19) as seed(n);
  insert into pleos_private.cruise_events(installation_id,segment_id,heading8,occurred_hour)
  select md5('pleos-demo-cruise-owner-'||n)::uuid,boundary_cell,0,charge_anchor
  from generate_series(1,5) as seed(n);

  insert into pleos_private.migration_versions(version,description)
  values('20261111000100','Spread charging scores and counts, four CRONG risks, and a threshold demo cell');
end $seed$;
commit;
