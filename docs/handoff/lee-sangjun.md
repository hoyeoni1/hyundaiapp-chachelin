# 인수인계 — 이상준 (차슐랭 로직)

담당 경로: 차슐랭 로직 — `app/src/main/java/com/hyundaiapp/chachelin/` 아래 `domain/`, `data/`(인터페이스 합의), ViewModel, `AppContainer`·`ChachelinApp`·`RecordingService`. 화면(`ui/` Screen·테마)은 길민종

## 현재 상태 (2026-11-09 기준, 매번 덮어쓰기)

- 한 일 요약:
  - PR #3(판정·업로드·화면 연결·기록 서비스·철회), #20(충전소 조회 실패 재조회·결과 알림)은 main에 머지됨
  - **11-09 `fix/demo-feedback-20261109`**: 시연 피드백 7개 중 1~4 반영, 6은 이미 해결된 것 확인, 5·7은 결정 필요
    - 1 화면 전환: 들어가면 오른쪽에서, 돌아오면 왼쪽에서 짧게 밀어 넣기(220ms)
    - 2 이동 버튼: 왼쪽 고정 이동 막대(홈·충전소·내 기록·설정). 충전 흐름(S3·S4·S5)과 첫 동의에서는 숨김
    - 3 홈: 동의 후 시작 화면. 충전 상태(기록 중·평가 대기·실패 → 바로 가기), 근처 충전소·내 충전 기록·설정 큰 버튼, 최근 기록 한 줄
    - 4 동의 화면 재진입: 동의를 마쳤으면 같은 화면이 "설정"으로 열려 저장된 동의·차종을 보여 줌(다시 동의 안 받음). 차종 변경·저장, [동의 철회]도 여기서
    - 6 상반 태그: main(PR #19)에서 이미 쌍마다 하나만 고를 수 있음 — 에뮬레이터에서 [빠름] 뒤 [느림]을 누르면 [빠름]이 풀림을 확인. 시연은 이전 APK였을 가능성
  - 단위 테스트 debug·release 각 105개 통과, debug·release 빌드, `:app:lintDebug` 통과
- 확인한 것(에뮬레이터, 2026-11-09, Fake 저장소): 새로 설치 → 첫 실행 동의(막대 없음) → 시작 → 홈(막대) → [충전소] → S1 → [설정] → 동의함·코나 선택 상태 → 아이오닉 6으로 저장 → 홈에 "내 차 아이오닉 6" / `demo-charge.sh success` → S3 → S4(막대 없음) → 태그 상반 쌍 배타 → [평가 건너뛰기] → 홈(기록 1건, 최근 기록 줄)
- 진행 중 브랜치·PR: `fix/demo-feedback-20261109` (PR 리뷰·머지 대기)
- 다음 할 일(우선순위 순):
  1. **결정 필요(장호연)**: 피드백 5(리뷰 텍스트 코멘트) — PRD S4는 "키보드 없음·3초 평가"이고 서버 `reviews`에 글 칸이 없음. 넣으려면 서버 열 추가(황준용)·약관(자유 입력 개인정보)·S4 화면이 함께 바뀜. 추천: 이번 제출에서는 빼고 확장 계획(음성 후기 STT)에 적기
  2. **결정 필요(장호연)**: 피드백 7(충전 속도는 유저 태그 대신 실제 기록으로) — 이미 S1·S2·S4에 계산 평균 kW가 나옴. 추천: [빠름]/[느림] 태그를 빼고 S2에 "이 충전소 계산 평균 vs 충전기 최대 kW"를 보여 주기. 태그 목록은 서버 enum·시드·S2 상위 태그와 묶여 있어 황준용·길민종과 같이 바꿔야 함
  3. 길민종 PR #37(충전 시작 알림)이 `ChachelinNav.kt`·`MainActivity`·`AppContainer`를 고쳐서 이 브랜치와 충돌할 수 있음 → 먼저 머지되는 쪽에 맞춰 다른 쪽을 고침. #37은 내 담당 경로(`AppContainer`·`ResultNotification` 대체 등)를 고치므로 내 확인이 필요(장호연 메모)
  4. 서버 키가 있는 PC에서 오프라인 충전 리허설, 이슈 #12·#13, NAV02(이번 홈·막대로 S3·S4 재진입은 홈 상태 칸·아래 버튼으로 가능해짐)
- 막힌 점·질문(누구에게):
  - 길민종: **담당 경로 밖을 고쳤음(시연 피드백, 사용자 요청)** — `ui/ChachelinNav.kt`(홈·설정 화면, 이동 막대, 전환), `ui/onboarding/OnboardingScreen.kt`(설정 모드 제목·저장·철회 버튼), 새 파일 `ui/home/HomeScreen.kt`·`AppNavRail.kt`. 공통 부품(`CarComponents`·`CarIcons`·테마)은 건드리지 않음(새 아이콘은 `ui/home/AppNavRail.kt`의 `NavIcons`). 모양은 편하게 바꿔도 됨. 크루즈브레이커에도 같은 막대가 필요한지 판단
  - 장호연: 위 5·7 결정, release APK 재빌드(화면 흐름이 바뀌어 `docs/demo-scenario.md` 1단계 "S7→S1"이 "S7→홈"으로 바뀜)
- 주의(함정·직접 겪은 이상한 동작):
  - **에뮬레이터 32.x에서는 터치가 안 먹는다.** `ai.umos.drivingview`가 충돌을 반복하며 입력을 막음 → emulator 37.2.13. 이 앱을 끄면 홈 화면도 죽으니 끄지 말 것
  - **Windows에서 프로젝트 경로에 한글이 있으면 AGP가 빌드를 거부한다.** 영문 경로로 통째로 복사해 빌드하고 `diff`로 같은지, 로그에 `BUILD SUCCESSFUL`이 있는지 확인(11-09에도 복사본이 지워져 빌드가 조용히 안 돈 적 있음). Gradle 데몬이 lint jar를 잡으면 `gradlew --stop`
  - 알림 채널 중요도는 처음 만들 때 정해짐. 바꾸면 앱을 지우고 다시 설치
  - 기록은 S7 동의 후에만 돈다. `am start` 직후·[길안내] 후에는 내비가 앞을 덮음 → 하단 독의 앱 아이콘
  - 앱이 다시 살아날 때 충전구가 이미 열려 있으면 그때를 세션 시작으로 봄. 위치는 판교 고정(시드 st-003 좌표)
- 확인 방법:
  - `./gradlew assembleDebug assembleRelease test :app:lintDebug`
  - 화면 흐름: 새로 설치 → S7 → 홈 → 막대로 각 화면 → [설정]에서 동의 다시 안 묻는지 → `bash tools/demo-charge.sh success`로 S3·S4
  - 결과 알림: `adb shell pm grant --user 10 com.hyundaiapp.chachelin android.permission.POST_NOTIFICATIONS` → 내비를 앞으로 → `demo-charge.sh success` → `adb shell dumpsys notification --noredact | grep -A30 'chachelin|2|'`
- 시연 순서: `docs/demo-scenario.md`(장호연). 처음이면 S7 동의부터. PT에서 밝힐 것: SoC는 콘솔 주입, 충전 시간·kW는 시연 배속(×120), 다른 차량 기록은 시드 데이터

## 작업 기록 (최신이 위, 지우지 않음)

### 2026-11-09 (이상준, Claude 에이전트와 작업 — 시연 피드백)
- 한 일: 최신 main(#35·#36 포함)에서 `fix/demo-feedback-20261109`. 피드백 1(화면 전환 애니메이션), 2(왼쪽 이동 막대), 3(홈 화면), 4(동의 화면 재진입 시 설정으로) 구현. 6(상반 태그)은 main에서 이미 해결됨을 에뮬레이터로 확인. 5(텍스트 코멘트)·7(속도는 실제 기록으로)은 PRD·서버와 묶인 결정이라 장호연에게 넘김. 테스트 99 → 105개(`DemoFeedbackTest` 6개)
- 결정한 것과 이유:
  - 막대 칸은 홈 위에 한 단계로만 쌓음: 어느 칸에서든 뒤로 가면 홈, 홈에서 뒤로 가면 앱 종료 — 스택이 깊어져 "어디로 돌아가는지" 모르는 일을 막기 위해
  - 충전 흐름(S3·S4·S5)과 첫 동의에서는 막대를 숨김: 평가·동의 중에 다른 화면으로 새지 않게(3초 평가)
  - 설정은 S7 화면을 그대로 씀: 동의 문구·약관 보기를 두 벌로 관리하지 않기 위해. 동의 버튼은 이미 "동의함"으로 보여 다시 누를 필요 없음
  - 차종 순서를 `VehicleProfileStore.presetIndex`에 새로 저장. 예전 설치는 용량이 같은 첫 차종으로 보여 줌(아이오닉 5·6·EV6가 모두 77.4kWh라 셋 중 첫 번째로 보일 수 있음 — 다시 고르면 저장됨)
  - 전환은 220ms 짧은 밀어 넣기만: 정차 화면 전용이지만 차량 화면 가이드(애니메이션 최소)에 맞춤
- 바꾼 파일: `ui/ChachelinNav.kt`, `ui/onboarding/{OnboardingScreen,OnboardingViewModel}.kt`, `ui/home/{HomeScreen,HomeViewModel,AppNavRail}.kt`(새 파일), `ui/UiStates.kt`(`OnboardingUiState.editing`, `HomeUiState`), `data/VehicleProfileStore.kt`(`presetIndex`), 테스트 `DemoFeedbackTest`(새 파일), `docs/handoff/lee-sangjun.md`

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
