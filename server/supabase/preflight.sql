-- Read-only. New project: app_tables=[], app_rpcs=[], auth_table=auth.users.
select current_database() database_name,current_user database_role,to_regclass('auth.users') auth_table,
  (select coalesce(jsonb_agg(jsonb_build_object('schema',schemaname,'table',tablename) order by schemaname,tablename),'[]')
    from pg_tables where schemaname in ('public','pleos_private')) app_tables,
  (select coalesce(jsonb_agg(p.proname order by p.proname),'[]') from pg_proc p join pg_namespace n on n.oid=p.pronamespace
    where n.nspname='public' and p.proname like 'pleos\_%' escape '\') app_rpcs;
