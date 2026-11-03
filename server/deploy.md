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

## 재실행·오류 처리

- initial migration은 **한 번만 적용**한다. 이미 적용된 경우 테이블 중복 오류가 나며 transaction이 rollback된다. 오류 뒤 SQL Editor가 transaction aborted 상태라면 `ROLLBACK;`만 실행하고 원인을 확인한다. 성공한 initial migration을 지우거나 다시 만들지 않는다.
- demo migration은 적용 버전이 있으면 전체 입력을 건너뛴다. 재실행으로 중복하거나 시연 날짜를 갱신하지 않는다. 시연 날짜 갱신이 필요하면 별도 migration으로 관리한다.
- `DROP TABLE`, `DROP SCHEMA`, `db reset`은 사용하지 않는다. 수정이 필요하면 별도 migration으로 추가한다.
- initial SQL의 DELETE는 **철회 함수의 정의 안**에 있으며 migration 적용 시 호출되지 않는다. 실제 사용자 ID로 철회 함수를 테스트하지 않는다.
- 데이터 이전 대상은 현재 없다. 이 작업은 새 schema와 가상 시드 데이터를 넣는 초기 migration이다. 기존 사용자 기록을 변환·이전하는 스크립트는 아니다.

## API·후속 범위

- 인증 후 호출: pleos_register_installation, pleos_upload_charge, pleos_submit_review, pleos_retract_failure, pleos_withdraw_installation, pleos_upload_pass, pleos_upload_disengage, pleos_my_charges, pleos_nearby_stations, pleos_station_detail, pleos_nearby_risk.
- 조회는 기본 p_demo=false로 실측 데이터만 본다. 시연 연결에는 p_demo=true와 SIMULATED 설치 등록을 사용한다. 원본을 다른 설치에 노출하지 않는다.
- 차슐랭 점수는 우선 **평균 별점**(리뷰 5개 미만은 null). 실패 횟수는 별도 제공하며 실패 감점은 넣지 않았다. 이 임시 선택을 최종 제품 점수 공식으로 간주하지 않는다.
- 실패 정정은 계측 데이터 없는 charge_retractions 영수증도 저장하여, 업로드보다 정정이 먼저 도착해도 과거 업로드가 기록을 되살리지 못하게 한다. 동의 철회 시 이 영수증도 삭제한다.
- 시연 데이터의 이름·출력·평가·주행 기록은 검증된 현실 정보가 아니다. 기존 4곳의 시드 ID를 유지하고 추가 6곳은 가상 명칭으로 표시한다.
- 원본·REVOKED 설치·Auth·백업 보관 정책, 동의 문구, 비정상 사용 제한과 앱 인증·영속 outbox 구현은 후속 과제다. 자동 만료/정리 작업은 아직 없다.
- 조회 위치는 저장하지 않지만 현재 반경 RPC 호출 시 서버로 전달된다. 기존 기록 업로드의 좌표 미전송 원칙과 구분해 문구·앱 계약을 확인해야 한다.

## 로컬 검증

server/에서 `pnpm install --frozen-lockfile` 후 `node --test tests/database.test.mjs`.

원격 Auth/JWT 대신 테스트 전용 auth.users/auth.uid()와 anon/authenticated 역할을 만든 별도 메모리 PostgreSQL에서 실제 migration SQL을 실행한다. SQL 문법·제약·RLS·RPC·시드 집계는 실행 검증하지만 Supabase 전체 서비스나 동시 실행을 완전히 재현하는 것은 아니다.
