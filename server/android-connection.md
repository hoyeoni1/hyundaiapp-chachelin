# Android 앱의 Supabase 연결 (2026-11-03)

현재 두 저장소의 로컬 브랜치 `codex/supabase-foundation-20261102`에서 **제품 앱 저장소를 실제 Supabase에 연결했다.** 기존 SDK raw probe와 다른 작업이며, 제품 core의 `backend` 패키지는 release에도 포함되는 공통 인증·전송 구현이다. core 버전은 0.5.0이다. 서버 작업의 원격 push/새 PR은 아직 하지 않았다.

## 차슐랭 의존성

차슐랭 main에는 아직 충전 서비스·기록기 연결이 없으므로 이상준의 `origin/feat/charge-session-logic`(737fb38)을 이 로컬 브랜치에 merge했다. 원격 PR #3이나 main은 머지하지 않았다. 새 서버 PR을 제출할 때 이 의존성과 타인의 구현 범위를 명시해야 한다. 그 PR의 Fake 중복 문제는 이번 실제 서버 전환과 별개이며 Fake 원본은 고치지 않았다.

## 이 PC의 빌드 설정

각 저장소의 Git 제외 `local.properties`에 아래 공개 설정을 추가했다. sdk.dir 등 기존 값은 보존한다.

```properties
supabase.url=https://YOUR_PROJECT_REF.supabase.co
supabase.publishableKey=sb_publishable_REPLACE_ME
```

- app BuildConfig에 공개 URL/키만 들어간다. 관리자 키·DB 비밀번호·사용자 토큰은 없다.
- 설정이 없으면 기존 Fake를 사용한다. 이 PC의 두 debug APK는 실제 프로젝트로 설정했다.
- debug는 SIMULATED 설치 및 DEMO 집계를 사용한다. release는 LIVE 집계만 조회한다. 현재 충전소는 시드뿐이므로 release의 실측 목록을 채우려면 환경부 API 캐시가 필요하다.
- debug/live 및 프로젝트/앱별 인증 파일·Keystore 별칭을 분리했다. debug에서 만든 인증을 release에서 재사용하지 않는다.
- app에 android.permission.INTERNET 추가. HTTPS 인증서 검증은 Android 기본 동작을 유지한다.

## 인증·전송

1. 기존 동의 완료 상태 또는 기록 시작 시 Backend를 활성화한다. 동의 전의 새 설치는 서버 인증/등록을 시작하지 않는다.
2. 익명 인증을 생성하고 같은 동의 세대의 가명 ID로 설치를 등록한다.
3. access/refresh token은 Android Keystore AES-GCM으로 암호화한 AtomicFile에 저장한다. 파일은 noBackupFilesDir에 있어 Android 백업에서 제외된다. 재실행 시 복원하고 만료/401이면 refresh token을 갱신한다.
4. 앱 저장소 upload는 요청을 **로컬 영속 outbox에 기록한 뒤 반환**한다. 실제 서버 확정은 worker ACK 시점이다. FIFO로 세션 다음 후기를 보내고, 같은 키 재전송은 서버가 한 건으로 처리한다.
5. 네트워크/5xx 오류는 5초/15초/1분/5분으로 재시도한다. 영구적인 400/403/404/409/422 오류는 실패 상태로 보관하고 무한 재시도하지 않는다.
6. 실패 정정은 아직 전송하지 않은 해당 세션/후기를 제거하고 정정 영수증 요청을 보내 늦은 업로드도 막는다.
7. 철회는 일반 작업을 비우고 철회 의도를 먼저 영속 기록한 뒤 기존 앱 설정을 지운다. 서버 삭제 ACK까지 옛 인증을 보관한다. 재동의는 새 동의 세대이므로 과거 삭제 요청의 토큰을 덮어쓰지 않는다.

표준 로그 태그는 `SupabaseSync`이며 connected/retry/rejected 같은 상태 코드만 남긴다. JWT·요청 본문·원시 Auth 응답은 로그에 남기지 않는다. 동의 버전은 아직 개발용 development-v1이며 정식 약관 버전은 별도 확정해야 한다.

## 제품 저장소 연결

- 차슐랭: SupabaseStationRepository가 목록/상세/최근접, SupabaseChargeRecordRepository가 세션/후기/정정/철회/내 기록을 담당한다. History Flow는 queue 변경/전송 성공 때 원격 기록을 갱신한다. 목록·상세는 화면 진입 시 다시 조회한다.
- 크루즈: SupabaseCruiseServer가 기존 CruiseEventRepository와 SegmentRiskRepository를 구현한다. 현재 앱 판정·방향/거리 안내·TTS는 유지하고 업로드와 위험도 조회만 서버로 교체한다.
- 두 앱 AppContainer가 동일 제품 backend를 생성한다. 실제 SDK 판정 로직·UI 테마를 새로 작성한 것은 아니다.

## 확인한 결과

- 실제 익명 로그인 활성화 확인.
- 원격 HTTP 통합: 두 독립 익명 사용자 로그인, 세션 중복 방지, 후기, 타 사용자 집계 반영, 타인 원본 접근 거절, 크루즈 통과/해제, 토큰 갱신, 철회/늦은 업로드 거절 통과. 테스트용 Auth 계정과 업무 기록은 해당 run UUID로 한정해 정리했다.
- 두 앱 debug/release 빌드 및 기존 앱 단위 테스트 통과. 앱 테스트는 차슐랭 각 63, 크루즈 각 44. 공통 core는 debug 28/release 14(신규 backend 테스트 11 포함) 통과. 앱 lint 오류 0, 경고 차슐랭 10/크루즈 13.
- Pleos API 34 emulator-5554, user 10에서 **두 앱 모두** production backend/HTTPS/Keystore/AtomicFile을 사용해 익명 인증·조회·암호화 세션 재로드·영속 outbox 쓰기·철회 ACK와 로컬 인증 제거 통과.
- 위 Android 검사는 별도 debug 전용 BackendConnectionCheckActivity에서 수행했다. 제품 동의 상태·개인 기록을 바꾸지 않는 독립 테스트 저장소를 사용한다. shell의 DUMP 권한이 있어야 실행 가능하고 release에는 없다.
- APK 업데이트로 INTERNET 권한을 처음 추가했을 때 첫 실행이 EPERM을 반환했다. 앱 force-stop 후 재실행해서 네트워크 정상 수신을 확인했다.

## Android 연결 재검사

최신 debug APK를 설치한 뒤, 연결된 에뮬레이터의 현재 사용자로 실행한다.

```powershell
adb shell am start --user 10 -n com.hyundaiapp.chachelin/.BackendConnectionCheckActivity
adb shell run-as com.hyundaiapp.chachelin --user 10 cat files/backend-check-result.json

adb shell am start --user 10 -n com.hyundaiapp.cruisebreaker/.BackendConnectionCheckActivity
adb shell run-as com.hyundaiapp.cruisebreaker --user 10 cat files/backend-check-result.json
```

완료 결과는 ok=true와 네 가지 checks다. 실제 익명 테스트 사용자/설치를 만들므로 무한 반복하지 않는다. 정상 검사에서는 업무 기록 삭제와 로컬 토큰 제거까지 수행하며, 서버의 최소 REVOKED 설치/Auth 정리는 별도 관리 작업이다. 이 턴에서 식별된 테스트 설치는 CLI로 정리했고, 실패한 검사에서 식별되지 않은 최소 차단 정보가 남아 있을 수 있다. 이는 시드/점수/위험도에는 포함되지 않는다.

## 남은 범위

- 모든 화면을 실제 SDK 주입부터 끝까지 재검사한 것은 아니다. 이번 Android 검사는 제품 backend의 독립 통합 검사이며, 기존 충전/크루즈 도메인은 단위 테스트와 팀의 선행 시연에 의존한다.
- outbox와 철회 재시도는 앱 프로세스가 살아 있거나 다음 앱 실행 때 진행한다. WorkManager 기반의 프로세스 종료 후 독립 재시작은 아직 없다. 오프라인 철회가 끝나지 않은 상태를 사용자에게 보여 주는 UI도 후속 과제다.
- 내 기록은 서버 조회 성공 결과를 메모리에 유지한다. 앱 종료 후 오프라인에서 전체 과거 기록을 다시 보여 주는 영속 읽기 캐시는 아직 없다. 전송 대기 중인 새 기록은 서버 ACK 후 History에 반영된다.
- 차슐랭 최근접 충전소의 오프라인 fallback은 메모리 캐시뿐이다. 앱을 새로 시작한 오프라인 세션의 충전소 매칭은 기존 PR #3의 NOT_AT_STATION 한계를 유지한다.
- 조회 시 현재 위치는 반경 RPC로 전달되지만 DB에는 저장하지 않는다. 수집 동의 문구·원본 및 Auth 보관/정리 정책·환경부 API 캐시는 후속으로 확정한다.
