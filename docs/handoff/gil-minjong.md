# 인수인계 — 길민종 (두 앱 UI)

담당 경로: `app/src/main/java/com/hyundaiapp/*/ui/`(화면·테마). 상태 계약 `ui/UiStates.kt`는 로직 담당과 합의

## 현재 상태 (2026-11-03 기준, 매번 덮어쓰기)

- 한 일 요약: 열린 담당 이슈 없음. PR #2·#3 리뷰와 이상준 handoff의 UI 요청 확인 후 S7 선택 알림 권한 요청 추가. Android 13+에서 설명 카드의 [알림 허용] 버튼으로만 요청하며 거부해도 필수 동의·차종 선택 후 시작 가능. 가로·세로 내용은 스크롤 가능.
  - PR #3의 `ChachelinNav` ViewModel 연결·S6 철회 버튼 확인. S2~S7 ViewModel 연결은 이미 완료되어 임시 상태 교체 작업은 끝남. S4 [나중에]는 세션 기록을 남기고 리뷰 없이 닫는 기존 동작 사용.
  - S2 추가 후속: 이상준 handoff의 제안대로 `StationDetailUiState.loading/error`와 재시도 UI·콜백을 함께 추가. 충전소 없음·조회 예외·10초 응답 지연을 오류로 끝내며 재시도 가능. 재방문 갱신 중에는 기존 상세·길안내 확인 상태를 유지하고, 실패하면 이전 정보 안내와 재시도를 표시. 재조회 때 이전 요청은 취소하고 취소 예외를 삼키지 않음. 조회 시작은 Nav 진입 효과 한 곳으로 모아 init과의 중복 조회 제거.
  - 동의 철회 확인 창: CarColors 및 64dp 확인/취소 버튼 적용. PR #2 선택 제안대로 S1·S2의 평균 kW 집계가 없으면 "기록 없음", 충전 중/완료 화면의 기존 표시는 유지.
  - 줄바꿈 정리: 공통 제목·본문에 한국어 로캘과 문단 줄바꿈 적용. 좁은 동의 카드에서는 버튼을 아래로 옮겨 본문 폭 확보. 시작 버튼은 한 줄 "시작하기"로 통일하고 비활성 사유는 별도 안내. 실패 안내는 충전소명·실패 원인·공유 안내를 분리, S7 소개 문구를 의미 단위 두 줄로 정리.
  - 선택 리뷰 후속: 알림 권한 요청 이력을 별도 UI 설정에 보관하고 rationale을 확인. 재요청 불가 시 [알림 설정]으로 시스템 설정 진입, 돌아오면 허용 상태 갱신. 앱 알림 설정이 없는 경우 앱 상세 설정으로 대체. 자동으로 설정 화면을 열지 않음.
- 진행 중 브랜치·PR: `fix/onboarding-notifications` — https://github.com/hoyeoni1/hyundaiapp-chachelin/pull/6 (리뷰·머지 대기). 이전 UI PR #1·#2는 머지 완료.
- 다음 할 일:
  1. x86_64 Pleos PC에서 S7 알림 허용/거부, 가로·세로 터치와 S6 철회·재동의 확인.
  2. 장호연이 약관 전문 확정 후 `TermsDialog`의 준비 중 문구 교체.
  3. 결과 알림·실측 속도 분포는 이상준/황준용과 별도 진행. 이번 알림 변경은 기록 중 알림 권한 요청만 연결.
- 막힌 점·질문: 서버 속도 분포 계약은 미정. Mac Apple Silicon에서 Pleos x86_64 에뮬레이터 실행 불가.
- 주의: `StationDetailViewModel` 수정은 이상준이 handoff에서 제안한 오류 상태 계약·재시도 연결에 한정(서버/data/domain 변경 없음). `ui/theme/Theme.kt`·`ui/components/CarComponents.kt`는 두 앱에 동일하게 수정했으며 `CarIcons.kt`는 변경하지 않음. 새 알림 권한 helper는 두 앱에서 패키지만 다름. `core/`·로직 경로 수정 없음. 머지는 장호연 담당.
- 확인 방법: JDK 17로 `bash ./gradlew assembleDebug test :app:lintDebug`. `gradlew` 실행 비트가 없어 bash 사용. 렌더/화면 확인은 작업 기록 참조.

## 작업 기록 (최신이 위, 지우지 않음)

### 2026-11-03 — 선택 리뷰 반영
- 최종 확인: `assembleDebug test :app:lintDebug` 통과. app debug/release 각각 73개, core debug 17/release 3개, 실패 0. 임시 UI 검사 6개 통과.
- 한 일: 리뷰의 선택 제안도 반영. 알림 영구 거부/재요청 불가 시 명시적 버튼으로 설정 진입, 요청 이력과 rationale로 최초 요청 구분, 복귀 시 실제 권한 재확인. UI 설정 이력은 단순 요청 여부만 저장하며 서비스·동의 계약은 바꾸지 않음.
- S2: 재조회 중 기존 상세·길안내 확인 상태 유지, 갱신 실패 시 이전 정보 안내와 재시도. 충전소가 삭제됐으면 캐시 목적지까지 제거. 현재 상태의 "공통 부품 수정하지 않음" 문구 정정.
- 검증: 권한 분기 테스트 4개와 상세 캐시 갱신·삭제 회귀 테스트 2개 추가. 임시 Robolectric에서 실제 설정 Intent·복귀 권한 갱신과 상세 유지·재시도 UI 확인.


### 2026-11-03 — 텍스트 줄바꿈 정리
- 문제: 차슐랭 실패 안내의 "요." 한 줄 고립, 좁은 동의 카드의 "필수"·"선택"·권한 설명 및 긴 시작 버튼 문구 잘림. 크루즈 S5 제목도 좁은 폭에서 "곳을"만 한 줄에 남음.
- 한 일: 동의 카드의 500dp 미만 본문 폭에서 버튼을 아래로 배치, 공통 시작 버튼/비활성 사유 분리. 공통 테마의 한국어 로캘·제목/본문 줄바꿈 설정을 두 앱에 동일 반영. 앱별 소개·실패 설명을 의미 단위로 나눔.
- 검증: 기본 화면 22개와 추가 좁은 화면 3개(가로 1024×600, 세로 600×1024)를 다시 렌더해 실제 줄 배치 확인. 가로·세로 PDF 23쪽도 줄바꿈 정리본으로 재생성. 임시 렌더 설정·샘플 데이터는 커밋하지 않음.
- 바꾼 파일: `ui/components/CarComponents.kt`, `ui/theme/Theme.kt`, `ui/onboarding/OnboardingScreen.kt`·`ui/review/FailureNoticeScreen.kt`, 본 문서.


### 2026-11-03 — 추가 요청 재점검
- 한 일: 사용자 "더 없어?"에 따라 초기 UI PR #1·현재 PR·인라인 댓글까지 재점검. 이상준 handoff의 S2 무한 로딩 요청을 추가 반영. UiState 오류 필드와 ViewModel 결과 투영·Screen 재시도 버튼을 같은 PR에 구현.
- 검증: 빌드·전체 테스트·앱 lint 통과. 회귀 테스트 4개 추가(충전소 없음/단일 조회, 실패→재시도 복구, 10초 시간 초과, 겹친 재조회 취소). UI는 오류 안내·재시도·뒤로 콜백과 로딩 중 재시도 없음도 임시 Robolectric으로 확인.
- 바꾼 파일: `ui/{UiStates,ChachelinNav}.kt`, `ui/stations/{StationDetailScreen,StationDetailViewModel}.kt`, `ViewModelsTest`, `StationDetailViewModelTest`, 본 문서.

### 2026-11-03
- 검증: `assembleDebug test :app:lintDebug` 통과. 앱 단위 테스트 67개(debug/release 각각), core debug 17개/release 3개. 앱 lint 오류 0(기존 경고 9개). 임시 Gradle init+Robolectric 2개 통과: 1280×720에서 알림 거부 상태로 시작 가능·알림 버튼 콜백, 720×1280에서 비동의 시작 비활성·알림 카드 접근 가능. 두 PNG를 직접 확인. 임시 테스트/의존성은 커밋하지 않음. Pleos 에뮬레이터 실행은 하지 않음.
- 한 일: 두 저장소의 열린 PR·이슈와 차슐랭 PR #2·#3 리뷰 확인. 이상준의 S7 알림 권한 요청을 UI에서 연결. S6 철회 버튼과 Nav ViewModel 연결 확인, 철회 확인 창의 대비·터치 영역 보완. S1·S2 평균 집계 없음은 "기록 없음"으로 표시.
- 결정: 알림 권한은 선택(거부해도 충전 기록 서비스는 동작), 요청은 S7 설명 뒤 버튼에서만. 알림 허용 여부는 앱 복귀 때 시스템 설정으로 다시 확인. 화면은 상태·콜백만 받음.
- 바꾼 파일: `ui/ChachelinNav.kt`, `ui/components/NotificationPermission.kt`, `ui/onboarding/OnboardingScreen.kt`, `ui/stations/{StationListScreen,StationDetailScreen}.kt`, 본 문서.


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
