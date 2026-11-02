# 인수인계 — 이상준 (차슐랭 로직)

담당 경로: 차슐랭 로직 — `app/src/main/java/com/hyundaiapp/chachelin/` 아래 `domain/`, `data/`(인터페이스 합의), ViewModel, `AppContainer`·`ChachelinApp`·`RecordingService`. 화면(`ui/` Screen·테마)은 길민종

## 현재 상태 (2026-11-02 기준, 매번 덮어쓰기)

- 한 일 요약:
  - **코드 완료, 단위 테스트 통과(52개). 에뮬레이터 실행은 아직 안 해 봄**(이 PC에 Pleos 이미지 AVD가 없음). 포그라운드 서비스·화면 전환·부팅 자동 시작은 빌드만 확인했고 실제 동작은 미확인
  - 판정(F1~F4): `ChargeSessionTracker` + `ChargeRules.judge`. 충전소 매칭(150m)은 충전구를 열 때 따로 조회, 닫을 때 최대 10초 기다림
  - 업로드·재시도: `ChargeSessionRecorder`(5초·15초·1분·5분 간격, 5분으로 계속). 리뷰는 세션 업로드 후 전송, [실패 아님]은 업로드를 멈추고 서버에서 삭제
  - 화면 연결: S2~S7 ViewModel + `ChachelinNav` 연결. 충전구를 열면 S3, 닫으면 S4/S5로 자동 전환. 길안내(F6)는 S2 확인 후 `requestRoute`
  - 앱 수명: `RecordingService`(specialUse) + `BootReceiver`. 크루즈브레이커(이민하)와 같은 구조. **동의 전에는 차량·위치 신호를 받지 않음**
  - 동의 철회: S6 헤더 [동의 철회] → 확인 창 → 서비스 정지, 내 기록·동의·차종·가명 차량 ID 삭제
  - 아직 없는 것: 서버 저장소(Fake 사용 중 → 충전해도 S1·S2의 점수·실패 수가 안 바뀜), S2 실측 속도 분포
- 진행 중 브랜치·PR: `feat/charge-session-logic` (푸시함, PR 생성·리뷰·머지 대기)
- 다음 할 일(우선순위 순):
  1. Pleos 이미지 에뮬레이터에서 아래 "확인 방법" 순서대로 실행. 특히 앱을 뒤로 보낸 채 충전구를 열고 닫을 때 기록이 남는지(PRD 8절 "같은 해법을 충전 상황에서 확인")
  2. 앱이 뒤에 있을 때 충전이 끝나면 S4/S5가 앞으로 뜨지 않는다(Android의 백그라운드 화면 시작 제한). 앱을 열어야 보임. 결과 알림(눌러서 S4/S5로)이 필요한지 에뮬레이터에서 확인 후 결정
  3. 서버 구현이 나오면 Fake 교체 후 시연 5·6단계(점수·실패 수 변화) 확인
- 막힌 점·질문(누구에게):
  - 장호연: ① **시연 kW가 비현실적** — `demo-charge.sh`는 약 15초에 SoC 40→80%를 올려서 S3·S4에 약 7,400kW가 표시됨. 스크립트를 느리게 할지, 시연 표시를 바꿀지 결정 필요 ② 시연 스크립트는 **먼저 개발 메뉴 S7에서 동의·차종 선택**을 해야 기록이 돈다(스크립트 주석에 추가 필요) ③ PRD 5.3에 없는 경우를 아래 "결정"처럼 정함 — 이견 있으면 알려 주기 ④ 포그라운드 서비스 퍼미션 4개 추가(주최 측 문의 5번과 같은 조건)
  - 황준용: `data/` 인터페이스 변경 — `ChargeRecordRepository.upload(record, vehicleId)`(같은 세션 id는 한 건만), `retract(sessionId)`(없어도 오류 아님), `deleteMine(vehicleId)`, `StationRepository.detail(stationId)`. 재시도 대기열은 메모리뿐이라 앱이 죽으면 못 보낸 기록이 사라짐 → 저장소 구현에서 쌓아 두고 재전송 필요(크루즈브레이커와 같은 방식). `getEvBatteryCapacity` 퍼미션은 계속 확인 필요. `core`의 `:core:lintDebug`가 `MissingPermission` 등 오류 28개로 실패함(`NaviSource.kt` 등, 이번 변경과 무관)
  - 길민종: **담당 경로 밖을 고쳤음(이민하 PR과 같은 방식)** — `ChachelinNav.kt`를 ViewModel로 교체(임시 상태 삭제, 할 일 2번 끝남), S6 `HistoryScreen`에 `onWithdrawConsent`와 [동의 철회] 버튼 추가(모양은 바꿔도 됨). 질문 답: ① S2~S7 ViewModel 완료 ② S4 [나중에] = 세션 기록만 남기고 리뷰 없이 닫기 ③ 속도 분포는 서버 집계가 정해지면 UiState에 함께 추가 ④ S6 `stationName == null` = 충전소 아님, 맞음. 합의 필요: S2 불러오기 실패 시 "불러오는 중"이 계속 보임(`StationDetailUiState`에 error 필드 추가 제안), S7에서 알림 권한 요청
- 주의(함정·직접 겪은 이상한 동작):
  - **기록은 S7 동의 후에만 돈다.** 디버그 빌드도 마찬가지 → 새로 설치했으면 개발 메뉴 S7부터. 그 전에는 신호가 안 들어와 S1이 로딩에 머문다
  - **Windows에서 프로젝트 경로에 한글이 있으면 AGP가 빌드를 거부한다**(`-Pandroid.overridePathCheck=true`로도 안 됨). 저장소 설정은 건드리지 않고 영문 경로로 옮기거나 복사해서 빌드
  - 앱이 다시 살아날 때 충전구가 이미 열려 있으면 그때를 세션 시작으로 본다 → 충전 시간·ΔSoC가 실제보다 작게 잡힘
  - 충전소 조회 실패·위치 없음은 "충전소 아님"으로 기록된다 → 서버 연결 후 네트워크가 끊기면 인증 충전이 빠질 수 있음
  - 에뮬레이터 SoC는 충전 중에도 자동으로 안 오름 → 콘솔로 주입. 위치는 판교 부근 고정(시드 st-003과 같은 좌표)
  - `getEvBatteryCapacity`는 `CAR_ENERGY`만으로 "need Permission" → 차종 프리셋(S7) 값을 씀. SDK 값이 오면 그쪽이 먼저
  - 시연용 기준값은 `ChargeRules.DEMO`(디버그 빌드 자동 적용)
- 확인 방법:
  - `./gradlew assembleDebug test`, `./gradlew :app:lintDebug`
  - 시연: 설치 → 개발 메뉴 S7 동의 2개 + 차종 + 시작하기 → `bash tools/vconsole.sh up` → `put 'ev-battery?field=charge_port_open&value=true'` → S3 → `put 'ev-battery?field=soc&value=50'` … → 10초 뒤 `put 'ev-battery?field=charge_port_open&value=false'` → S4. 실패는 SoC 그대로 30초 뒤 닫기 → S5
  - 서비스: `adb shell dumpsys activity services com.hyundaiapp.chachelin | grep RecordingService`

## 작업 기록 (최신이 위, 지우지 않음)

### 2026-11-02 (이상준, Claude 에이전트와 작업)
- 한 일: `feat/charge-session-logic` — `ChargeSessionTracker` 구현, `ChargeRules.judge`, `ChargeSessionRecorder`(업로드·재시도), `VehiclePreset`, `VehicleProfileStore`, S2~S7 ViewModel, `ChachelinNav` 연결, `RecordingService`·`BootReceiver`, 동의 철회, 가명 차량 ID 업로드, Fake 저장소에 상세 시드. 테스트 2 → 52개
- 결정한 것과 이유:
  - PRD 5.3에 없는 경우는 기록하지 않음: 충전소에서 충전구만 잠깐 연 경우, ΔSoC 1%p, ΔSoC ≥ 2%p인데 시간 미달. 충전 성공도 실패도 아니어서 점수를 흐리지 않게 함
  - 충전소 밖에서 SoC가 오르지 않은 경우도 기록하지 않음: 충전이 아니므로 "집 충전" 기록이 되면 안 됨
  - 충전 시간은 충전구 열림~닫힘(PRD 5.3 세션 시작·종료 기준). 실제 플러그 연결 시각은 알 수 없음
  - 화면은 전송을 기다리지 않음: 3초 평가(F5)가 목표이고 `UiState`에 오류 필드도 없어서 재시도는 Recorder가 맡음
  - 포그라운드 서비스·동의 게이트·철회·Nav 수정 범위는 이민하 PR(#5·#6·#10)과 같게 맞춤: 두 앱이 같은 구조여야 공통 심사 문의·리뷰가 한 번에 끝남
  - 철회 버튼은 설정 화면이 없어 S6에 둠
- 확인: `assembleDebug test` 통과(52개, 실패 0), `:app:lintDebug` 통과(오류 0, 경고 6). 에뮬레이터 미실행
- 바꾼 파일: `RecordingService.kt`(새 파일), `AppContainer.kt`, `ChachelinApp.kt`, `MainActivity.kt`, `AndroidManifest.xml`, `domain/{ChargeSession,ChargeSessionTracker,ChargeSessionRecorder,VehiclePreset}.kt`, `data/{ChargeRecordRepository,StationRepository,VehicleProfileStore}.kt`, `ui/ChachelinNav.kt`, `ui/history/HistoryScreen.kt`, `ui/{stations,charging,review,history,onboarding}/*ViewModel.kt`(새 파일), 테스트 4개(새 파일), `docs/handoff/lee-sangjun.md`

### 2026-10-30
- 장호연: 파일 생성. 첫 작업은 README "담당별 첫 작업" 기준으로 적어 둠.
