-- Manual maintenance, NOT a migration. Run as project postgres right before recording the demo video (2026-11-07 rehearsal).
-- Rehearsals on the emulator upload SIMULATED records (demo builds) into the shared demo project, so seed numbers drift
-- (e.g. 시흥동 주민센터 인증 41 / 7일 실패 6, recent failure times = rehearsal times).
-- This deletes every SIMULATED record (charges, reviews via cascade, retractions, cruise events/passes) and keeps:
--   - SEED rows (synthetic demo data) and LIVE rows (real users) untouched
--   - the SIMULATED installations themselves, so already-installed demo apps keep working without reinstall
-- Then run refresh-demo-dates.sql (seed dates to today) and verify.sql.
begin;
select pg_advisory_xact_lock(20261102,1);

create temporary table rehearsal_installs on commit drop as
select id from pleos_private.app_installations where data_source='SIMULATED';

select
  (select count(*) from rehearsal_installs) simulated_installations,
  (select count(*) from pleos_private.charge_sessions where installation_id in (select id from rehearsal_installs)) charges_to_delete,
  (select count(*) from pleos_private.reviews where installation_id in (select id from rehearsal_installs)) reviews_to_delete,
  (select count(*) from pleos_private.cruise_events where installation_id in (select id from rehearsal_installs)) cruise_events_to_delete,
  (select count(*) from pleos_private.cruise_passes where installation_id in (select id from rehearsal_installs)) cruise_passes_to_delete;

delete from pleos_private.charge_retractions where installation_id in (select id from rehearsal_installs);
delete from pleos_private.charge_sessions where installation_id in (select id from rehearsal_installs);
delete from pleos_private.cruise_events where installation_id in (select id from rehearsal_installs);
delete from pleos_private.cruise_passes where installation_id in (select id from rehearsal_installs);

commit;
