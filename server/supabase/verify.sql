-- Single SELECT: SQL Editor/CLI otherwise returns only the last statement's rows.
-- Read-only; no user records or credentials are returned.
select jsonb_build_object(
  'versions',(select jsonb_agg(to_jsonb(v) order by version) from pleos_private.migration_versions v),
  'seed_counts',jsonb_build_object(
    'stations',(select count(*) from pleos_private.stations where source='SEED'),
    'charge_sessions',(select count(*) from pleos_private.charge_sessions c join pleos_private.app_installations i on i.id=c.installation_id where i.data_source='SEED'),
    'cruise_events',(select count(*) from pleos_private.cruise_events e join pleos_private.app_installations i on i.id=e.installation_id where i.data_source='SEED'),
    'cruise_passes',(select count(*) from pleos_private.cruise_passes p join pleos_private.app_installations i on i.id=p.installation_id where i.data_source='SEED')),
  'rls',(select jsonb_agg(jsonb_build_object('table',c.relname,'enabled',c.relrowsecurity) order by c.relname)
    from pg_class c join pg_namespace n on n.oid=c.relnamespace where n.nspname='pleos_private' and c.relkind='r'),
  'rpc_permissions',(select jsonb_agg(jsonb_build_object('function',p.proname,
    'anon',has_function_privilege('anon',p.oid,'EXECUTE'),'authenticated',has_function_privilege('authenticated',p.oid,'EXECUTE')) order by p.proname)
    from pg_proc p join pg_namespace n on n.oid=p.pronamespace where n.nspname='public' and p.proname like 'pleos\_%' escape '\'),
  'function_fingerprint',(select md5(string_agg(p.oid::regprocedure::text||':'||replace(p.prosrc,chr(13),'')||':'||p.prosecdef::text||':'||coalesce(p.proconfig::text,''),E'\n' order by p.oid::regprocedure::text))
    from pg_proc p join pg_namespace n on n.oid=p.pronamespace where n.nspname='pleos_private' or (n.nspname='public' and p.proname like 'pleos\_%' escape '\'))
) verification;
