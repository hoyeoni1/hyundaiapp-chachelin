# Supabase 초기 schema·시연 데이터 적용

대상 프로젝트: **pleos-apps-dev**, ref `qbdikckaqroxbpacvtqo`.

최초 적용은 사용자가 대시보드에서 실행했다. **2026-11-02 CLI로 원격 버전·시드·RLS·함수 권한 및 함수 내용 일치를 확인하고, 표준 migration 이력 2건을 applied로 정합했다. dry-run은 Remote database is up to date.** 아래 최초 설치 절차를 이미 적용된 프로젝트에서 다시 실행하지 않는다. 후속 변경은 [CLI](cli-setup.md)로 진행한다.

로컬 PostgreSQL(PGlite 0.5.14) 의미 테스트 15개도 통과했다. 후속 2026-11-03 사용자 익명 로그인 활성화 후 실제 JWT/PostgREST·두 앱 Android 연결을 확인했다([검증 기록](android-connection.md)). 원격 다중 연결의 철회-업로드 동시성 스트레스 검사는 아직 하지 않았다.

## 실행 순서

1. Supabase 대시보드에서 위 프로젝트를 열고 **SQL Editor → New query**.
2. `supabase/preflight.sql` 전체를 붙여넣고 **Run**. 신규 프로젝트라면 app_tables와 app_rpcs가 빈 배열이고 auth_table은 auth.users여야 한다. 앱 테이블·함수가 이미 있으면 결과를 공유하고 initial SQL 실행을 멈춘다.
3. 새 쿼리에 `supabase/migrations/20261102000100_initial_schema.sql` 전체를 붙여넣고 **Run**. 성공 시 schema·제약조건·인덱스·RLS·인증된 RPC가 생성된다. 원본 테이블은 `pleos_private`, 외부 API 함수는 `public.pleos_*`다.
4. 새 쿼리에 `supabase/migrations/20261102000200_demo_data.sql` 전체를 붙여넣고 **Run**. 개발/시연 프로젝트에만 적용한다. 모든 가상 설치는 SEED로 표시되며 사용자 인증 계정은 만들지 않는다.
5. 새 쿼리에 `supabase/verify.sql` 전체를 붙여넣고 **Run**. 아래 기대 결과와 비교한다. 현재 파일은 단일 SELECT로 모든 결과를 verification JSON에 묶으므로 마지막 SELECT만 보이는 문제가 없다.

## 기대 결과

- migration_versions: `20261102000100`, `20261102000200` 두 행.
- 시연 충전소 10, 충전 세션 300, 해제 400, 통과 2,000.
- 앱용 7개 테이블과 내부 관리 2개(charge_retractions, migration_versions), 총 9개 테이블의 RLS가 모두 true.
- public.pleos_*의 anon_can_execute=false, authenticated_can_execute=true.
- 실제 앱의 기존 Fake 연결은 그대로다. DB 생성만으로 Android 앱이 자동 연결되지는 않는다.

실행 성공/오류 메시지와 verify의 건수를 알려 주면 원격 반영 상태를 확인하고 앱 연결 작업을 이어갈 수 있다. 비밀번호·Secret key·토큰은 결과에 포함하지 않는다.

## 11-03 추가 migration (장호연이 대시보드에서 적용)

이미 initial·demo가 적용된 pleos-apps-dev에 **아래 순서로 한 번씩** 적용한다. 둘 다 다시 실행해도 안전하다(점수 함수는 같은 내용으로 교체, 시드는 이미 있으면 건너뜀).

1. SQL Editor → New query → `supabase/migrations/20261103000100_score_failure_penalty.sql` 전체 붙여넣기 → **Run**. `pleos_nearby_stations`만 교체한다(권한·`pleos_station_detail`은 그대로).
2. 새 쿼리 → `supabase/migrations/20261103000200_demo_failure_station.sql` → **Run**. st-004(대왕판교IC 휴게소, 시연)에 가상 5점 충전 10건·실패 15건(가상 소유자 12개)을 더한다.
3. 새 쿼리 → `supabase/refresh-demo-dates.sql` → **Run**(시드 날짜를 오늘 기준으로 맞춤. 새 시드도 함께 옮긴다).
4. 새 쿼리 → `supabase/verify.sql` → **Run**. 기대: versions에 `20261103000100`, `20261103000200`이 추가되어 4행, seed_counts.charge_sessions = **325**, RLS·RPC 권한은 이전과 같음(function_fingerprint는 바뀜).
5. 확인(선택): debug 앱 S1 목록에서 대왕판교IC 휴게소가 별점은 가장 높지만(약 4.3) 점수는 가장 낮은지(약 2.7) 본다.

CLI를 쓰는 사람(황준용)은 `db push --linked --dry-run`으로 두 파일이 대기 중인지 본 뒤 `db push --linked`로 적용해도 된다. 대시보드로 적용했으면 CLI 이력은 `migration repair --status applied 20261103000100 20261103000200`으로 맞춘다.

## 11-07 추가: 시드 이름 정리 + 리허설 기록 지우기 (장호연이 대시보드에서 적용, 시연 영상 촬영 직전)

11-07 제출 APK 리허설에서 나온 두 가지. 둘 다 다시 실행해도 안전하다.

1. SQL Editor → New query → `supabase/migrations/20261107000100_demo_names.sql` → **Run**. 이름만 바꾼다(SEED 행만):
   크루즈 시드 구간 "시연 지점 (고정 위치 앞 약 400m)" → "판교IC 진입로"(크루즈 #37), 차슐랭 st-005~010 "시연 충전소 NN · 가상 충전소" → "삼평동 공영주차장 (시연)" 등 지명 + "(시연)" · "시연 데이터"(st-001~004와 같은 표기).
2. 새 쿼리 → `supabase/clear-rehearsal-data.sql` → **Run**. 에뮬레이터 리허설이 올린 SIMULATED 기록(충전·리뷰·실패 취소·크루즈 해제/통과)을 지운다. SEED·LIVE는 그대로, 설치(app_installations)도 남겨 이미 깔린 앱은 그대로 쓴다. 첫 결과 표에 지울 건수가 나온다.
3. 새 쿼리 → `supabase/refresh-demo-dates.sql` → **Run**(시드 날짜를 오늘로).
4. 새 쿼리 → `supabase/verify.sql` → **Run**. 기대: versions에 `20261107000100`이 추가되어 5행, seed_counts.charge_sessions = **325**(SEED만 세므로 2번과 상관없이 같음).
5. 확인(선택): 차슐랭 S1에서 st-005~010 이름이 바뀌고, 시흥동 주민센터 인증·7일 실패가 시드 값으로 돌아왔는지. 크루즈 S2 구간 이름이 "판교IC 진입로"인지.

리허설을 다시 하면 기록이 또 쌓인다 → **촬영 직전에** 2~3번을 다시 돌린다. CLI 이력은 `migration repair --status applied 20261107000100`.

## 재실행·오류 처리

- initial migration은 **한 번만 적용**한다. 이미 적용된 경우 테이블 중복 오류가 나며 transaction이 rollback된다. 오류 뒤 SQL Editor가 transaction aborted 상태라면 `ROLLBACK;`만 실행하고 원인을 확인한다. 성공한 initial migration을 지우거나 다시 만들지 않는다.
- demo migration은 적용 버전이 있으면 전체 입력을 건너뛴다. 재실행으로 중복하거나 시연 날짜를 갱신하지 않는다. 시연 날짜 갱신이 필요하면 별도 migration으로 관리한다.
- `DROP TABLE`, `DROP SCHEMA`, `db reset`은 사용하지 않는다. 수정이 필요하면 별도 migration으로 추가한다.
- initial SQL의 DELETE는 **철회 함수의 정의 안**에 있으며 migration 적용 시 호출되지 않는다. 실제 사용자 ID로 철회 함수를 테스트하지 않는다.
- 데이터 이전 대상은 현재 없다. 이 작업은 새 schema와 가상 시드 데이터를 넣는 초기 migration이다. 기존 사용자 기록을 변환·이전하는 스크립트는 아니다.

## API·후속 범위

- 인증 후 호출: pleos_register_installation, pleos_upload_charge, pleos_submit_review, pleos_retract_failure, pleos_withdraw_installation, pleos_upload_pass, pleos_upload_disengage, pleos_my_charges, pleos_nearby_stations, pleos_station_detail, pleos_nearby_risk.
- 조회는 기본 p_demo=false로 실측 데이터만 본다. 시연 연결에는 p_demo=true와 SIMULATED 설치 등록을 사용한다. 원본을 다른 설치에 노출하지 않는다.
- 차슐랭 점수는 initial에서 평균 별점만 썼고, 11-03 migration `20261103000100`에서 **평균 별점 × (1 − 최근 30일 실패율)**로 바뀐다(아래 "11-03 추가 migration").
- 실패 정정은 계측 데이터 없는 charge_retractions 영수증도 저장하여, 업로드보다 정정이 먼저 도착해도 과거 업로드가 기록을 되살리지 못하게 한다. 동의 철회 시 이 영수증도 삭제한다.
- 시연 데이터의 이름·출력·평가·주행 기록은 검증된 현실 정보가 아니다. 기존 4곳의 시드 ID를 유지하고 추가 6곳은 가상 명칭으로 표시한다.
- 원본·REVOKED 설치·Auth·백업 보관 정책, 동의 문구, 비정상 사용 제한과 앱 인증·영속 outbox 구현은 후속 과제다. 자동 만료/정리 작업은 아직 없다.
- 조회 위치는 저장하지 않지만 현재 반경 RPC 호출 시 서버로 전달된다. 기존 기록 업로드의 좌표 미전송 원칙과 구분해 문구·앱 계약을 확인해야 한다.

## 로컬 검증

server/에서 `pnpm install --frozen-lockfile` 후 `node --test tests/database.test.mjs`.

원격 Auth/JWT 대신 테스트 전용 auth.users/auth.uid()와 anon/authenticated 역할을 만든 별도 메모리 PostgreSQL에서 실제 migration SQL을 실행한다. SQL 문법·제약·RLS·RPC·시드 집계는 실행 검증하지만 Supabase 전체 서비스나 동시 실행을 완전히 재현하는 것은 아니다.


## 11-09 추가 별점 분포

검토 후 `20261109000100_demo_rating_variation.sql`을 적용한다. 화면의 평균 별점이 약 3.2~4.3점으로 구분된다. `20261107000100_demo_names.sql`과 함께 CLI dry-run으로 대상을 확인한다. 적용 뒤 `refresh-demo-dates.sql`, `verify.sql`을 실행하면 버전 6개·충전 시드 325건이어야 한다. 리허설 기록 삭제는 촬영 시점을 정해 별도로 실행한다. 자세한 범위와 테스트는 [피드백 확인 기록](review-20261109.md)에 있다.

## 11-10 앱 이름 변경: CRUISEBREAKER → CRONG

크루즈 앱 이름이 크롱(CRONG)으로 바뀌어(11-10 장호연 결정) 서버 앱 코드도 `CRONG`으로 바꾼다. `20261110000100_rename_crong.sql`이 기존 설치(시드·실제)의 `app`을 `CRONG`으로 바꾸고, CHECK 제약과 앱 이름을 쓰는 함수 4개(`pleos_register_installation`, `pleos_upload_pass`, `pleos_upload_disengage`, `pleos_nearby_risk`)를 다시 만든다. 다시 실행해도 된다.

**적용 순서는 자유다.** 등록 함수가 예전 코드 `CRUISEBREAKER`를 `CRONG`의 별칭으로 받아 주므로, 이름을 바꾸기 전 APK도 적용 뒤 그대로 동작한다. 반대로 크롱 APK(`CRONG`을 보냄)는 **적용 전에는 등록이 거절돼 기록 업로드가 안 된다**(근처 구간 조회는 됨). 적용 뒤 `refresh-demo-dates.sql`(이제 `CRONG` 기준), `verify.sql`을 실행한다. 버전은 7개가 된다.

**2026-11-11 원격 적용 완료.** 별점 분포와 크롱 앱 코드 전환을 CLI로 적용하고 시연 시드 날짜를 갱신했다. 원격 migration 7건, 대기 0건, 충전 시드 325건·크롱 해제 400건·통과 2,000건, RLS 및 인증 전용 RPC 권한을 확인했다. 로컬 DB 테스트 21개와 실제 API 통합 검증이 통과했다. 크롱 등록·업로드·주변 구간 조회를 확인하고 검증용 계정과 기록을 정리했다.
