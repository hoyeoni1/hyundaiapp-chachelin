# 인수인계 — 길민종 (두 앱 UI)

담당 경로: `app/src/main/java/com/hyundaiapp/*/ui/`(화면·테마). 상태 계약 `ui/UiStates.kt`는 로직 담당과 합의

## 현재 상태 (2026-11-01 기준, 매번 덮어쓰기)

- 한 일 요약: 공통 테마 + 차슐랭 S1~S7(가로·세로) 화면을 와이어프레임대로 구현. 두 앱 모두 `assembleDebug`·`test` 통과, Robolectric으로 화면을 이미지로 렌더해 눈으로 확인함. **에뮬레이터 실행은 아직 안 해 봄.**
  - 와이어프레임: https://claude.ai/artifact/9vidSJtHrxzCqtEcjJjVTf (비공개 — 팀이 보려면 Share로 공유 필요)
  - 화면: `stations/StationListScreen.kt`(S1), `stations/StationDetailScreen.kt`(S2), `charging/ChargingScreen.kt`(S3), `review/ReviewScreen.kt`(S4), `review/FailureNoticeScreen.kt`(S5), `history/HistoryScreen.kt`(S6), `onboarding/OnboardingScreen.kt`(S7)
  - 데이터: S1만 실제 `StationListViewModel`. 나머지는 `PreviewData`나 `ChachelinNav` 안의 임시 상태로 동작
- 진행 중 브랜치·PR: 화면 PR(https://github.com/hoyeoni1/hyundaiapp-chachelin/pull/1)은 11-01 머지됨. 리뷰 후속 `fix/ui-review-followups` PR 올림(리뷰·머지 대기)
- 다음 할 일(우선순위 순):
  1. 에뮬레이터에서 개발 메뉴 → 각 화면 확인(터치 반응·글자 크기). **이 Mac(Apple Silicon)은 Pleos 이미지(x86_64 전용)를 못 돌린다** → 에뮬레이터가 있는 팀원 PC에서 확인
  2. 이상준 ViewModel이 나오면 `*Nav.kt`의 임시 상태·`PreviewData`를 ViewModel로 교체
  3. 약관 전문 문구 받으면 `TermsDialog`의 "[약관 전문 준비 중]" 교체(장호연)
  4. 영상용 화면 연출(장호연과)
- 막힌 점·질문(누구에게): 이상준에게 — ① S2~S7 ViewModel — 화면 콜백 이름은 각 Screen 함수 파라미터 그대로(S2 `onRouteClick`→`confirmRoute=true`, `onConfirmRoute`에서 NaviHelper `requestRoute`) ② S4 [나중에](`onLater`) 동작 확정 ③ S2 실측 속도 분포는 UiState에 없어 뺐다. 넣으려면 UiState 필드 추가 필요 ④ S6 `stationName == null`을 '충전소 아님(기록만)'으로 표시함 — 맞는지
- 주의(함정·직접 겪은 이상한 동작):
  - 화면은 UiState와 콜백만 받는다(ViewModel·SDK 직접 참조 금지). `UiStates.kt`는 고치지 않았다
  - **`ui/theme/Theme.kt`, `ui/components/CarComponents.kt`, `ui/components/CarIcons.kt`는 두 앱이 같은 내용**(패키지만 다름). 한쪽을 고치면 다른 앱도 같이 고친다
  - 색은 `CarColors`만 쓴다. 파랑(정보·인증)·주황(위험·실패) 둘뿐이고 상태는 늘 글자와 함께
  - `Surface(color = CarColors.SurfaceVariant)`는 기본 글자색이 회색이 된다 → `contentColor = CarColors.OnSurface`를 같이 준다
  - 한글이 글자 단위로 줄바꿈되어 제목이 "미/리"처럼 끊긴다 → 고정 문구는 `\n`으로 줄을 직접 나눴다
  - 아이콘은 material-icons 없이 `CarIcons`(SVG path)로 그린다
  - 크루즈브레이커 S1은 주행 중 화면: 터치 요소·애니메이션·자동 스크롤·여러 줄 금지(배너 `maxLines = 1`)
  - 동의 화면: [동의] 버튼을 눌러야만 동의. 약관 창 닫기·뒤로 가기는 동의 아님
  - 세로/가로는 `BoxWithConstraints`로 폭<높이면 세로 배치. 같은 화면 함수 하나로 처리
- 확인 방법: 앱 실행 → 개발 메뉴 → 각 S 화면. Android Studio에서는 각 Screen 파일 아래 `@Preview`(가로 1280×720, 차슐랭은 세로 720×1280도). CLI: `./gradlew assembleDebug test`

## 작업 기록 (최신이 위, 지우지 않음)

### 2026-11-01
- 한 일: 화면 PR #1 머지 후 리뷰 후속(브랜치 `fix/ui-review-followups`). 실측 평균 kW가 null이면 "-kW" 대신 "측정 중"(`formatKw`, S1·S2·S3·S4). `SelectableButton`에 `role` 파라미터를 두고 S7 차종은 `Role.RadioButton`(크루즈브레이커와 공통 부품 같이 수정). Robolectric 렌더로 확인: S7 가로 시작 버튼 문구 한 줄, S4 가로 별점·태그 6개·버튼이 720dp 안에 스크롤 없이 들어감. `assembleDebug`·`test` 통과.
- 결정한 것과 이유: S3 큰 숫자 칸은 값이 없으면 단위 "kW"도 빼고 "측정 중"만 보여 준다(숫자 없는 단위는 읽기 어색함)
- 바꾼 파일: `ui/components/Formats.kt`, `ui/components/CarComponents.kt`, `ui/stations/`, `ui/charging/ChargingScreen.kt`, `ui/review/ReviewScreen.kt`, `ui/onboarding/OnboardingScreen.kt`, `docs/handoff/gil-minjong.md`


### 2026-10-31
- 한 일(2): 와이어프레임대로 코드 구현(브랜치 `feat/ui-screens`). 공통 테마(`CarColors`·`CarDimens`·글자 크기), 공통 부품(`CarComponents.kt`·`CarIcons.kt`), 차슐랭 S1~S7(가로·세로) 화면, `*Nav.kt` Placeholder 교체. `assembleDebug`·`test` 통과, Robolectric 렌더로 화면 확인(렌더 설정은 커밋 안 함).
- 결정한 것과 이유(2):
  - 두 앱이 같은 부품 파일을 복사해 쓴다: `core/`는 황준용 담당이라 공통 UI 모듈을 만들지 않음
  - ViewModel이 없는 화면은 `*Nav.kt`에서 임시 상태를 들고 콜백을 연결: 개발 메뉴에서 바로 눌러 볼 수 있게
- 바꾼 파일(2): `ui/theme/Theme.kt`, `ui/components/`(새로), `ui/*Nav.kt`, 각 화면 폴더(`ui/<화면>/`), `docs/handoff/gil-minjong.md`
- 한 일: 두 앱 공통 테마 + 전체 화면 와이어프레임 16장(크루즈브레이커 6장, 차슐랭 9장, 테마 1장)을 Claude Design 캔버스로 만듦(https://claude.ai/artifact/9vidSJtHrxzCqtEcjJjVTf). 코드 변경 없음.
- 결정한 것과 이유:
  - 1280×720 가로 기준, 어두운 배경 + 큰 글씨(차량 화면 가이드 2.3: 큰 터치 영역·높은 대비·텍스트 최소화)
  - 강조색은 파랑·주황 둘만: 색약에서도 밝기로 구분되고, 두 앱이 같은 의미(파랑=정보·인증, 주황=위험·실패)로 쓴다
  - 크루즈브레이커 S1 배너는 주황 바탕 한 줄 64sp, 아이콘 1개. 평소에는 "기록 중" 작은 표시만
  - 차슐랭 S4는 별 96px·태그 72px 버튼, 키보드 없음. 세로 화면은 좌우 2단 → 위아래 1단 규칙
- 바꾼 파일: `docs/handoff/gil-minjong.md`

### 2026-10-30
- 장호연: 파일 생성. 첫 작업은 README "담당별 첫 작업" 기준으로 적어 둠.
