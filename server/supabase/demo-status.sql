-- Read-only maintenance check. Run as project postgres.
select jsonb_build_object(
  'kst_today',(now() at time zone 'Asia/Seoul')::date,
  'latest_seed_charge',(
    select max(c.ended_at) from pleos_private.charge_sessions c
    join pleos_private.app_installations i on i.id=c.installation_id where i.data_source='SEED'),
  'seed_failures_7d',(
    select count(*) from pleos_private.charge_sessions c
    join pleos_private.app_installations i on i.id=c.installation_id
    where i.data_source='SEED' and c.result='FAILED' and c.record_state='VALID' and c.ended_at>=now()-interval '7 days'),
  'latest_seed_pass_date',(
    select max(p.pass_date) from pleos_private.cruise_passes p
    join pleos_private.app_installations i on i.id=p.installation_id where i.data_source='SEED'),
  'seed_passes_28d',(
    select count(*) from pleos_private.cruise_passes p
    join pleos_private.app_installations i on i.id=p.installation_id
    where i.data_source='SEED' and p.pass_date between (now() at time zone 'Asia/Seoul')::date-27 and (now() at time zone 'Asia/Seoul')::date),
  'seed_events_28d',(
    select count(*) from pleos_private.cruise_events e
    join pleos_private.app_installations i on i.id=e.installation_id
    where i.data_source='SEED' and e.event_date between (now() at time zone 'Asia/Seoul')::date-27 and (now() at time zone 'Asia/Seoul')::date)
) as demo_status;
