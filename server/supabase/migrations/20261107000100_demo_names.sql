-- Synthetic demo data only: rename seed labels that looked like developer placeholders on screen (2026-11-07 rehearsal, CruiseBreaker #37).
-- - CruiseBreaker seed segment "시연 지점 (고정 위치 앞 약 400m)" -> "판교IC 진입로" (same as app Fake data, README and demo-drive.sh; S2 already shows "시연용 시드 데이터 포함").
-- - Chachelin seed stations st-005..st-010 "시연 충전소 NN / 가상 충전소" -> place-like names with "(시연)" and operator "시연 데이터",
--   the same labelling as st-001..st-004. Names stay marked "(시연)": PT and video still say these are seed data.
-- Only renames SEED rows. Touches no records, scores or permissions. Skipped on live-only deployments and when already applied.
begin;
select pg_advisory_xact_lock(20261102,1);
do $seed$
begin
if exists(select 1 from pleos_private.migration_versions where version='20261107000100') then return; end if;
if not exists(select 1 from pleos_private.migration_versions where version='20261102000200') then return; end if;

update pleos_private.segments set road_name='판교IC 진입로'
where geohash=pleos_private.geohash_encode(37.4163,127.0947) and road_name='시연 지점 (고정 위치 앞 약 400m)';

update pleos_private.stations s set name=v.name, operator='시연 데이터', updated_at=now()
from (values
  ('st-005','삼평동 공영주차장 (시연)'),
  ('st-006','판교 알파돔시티 (시연)'),
  ('st-007','백현동 카페거리 (시연)'),
  ('st-008','판교 제2테크노밸리 (시연)'),
  ('st-009','운중동 행정복지센터 (시연)'),
  ('st-010','판교 현대백화점 (시연)')
) as v(id,name)
where s.id=v.id and s.source='SEED';

insert into pleos_private.migration_versions(version,description)
values('20261107000100','Synthetic demo: rename seed segment and stations st-005..st-010 to place-like names (still marked 시연)') on conflict(version) do nothing;
end $seed$;
notify pgrst,'reload schema';
commit;
