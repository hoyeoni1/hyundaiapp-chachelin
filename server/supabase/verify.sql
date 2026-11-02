-- Read-only after applying migrations. No user data or credentials are returned.
select version,description,applied_at from pleos_private.migration_versions order by version;
select 'stations' object,count(*) rows from pleos_private.stations where source='SEED'
union all select 'seed_charge_sessions',count(*) from pleos_private.charge_sessions c
  join pleos_private.app_installations i on i.id=c.installation_id where i.data_source='SEED'
union all select 'seed_cruise_events',count(*) from pleos_private.cruise_events e
  join pleos_private.app_installations i on i.id=e.installation_id where i.data_source='SEED'
union all select 'seed_cruise_passes',count(*) from pleos_private.cruise_passes p
  join pleos_private.app_installations i on i.id=p.installation_id where i.data_source='SEED';
select c.relname,c.relrowsecurity from pg_class c join pg_namespace n on n.oid=c.relnamespace
where n.nspname='pleos_private' and c.relkind='r' order by c.relname;
select p.proname,has_function_privilege('anon',p.oid,'EXECUTE') anon_can_execute,
  has_function_privilege('authenticated',p.oid,'EXECUTE') authenticated_can_execute
from pg_proc p join pg_namespace n on n.oid=p.pronamespace
where n.nspname='public' and p.proname like 'pleos\_%' escape '\' order by 1;
