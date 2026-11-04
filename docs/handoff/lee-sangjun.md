# 인수인계 — 이상준 (차슐랭 로직)

담당 경로: 차슐랭 로직 — `app/src/main/java/com/hyundaiapp/chachelin/` 아래 `domain/`, `data/`(인터페이스 합의), ViewModel, `AppContainer`·`ChachelinApp`·`RecordingService`. 화면(`ui/` Screen·테마)은 길민종

## 현재 상태 (2026-11-04 기준, 매번 덮어쓰기)

- 한 일 요약:
  - PR #3(판정·업로드·ViewModel·기록 서비스·철회)은 11-02 main에 머지됨. 서버(황준용 PR #10, Supabase)도 머지돼 `local.properties`에 공개 URL·키가 있으면 실제 저장소, 없으면 Fake로 동작
  - **11-04 `feat/station-retry-and-result-notice`**: 서버 연결 후 남은 PRD 5.3 결정 두 가지
    - 충전소 조회 실패: "충전소 아님"으로 단정하지 않고, 충전구를 연 위치로 될 때까지 다시 조회(5초·15초·1분·5분, 5분으로 계속)한 뒤 판정·업로드. 충전소와 상관없이 기록하지 않는 세션은 기다리지 않음. 실제 저장소 `nearest`는 오프라인일 때 캐시에서 찾고, 캐시에 없으면 null 대신 오류를 던짐
    - 결과 알림: 앱이 화면에 없을 때 S4/S5 대상 결과가 생기면 알림("충전 완료 · 3초 평가" / "충전 실패를 기록했어요"), 누르면 앱이 열려 S4/S5. 세션마다 한 번, 처리하거나 앱을 열면 지움. 알림 권한이 없으면 띄우지 않음(기록은 그대로)
  - 단위 테스트 debug·release 각 91개 통과, debug·release 빌드, `:app:lintDebug` 오류 0(경고 16)
- 확인한 것(에뮬레이터, 2026-11-04, Fake 저장소): 내비가 앞에 있을 때 인증 충전 → 알림 센터에 "충전 완료 · 3초 평가 / 시흥동 주민센터 · 40% → 80%" → 누르면 S4, 알림 지워짐 / 실패 → "충전 실패를 기록했어요" → 누르면 앱이 열림
  - **확인 못 한 것**: 실제 Supabase 연결 상태(이 PC에 키 없음, 조회 실패·재조회는 단위 테스트로만 확인), 화면 위 팝업(헤드업) — 중요도 HIGH로 했지만 Pleos 에뮬레이터 캡처에서는 팝업이 보이지 않고 알림 센터에만 쌓임
- 진행 중 브랜치·PR: `feat/station-retry-and-result-notice` (PR 리뷰·머지 대기)
- 다음 할 일(우선순위 순):
  1. 서버 키가 있는 PC에서 연결 리허설: Wi-Fi를 끈 채 충전 → 결과가 미뤄지는지, 다시 켜면 인증 충전으로 올라가는지
  2. 이슈 #12(S1 위치 대기 기한·최신 위치 우선), #13(길안내 요청 대기·실패·성공 계약, 황준용과) — 길민종 UI 감사에서 넘어온 것
  3. NAV02(길민종 제안): S3·S4에서 Back으로 나가면 앱 안에서 다시 들어갈 길이 없음 → 진행 중·미처리 세션 진입점(길민종과 합의)
  4. 조회 재시도 중인 세션은 메모리에만 있어 앱 프로세스가 죽으면 사라짐. 필요하면 영속 저장(황준용 outbox와 같은 방식)
- 막힌 점·질문(누구에게):
  - 황준용: `SupabaseStationRepository.nearest`를 고쳤음(오프라인 + 캐시에 없음 → null 대신 예외). `StationRepository.nearest` 계약에 "null은 없음이 확인된 경우만"을 적음. 리뷰 부탁
  - 장호연: PRD 5.3 "결과 알림" 구현됨, 헤드업 팝업은 실차·실제 Pleos에서 확인 필요. #17(태그 쌍 배타)은 장호연 PR #19에 들어 있어 이 브랜치에서는 손대지 않음
- 주의(함정·직접 겪은 이상한 동작):
  - **에뮬레이터 32.x에서는 터치가 안 먹는다.** 시스템 앱 `ai.umos.drivingview`(왼쪽 3D 차량)가 충돌·재시작을 반복하고, 그때 생기는 전체 화면 입력 차단 창이 터치를 삼킨다(logcat `Not sending touch gesture to ... ActivityRecordInputSink`). 왼쪽 패널이 비어 있으면 이 증상. **emulator 37.2.13으로 올리면 해결**. 이 앱을 끄면(`pm disable-user`) 홈 화면이 같이 죽으니 끄지 말 것
  - **Windows에서 프로젝트 경로에 한글이 있으면 AGP가 빌드를 거부한다.** 영문 경로로 복사해서 빌드하고, 복사 후 `diff`로 같은지 확인할 것(복사가 조용히 실패해 옛 코드를 검증한 적 있음). Gradle 데몬이 lint 캐시 jar를 잡고 있으면 `lintVitalAnalyzeRelease`가 "다른 프로세스가 사용 중"으로 실패 → `gradlew --stop` 후 다시
  - 알림 채널 중요도는 처음 만들 때 정해진다. 채널 설정을 바꾸면 앱을 지우고 다시 설치해야 반영됨
  - **기록은 S7 동의 후에만 돈다.** 새로 설치했으면 개발 메뉴 S7부터. S7 화면 배치가 바뀌었음(알림 허용 줄 추가)
  - `am start` 직후나 [길안내] 후에는 내비가 앞을 덮는다 → 하단 독의 앱 아이콘을 누른다
  - 앱이 다시 살아날 때 충전구가 이미 열려 있으면 그때를 세션 시작으로 본다 → 충전 시간·ΔSoC가 실제보다 작게 잡힘
  - 에뮬레이터 SoC는 충전 중에도 자동으로 안 오름 → 콘솔로 주입. 위치는 판교 부근 고정(시드 st-003과 같은 좌표)
  - `getEvBatteryCapacity`는 `CAR_ENERGY`만으로 "need Permission" → 차종 프리셋(S7) 값을 씀
- 확인 방법:
  - `./gradlew assembleDebug assembleRelease test :app:lintDebug`
  - 결과 알림: `adb shell pm grant --user 10 com.hyundaiapp.chachelin android.permission.POST_NOTIFICATIONS` → 독에서 내비를 앞으로 → `bash tools/demo-charge.sh success` → `adb shell dumpsys notification --noredact | grep -A30 'chachelin|2|'`, 알림 센터는 `adb shell cmd statusbar expand-notifications`
  - 서비스: `adb shell "dumpsys activity services" | grep RecordingService`
- 시연 순서: `tools/demo-charge.sh`(main에 있음) 주석과 PRD 7절. 처음이면 개발 메뉴 S7 동의부터. PT에서 밝힐 것: SoC는 콘솔 주입, 충전 시간·kW는 시연 배속(×120), 다른 차량 기록은 시드 데이터

## 작업 기록 (최신이 위, 지우지 않음)

### 2026-11-04 (이상준, Claude 에이전트와 작업 — 서버 연결 후 남은 일)
- 한 일: 최신 main(서버 PR #10 포함)에서 `feat/station-retry-and-result-notice`. PRD 5.3의 11-02 결정 중 남은 두 가지 구현 — 충전소 조회 실패 시 재조회(`ChargeSessionTracker`, `SupabaseStationRepository.nearest`), 결과 알림(`domain/ChargeResultNotices.kt`, `ResultNotification.kt`, `MainActivity` 화면 표시 여부). 테스트 81 → 91개(조회 실패 4개, 알림 6개). 에뮬레이터에서 알림 표시·눌러서 S4 열림 확인
- 결정한 것과 이유:
  - 다시 조회할 때는 충전구를 연 위치를 쓴다: 닫은 뒤 차가 움직여도 충전한 곳으로 판정하기 위해
  - 충전소가 있든 없든 판정이 "기록 안 함"인 세션은 조회를 기다리지 않는다: 잠깐 연 경우까지 재조회하면 의미 없는 대기·요청만 늘어남
  - 결과 알림은 세션마다 한 번만: 앱을 열어 결과 화면을 봤는데 다시 뒤로 갈 때마다 울리면 방해가 됨
  - 알림 중요도 HIGH: 정차 중 바로 보여야 평가가 모임(PRD 3.2 "평가하는 시점")
- 바꾼 파일: `domain/ChargeSessionTracker.kt`, `domain/ChargeResultNotices.kt`(새 파일), `ResultNotification.kt`(새 파일), `AppContainer.kt`, `MainActivity.kt`, `data/StationRepository.kt`(계약 주석), `data/SupabaseRepositories.kt`(`nearest`), 테스트 `ChargeSessionTrackerTest`·`ChargeResultNoticesTest`(새 파일), `docs/handoff/lee-sangjun.md`

### 2026-11-02 오후 (이상준, Claude 에이전트와 작업 — 에뮬레이터 시연)
- 한 일: 이 PC에 Pleos 이미지(42dot SDK 라이선스 동의, sha1 확인)·AVD `Pleos_Connect_2_1_0` 설치, emulator 32.1.15 → 37.2.13(기존은 SDK의 `emulator-32.1.15-backup`에 둠). 시연 흐름 전체 확인(위 "확인한 것"). `ChargeRules.demoTimeScale`, Fake 저장소 연결, S1·S2 들어올 때 다시 조회, `HistoryScreen` 미리보기 컴파일 오류 수정. 테스트 63개
- **바로잡음**: 앞선 커밋 `7bb7ae1`은 컴파일되지 않았다. 당시 "52개 통과"는 복사 실패로 옛 코드를 검증한 결과였다(위 "주의"). 이 커밋에서 고쳤다
- 결정한 것과 이유:
  - 시연 배속은 화면 시간·kW에만 곱하고 판정 기준·기록 시각에는 쓰지 않음: 판정 로직은 실제와 같게 두고 숫자만 읽을 수 있게 함. 디버그 빌드 전용, PT에서 밝힘
  - 확인 창은 `AlertDialog` 그대로 둠: 처음에 Dialog 문제로 보고 창 안에 그리는 방식으로 바꿨으나 원인이 에뮬레이터 버전으로 밝혀져 되돌림
- 바꾼 파일: `domain/{ChargeSession,ChargeSessionTracker}.kt`, `data/{StationRepository,ChargeRecordRepository}.kt`, `AppContainer.kt`, `ui/ChachelinNav.kt`, `ui/history/HistoryScreen.kt`, `ui/charging/ChargingViewModel.kt`, `ui/review/ReviewViewModel.kt`, 테스트 3개, `docs/handoff/lee-sangjun.md`

### 2026-11-02 (이상준, Claude 에이전트와 작업)
- 한 일: `feat/charge-session-logic` — `ChargeSessionTracker` 구현, `ChargeRules.judge`, `ChargeSessionRecorder`(업로드·재시도), `VehiclePreset`, `VehicleProfileStore`, S2~S7 ViewModel, `ChachelinNav` 연결, `RecordingService`·`BootReceiver`, 동의 철회, 가명 차량 ID 업로드, Fake 저장소에 상세 시드
- 결정한 것과 이유:
  - PRD 5.3에 없는 경우는 기록하지 않음: 충전소에서 충전구만 잠깐 연 경우, ΔSoC 1%p, ΔSoC ≥ 2%p인데 시간 미달. 충전 성공도 실패도 아니어서 점수를 흐리지 않게 함
  - 충전소 밖에서 SoC가 오르지 않은 경우도 기록하지 않음: 충전이 아니므로 "집 충전" 기록이 되면 안 됨
  - 충전 시간은 충전구 열림~닫힘(PRD 5.3 세션 시작·종료 기준). 실제 플러그 연결 시각은 알 수 없음
  - 화면은 전송을 기다리지 않음: 3초 평가(F5)가 목표이고 `UiState`에 오류 필드도 없어서 재시도는 Recorder가 맡음
  - 포그라운드 서비스·동의 게이트·철회·Nav 수정 범위는 이민하 PR(#5·#6·#10)과 같게 맞춤: 두 앱이 같은 구조여야 공통 심사 문의·리뷰가 한 번에 끝남
  - 철회 버튼은 설정 화면이 없어 S6에 둠
- 바꾼 파일: `RecordingService.kt`(새 파일), `AppContainer.kt`, `ChachelinApp.kt`, `MainActivity.kt`, `AndroidManifest.xml`, `domain/{ChargeSession,ChargeSessionTracker,ChargeSessionRecorder,VehiclePreset}.kt`, `data/{ChargeRecordRepository,StationRepository,VehicleProfileStore}.kt`, `ui/ChachelinNav.kt`, `ui/history/HistoryScreen.kt`, `ui/{stations,charging,review,history,onboarding}/*ViewModel.kt`(새 파일), 테스트 4개(새 파일), `docs/handoff/lee-sangjun.md`

### 2026-10-30
- 장호연: 파일 생성. 첫 작업은 README "담당별 첫 작업" 기준으로 적어 둠.
