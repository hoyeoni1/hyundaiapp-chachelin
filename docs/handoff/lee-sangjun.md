# 인수인계 — 이상준 (차슐랭 로직)

담당 경로: 차슐랭 로직 — `app/src/main/java/com/hyundaiapp/chachelin/` 아래 `domain/`, `data/`(인터페이스 합의), ViewModel, `AppContainer`·`ChachelinApp`·`RecordingService`. 화면(`ui/` Screen·테마)은 길민종

## 현재 상태 (2026-11-02 기준, 매번 덮어쓰기)

- 한 일 요약:
  - **Pleos 에뮬레이터에서 PRD 7절 시연 흐름 동작 확인**(아래 "확인한 것"). 단위 테스트 63개 통과, `:app:lintDebug` 오류 0
  - 판정(F1~F4): `ChargeSessionTracker` + `ChargeRules.judge`. 충전소 매칭(150m)은 충전구를 열 때 따로 조회, 닫을 때 최대 10초 기다림
  - 업로드·재시도: `ChargeSessionRecorder`(5초·15초·1분·5분 간격, 5분으로 계속). 리뷰는 세션 업로드 후 전송, [실패 아님]은 업로드를 멈추고 서버에서 삭제
  - 화면 연결: S2~S7 ViewModel + `ChachelinNav` 연결. 충전구를 열면 S3, 닫으면 S4/S5로 자동 전환. 길안내(F6)는 S2 확인 후 `requestRoute`
  - 앱 수명: `RecordingService`(specialUse) + `BootReceiver`. 크루즈브레이커(이민하)와 같은 구조. **동의 전에는 차량·위치 신호를 받지 않음**
  - 동의 철회: S6 헤더 [동의 철회] → 확인 창 → 서비스 정지, 내 기록·동의·차종·가명 차량 ID 삭제
  - 시연용: 디버그 빌드만 시간 배속 ×120(`ChargeRules.DEMO.demoTimeScale`, 15초 = 30분). Fake 저장소끼리 묶어서 내 충전이 S1·S2 숫자에 바로 반영됨
  - 아직 없는 것: 서버 저장소(Fake 사용 중, 기록은 메모리에만 있어 앱을 강제 종료하면 사라짐), S2 실측 속도 분포, 시드 충전소는 4곳(PRD는 10곳)
- 확인한 것(에뮬레이터, 2026-11-02): 동의 전 서비스·신호 없음 → S7 동의 → 포그라운드 서비스 시작 / S1 목록 / S2 → [길안내] → 내비 경로 잡힘 / `demo-charge.sh success` → S3(SoC·시간·kW 갱신) → S4(32분·59kW) → 별점·태그·완료 / `demo-charge.sh fail` → S5 → [확인] / [실패 아님] → 기록 삭제 / S1·S2 숫자 변화(시흥동 3.2→3.4, 인증 21→22, 실패 4→5) / S6 인증·실패 표시 / **내비가 앞에 있는 동안 충전 기록됨(SoC 20→70%)** / 철회 → 서비스·신호 끊김, prefs 빔 / 재동의 / **재부팅 후 앱을 안 열어도 서비스 시작, 충전 기록됨(30→60%)**
- 진행 중 브랜치·PR: `feat/charge-session-logic` (푸시함, PR 생성·리뷰·머지 대기)
- 다음 할 일(우선순위 순):
  1. 11-05 중간 점검 시연 리허설(아래 "시연 순서")
  2. 앱이 뒤에 있을 때 충전이 끝나면 S4/S5가 앞으로 뜨지 않는다(기록은 됨, 앱으로 돌아오면 떠 있음). 결과 알림(눌러서 S4/S5로)이 필요한지 결정
  3. 서버 구현이 나오면 Fake 교체 후 시연 5·6단계 다시 확인
- 막힌 점·질문(누구에게):
  - 장호연: ① **시연 배속을 추가함** — 콘솔 주입으로는 kW가 약 7,000~8,000으로 나와서 디버그 빌드에만 ×120을 곱함. 충전 시간·kW는 배속 값이므로 **PT에서 SoC 주입과 함께 밝혀야 함**(PRD 7절 "PT에서 밝힐 것"에 추가 요청). 빼려면 `ChargeRules.DEMO`의 값만 지우면 됨 ② `feat/demo-scripts`의 `demo-charge.sh`가 이 코드에서 동작함을 확인 → PR로 main에 넣어 주기. 주석에 "먼저 개발 메뉴 S7에서 동의"를 추가 요청 ③ PRD 5.3에 없는 경우를 아래 "결정"처럼 정함 ④ 포그라운드 서비스 퍼미션 4개 추가(주최 측 문의 5번과 같은 조건) ⑤ `docs/dev-env-setup-log.md`에 아래 에뮬레이터 함정 추가 요청
  - 황준용: `data/` 인터페이스 변경 — `ChargeRecordRepository.upload(record, vehicleId)`(같은 세션 id는 한 건만), `retract(sessionId)`(없어도 오류 아님), `deleteMine(vehicleId)`, `StationRepository.detail(stationId)`. 재시도 대기열은 메모리뿐이라 앱이 죽으면 못 보낸 기록이 사라짐 → 저장소 구현에서 쌓아 두고 재전송 필요. PRD 시드 10곳은 서버 시드에서. `:core:lintDebug`가 `MissingPermission` 등 오류 28개로 실패함(`NaviSource.kt` 등, 이번 변경과 무관)
  - 길민종: **담당 경로 밖을 고쳤음(이민하 PR과 같은 방식)** — `ChachelinNav.kt`를 ViewModel로 교체(임시 상태 삭제, 할 일 2번 끝남), S6 `HistoryScreen`에 `onWithdrawConsent`와 [동의 철회] 버튼 추가(모양은 바꿔도 됨). 질문 답: ① S2~S7 ViewModel 완료 ② S4 [나중에] = 세션 기록만 남기고 리뷰 없이 닫기 ③ 속도 분포는 서버 집계가 정해지면 UiState에 함께 추가 ④ S6 `stationName == null` = 충전소 아님, 맞음. 합의 필요: S2 불러오기 실패 시 "불러오는 중"이 계속 보임(`StationDetailUiState`에 error 필드 제안), S7에서 알림 권한 요청. 화면 확인 메모: S3의 "경과 시간"이 "결과 시간"처럼 보일 만큼 글자가 작음
- 주의(함정·직접 겪은 이상한 동작):
  - **에뮬레이터 32.x에서는 터치가 안 먹는다.** 시스템 앱 `ai.umos.drivingview`(왼쪽 3D 차량)가 충돌·재시작을 반복하고, 그때 생기는 전체 화면 입력 차단 창이 터치를 삼킨다(logcat `Not sending touch gesture to ... ActivityRecordInputSink`). 왼쪽 패널이 비어 있으면 이 증상. **emulator 37.2.13으로 올리면 해결**. 이 앱을 끄면(`pm disable-user`) 홈 화면이 같이 죽으니 끄지 말 것. `-gpu swiftshader_indirect`도 에뮬레이터가 죽음
  - **Windows에서 프로젝트 경로에 한글이 있으면 AGP가 빌드를 거부한다**(`-Pandroid.overridePathCheck=true`로도 안 됨). 영문 경로로 복사해서 빌드. PowerShell 스크립트에 한글 경로를 쓰면 인코딩이 깨져 복사가 조용히 실패한다 → 복사 후 `diff`로 확인할 것(이것 때문에 옛 코드를 검증하고 컴파일 안 되는 커밋을 푸시한 적 있음)
  - **기록은 S7 동의 후에만 돈다.** 디버그 빌드도 마찬가지 → 새로 설치했으면 개발 메뉴 S7부터. 그 전에는 신호가 안 들어와 S1이 로딩에 머문다
  - `am start` 직후나 [길안내] 후에는 내비가 앞을 덮는다 → 하단 독의 앱 아이콘을 누른다
  - 앱이 다시 살아날 때 충전구가 이미 열려 있으면 그때를 세션 시작으로 본다 → 충전 시간·ΔSoC가 실제보다 작게 잡힘
  - 충전소 조회 실패·위치 없음은 "충전소 아님"으로 기록된다 → 서버 연결 후 네트워크가 끊기면 인증 충전이 빠질 수 있음
  - 에뮬레이터 SoC는 충전 중에도 자동으로 안 오름 → 콘솔로 주입. 위치는 판교 부근 고정(시드 st-003과 같은 좌표라 충전은 시흥동 주민센터에서 일어남)
  - `getEvBatteryCapacity`는 `CAR_ENERGY`만으로 "need Permission" → 차종 프리셋(S7) 값을 씀. SDK 값이 오면 그쪽이 먼저
- 확인 방법:
  - `./gradlew assembleDebug test`, `./gradlew :app:lintDebug`
  - 서비스: `adb shell "dumpsys activity services" | grep RecordingService`. 저장값: `MSYS_NO_PATHCONV=1 adb shell run-as com.hyundaiapp.chachelin --user 10 cat shared_prefs/vehicle_profile.xml`
- 시연 순서(에뮬레이터 `Pleos_Connect_2_1_0`, emulator 37.2.13 이상):
  1. `emulator -avd Pleos_Connect_2_1_0 -no-snapshot-load -no-audio` → 부팅 후 약 1분 기다림(왼쪽 3D 차량이 보이면 준비 끝)
  2. 앱 설치·실행, 독 아이콘으로 앞으로. 처음이면 개발 메뉴 S7에서 동의 2개 + 차종 + 시작하기
  3. S1 → 충전소 선택 → S2 → [길안내] → [길안내 시작] → 내비 경로 → 독 아이콘으로 복귀
  4. `git checkout origin/feat/demo-scripts -- tools/demo-charge.sh`(PR 머지 전까지, 커밋하지 말 것) 후 `bash tools/demo-charge.sh success`(`adb`가 PATH에 없으면 `ADB=경로`) → S3 → S4 → 별 5개 + 태그 2개 + [완료]
  5. `bash tools/demo-charge.sh fail` → 35초 뒤 S5 → [확인]
  6. S1(숫자 변화) → 시흥동 주민센터 S2(실패 시각) → 내 충전 기록(S6)
  7. PT에서 밝힐 것: SoC는 콘솔 주입, 충전 시간·kW는 시연 배속(×120), 다른 차량 기록은 시드 데이터. PRD의 두 번째 에뮬레이터 장면은 서버가 있어야 가능(지금은 같은 앱에서 다시 조회)

## 작업 기록 (최신이 위, 지우지 않음)

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
