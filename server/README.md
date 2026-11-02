# 공통 Supabase 서버

차슐랭과 크루즈브레이커가 사용하는 서버의 원본은 이 디렉터리다. 담당은 황준용. SDK 진단 PR과 분리한 `codex/supabase-foundation-20261102`에서 작업한다.

스키마 및 전체 서비스 흐름: [schema-design.md](schema-design.md). 실행 가능한 migration 2개와 로컬 PostgreSQL 검증 15개를 준비했다. **원격 DB에는 아직 적용하지 않았으며 사용자가 SQL Editor에서 직접 실행하기로 했다.** [적용 순서와 기대 결과](deploy.md).

## 프로젝트 연결 확인 (2026-11-02)

- 프로젝트 ref: `qbdikckaqroxbpacvtqo`. 사용자가 선택한 지역은 Asia Pacific이며 구체적인 AWS 리전은 아직 확인하지 않았다.
- 공개 URL·Publishable key·JWKS URL만 git에서 제외한 `.env.local`에 설정했다. 이 파일을 읽는 배포/Android 연동은 아직 구현하지 않았다.
- `/auth/v1/health`, `/auth/v1/settings`: HTTP 200.
- Publishable key를 사용한 `GET /rest/v1/stations?select=id&limit=1`: HTTP 404/PGRST205. 아직 `public.stations`가 schema cache에 없다.
- REST 루트 문서 조회의 HTTP 401은 `Secret API key required` 응답이다. 일반 테이블 API의 공개 키 오류로 해석하지 않는다.
- Auth 설정의 `external.anonymous_users`: false. Anonymous Sign-In 활성화가 필요하다. 확인 중에는 가입 요청이나 테스트 계정을 생성하지 않았다.
- 관리자 Secret key는 저장·사용하지 않았다. 채팅에 제공된 키는 대시보드에서 폐기 후 재발급해야 한다. 새 Secret key는 채팅이나 앱에 넣지 않는다.
- 원격 SQL 실행·테이블 생성·함수 배포·Auth 설정 변경은 아직 하지 않았다.

## 인증·데이터 계약 방향

앱에서 동의한 뒤 Supabase Anonymous Sign-In으로 이메일 없는 인증 세션을 만든다. 공개 키만으로 개인 기록을 쓰거나 삭제할 수 있게 하지 않는다. 가명 차량 UUID는 기존 앱 계약을 유지하되, 소유권은 서버가 검증한 `auth.uid()`에 연결한다. JWT 자체를 직접 구현하거나 DB 비밀번호를 앱에 넣지 않는다.

- 차슐랭: `stations`, `charge_sessions`, `reviews`, 충전소 집계. 세션 ID 재전송은 한 건으로 유지하고 SUCCESS 세션에만 리뷰를 허용한다. NOT_AT_STATION 결과와 station_id null을 수용한다. 최신 기준은 이상준 PR #3의 저장소 계약이다.
- 크루즈: 해제·통과 기록 및 위험도 집계. 원래 좌표·차속을 저장하지 않는다. geohash 7자리+heading8, 한국 날짜, 같은 방향 9칸의 차량-일 중복 제거, 최근 28일, 해제율 30% 이상·분모 20 이상을 따른다.
- 개인 원본 데이터와 집계를 분리한다. 개인 데이터는 본인만 읽고 쓰며, 집계 응답에 다른 기기의 차량 ID·인증 ID를 노출하지 않는다.
- 동의 철회: 기록 중지 후 서버 삭제 완료 또는 영속 삭제 대기 작업 보존까지 기존 인증 세션을 유지한다. 이전 ID를 먼저 지워 삭제를 재시도하지 못하게 하는 흐름은 피한다. 자세한 재동의·인증 정리 절차는 앱 담당자와 맞춘다.
- 환경부 API 키는 서버 함수 비밀값으로 관리한다. 현재 환경부 API 연결·키 신청은 별도 과제다.

## 다음 작업

1. 사용자 SQL Editor 실행: 사전 확인 → initial schema → demo data → verify. Publishable key는 schema 변경 권한이 없다.
2. 원격 적용 결과 확인 및 Supabase 실제 인증·API·동시 실행 검증.
3. 앱에 인증·HTTP 저장소와 영속 재전송/삭제 작업 연결. 기존 Fake와 선택 가능하게 구성하고 두 에뮬레이터 공유 확인.

작성 완료: private 테이블 9개(앱 7 + 처리 영수증/버전 관리 2), 제약·인덱스·RLS, 인증 RPC 11개, 시연 충전소 10곳·충전 세션 300건·해제 400건·통과 2,000건. 실패 감점 없이 별점 평균과 실패 횟수를 각각 제공한다.

## 사용자가 대시보드에서 준비할 것

1. Settings → API Keys에서 채팅에 제공했던 Secret key 폐기. 필요하면 대체 키를 만들되 전달하지 않는다.
2. Authentication 설정에서 **Allow anonymous sign-ins / Anonymous Sign-In** 활성화. 메뉴 위치는 대시보드 버전에 따라 다를 수 있다.
3. URL·Publishable key는 이미 받았으므로 다시 보낼 필요가 없다. SQL 배포는 migration이 준비된 뒤 안내한다.

공식 문서: [키 종류](https://supabase.com/docs/guides/getting-started/api-keys), [익명 인증](https://supabase.com/docs/guides/auth/auth-anonymous).
