# 공통 Supabase 서버

차슐랭과 크롱이 사용하는 서버의 원본은 이 디렉터리다. 담당은 황준용. SDK 진단 PR과 분리한 `codex/supabase-foundation-20261102`에서 작업한다.

**처음 읽는 팀원:** [두 앱 공통 서버 설명서](../docs/server-overview.md)에서 아키텍처·테이블·RPC·권한·전송·철회·운영과 미완료 범위를 한 번에 확인할 수 있다. 현재 Android 공통 버전은 core v0.5.1이다.

스키마 및 전체 서비스 흐름: [schema-design.md](schema-design.md). **2026-11-02 원격 적용·CLI 연결·migration 이력 정합 확인 완료.** 로컬 PostgreSQL 테스트 15개 통과. [적용 방법 및 검증 상태](deploy.md).

원격 확인: 두 버전, 시드 10/300/400/2000, 테이블 9개 RLS, RPC 11개 권한 정상. 함수 fingerprint는 로컬과 일치(f0b69344326a5769b916cafeb7698bbe). 사용자 수동 적용을 검증한 뒤 CLI 표준 이력에 두 버전을 applied 등록했고 db push --dry-run은 upToDate=true·대기 migration 없음. SQL 본문·시드 재실행은 하지 않았다. [CLI 사용법](cli-setup.md).

**2026-11-03 Android 연결 완료:** 두 제품 앱 저장소가 공통 core v0.5.1 backend를 사용하도록 연결했다. 실제 익명 JWT HTTP 테스트와 두 앱의 Pleos/Keystore/영속 outbox 통합 검사를 통과했다. 현재 PC의 debug 빌드는 실제 Supabase 시연 데이터를 사용한다. [설정·검증·남은 범위](android-connection.md).

## 프로젝트 연결 확인 (2026-11-02)

- 프로젝트 ref: `qbdikckaqroxbpacvtqo`. 사용자가 선택한 지역은 Asia Pacific이며 구체적인 AWS 리전은 아직 확인하지 않았다.
- 공개 URL·Publishable key·JWKS URL만 git에서 제외한 `.env.local`에 설정했다. 원격 HTTP 테스트가 이 파일을 읽고 Android 빌드는 별도 local.properties의 공개 URL/키를 읽는다.
- `/auth/v1/health`, `/auth/v1/settings`: HTTP 200.
- 초기 생성 전 `GET /rest/v1/stations?select=id&limit=1`은 HTTP 404/PGRST205였다. 현재 원본은 의도적으로 private 스키마에 있고 조회는 인증된 RPC로만 제공한다.
- REST 루트 문서 조회의 HTTP 401은 `Secret API key required` 응답이다. 일반 테이블 API의 공개 키 오류로 해석하지 않는다.
- Auth 설정의 `external.anonymous_users`: true(사용자 활성화 후 재확인). 실제 익명 사용자 로그인·토큰 갱신·소유권·집계 공유·철회를 테스트했다.
- 관리자 Secret key는 저장·사용하지 않았다. 채팅에 제공된 키는 대시보드에서 폐기 후 재발급해야 한다. 새 Secret key는 채팅이나 앱에 넣지 않는다.
- 테이블·함수·시드는 사용자가 SQL Editor에서 적용했고 에이전트가 CLI 검증 및 표준 migration 이력 정합을 완료했다. 후속 통합 테스트에서 임시 익명 계정·시연 기록을 생성해 검증했으며 식별된 테스트 데이터는 정리했다. Auth 설정은 사용자가 변경했다.

## 인증·데이터 계약 방향

앱에서 동의한 뒤 Supabase Anonymous Sign-In으로 이메일 없는 인증 세션을 만든다. 공개 키만으로 개인 기록을 쓰거나 삭제할 수 있게 하지 않는다. 가명 차량 UUID는 기존 앱 계약을 유지하되, 소유권은 서버가 검증한 `auth.uid()`에 연결한다. JWT 자체를 직접 구현하거나 DB 비밀번호를 앱에 넣지 않는다.

- 차슐랭: `stations`, `charge_sessions`, `reviews`, 충전소 집계. 세션 ID 재전송은 한 건으로 유지하고 SUCCESS 세션에만 리뷰를 허용한다. 신규 NOT_AT_STATION/충전소 미연결 기록은 기기 암호화 저장소에만 두고 서버 outbox에 넣지 않는다. 기존 SQL의 nullable 계약은 호환성을 위해 유지한다. 최신 기준은 이상준 PR #3의 저장소 계약이다.
- 크루즈: 해제·통과 기록 및 위험도 집계. 원래 좌표·차속을 저장하지 않는다. geohash 7자리+heading8, 한국 날짜, 같은 방향 9칸의 차량-일 중복 제거, 최근 28일, 해제율 30% 이상·분모 20 이상을 따른다.
- 개인 원본 데이터와 집계를 분리한다. 개인 데이터는 본인만 읽고 쓰며, 집계 응답에 다른 기기의 차량 ID·인증 ID를 노출하지 않는다.
- 동의 철회: 기록 중지 후 서버 삭제 완료 또는 영속 삭제 대기 작업 보존까지 기존 인증 세션을 유지한다. 이전 ID를 먼저 지워 삭제를 재시도하지 못하게 하는 흐름은 피한다. 자세한 재동의·인증 정리 절차는 앱 담당자와 맞춘다.
- 환경부 API 키는 서버 함수 비밀값으로 관리한다. 현재 환경부 API 연결·키 신청은 별도 과제다.

## 다음 작업

1. 실제 제품 화면에서 SDK 값 주입 → 충전/크루즈 기록 → 서버 반영 → 다른 기기 조회까지 시연 리허설.
2. WorkManager 기반 프로세스 종료 후 재전송, 철회 대기 UI와 영속 읽기 캐시 보완. 현재는 프로세스 생존/다음 앱 실행에서 재시도한다.
3. 실제 환경부 API 캐시와 약관·보관 정책 확정. 원격 다중 연결의 철회-업로드 경쟁 검사도 후속이다.

작성 완료: private 테이블 9개(앱 7 + 처리 영수증/버전 관리 2), 제약·인덱스·RLS, 인증 RPC 11개, 시연 충전소 10곳·충전 세션 300건·해제 400건·통과 2,000건. 처음에는 실패 감점 없이 별점 평균만 썼고, 11-03 migration 2개(`20261103000100` 점수 = 별점 × (1 − 30일 실패율), `20261103000200` 실패 잦은 시연 충전소 st-004)를 추가했다(원격 적용 전, [deploy.md](deploy.md) "11-03 추가 migration").

## 사용자가 대시보드에서 준비할 것

1. Settings → API Keys에서 채팅에 제공했던 Secret key 폐기. 필요하면 대체 키를 만들되 전달하지 않는다.
2. Authentication 설정에서 **Allow anonymous sign-ins / Anonymous Sign-In** 활성화. 메뉴 위치는 대시보드 버전에 따라 다를 수 있다.
3. URL·Publishable key는 이미 받았으므로 다시 보낼 필요가 없다. SQL 배포는 migration이 준비된 뒤 안내한다.

공식 문서: [키 종류](https://supabase.com/docs/guides/getting-started/api-keys), [익명 인증](https://supabase.com/docs/guides/auth/auth-anonymous).

시드 날짜가 오래되면 [시연 날짜 갱신 절차](demo-maintenance.md)를 따른다. 원래 migration은 재실행하지 않는다.

[11월 9일 피드백 확인](review-20261109.md)에 별점 분포 조정, PR #36 SQL 검토, 프로젝트 리전·로그 근거, core v0.5.2 검토를 정리했다.

[11월 10일 담당자 전달 답변](review-20261109.md#2026-11-10-담당자-전달-답변)에 리전·실제 요청 로그·core v0.5.2 확인 완료 답을 모았다. [실제 로그 검증과 문구 제안](log-verification-20261109.md)은 약관 담당자의 참고 자료이며 확정 약관이 아니다.
