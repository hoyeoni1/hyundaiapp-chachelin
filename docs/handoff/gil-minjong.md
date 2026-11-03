# 인수인계 — 길민종 (두 앱 UI)

담당 경로: `app/src/main/java/com/hyundaiapp/*/ui/`(화면·테마). 상태 계약 `ui/UiStates.kt`는 로직 담당과 합의

## 현재 상태 (2026-11-03 기준, 매번 덮어쓰기)

- 한 일 요약: 사용자 요청으로 PRD의 관측·해석 구분을 구체적인 수정안으로 작성. 충전량 증가 미확인·SoC 기반 계산 평균으로 제품 표현을 수정하고 관측 충분성, 취소 집계, 표본/조회 상태와 검증 기준을 제안했다. 기존 수식과 임계값은 유지한다.
- 진행 중 브랜치·PR: `docs/observation-based-prd` (PR 생성 후 링크 기록). 기존 UI 작업은 별도 PR #11: https://github.com/hoyeoni1/hyundaiapp-chachelin/pull/11
- 다음 할 일: 장호연·이상준·황준용: 관측 충분성 제외와 감점 적용 범위, 취소 반영 계약 검토. 합의 후 API/UiState·앱 문구를 구현 PR로 분리하고 장호연이 제출 원고·DOCX 갱신.
- 남은 UI 확인: x86 Pleos PC에서 가로·세로, 알림 설정 복귀, 동의 철회/재동의 및 결과/정차 화면 복귀 확인. 약관 전문 확정 후 `TermsDialog` 교체. 기존 UI PR에서 이어서 진행한다.
- 검토 사항: PRD는 장호연, 크루즈 판정 원본은 이민하 검토 대상. 사용자가 명시적으로 PRD 수정·PR 제출을 요청해 검토 브랜치로 작성했다. 담당자의 승인이나 머지가 완료됐다고 간주하지 않는다.
- 주의: 이 PR은 문서만 변경한다. 새 상태·로그·API 요구사항은 미구현 제안이며 제출 일정에 포함할 범위는 팀 검토 필요. 기존 수식·수치 기준의 임의 변경 없음. 머지는 장호연 담당.
- 확인 방법: `git diff --check`, 문서 내 용어·임계값·계산 예시·상대 링크 점검. JDK 17 `bash ./gradlew assembleDebug test` 통과(아래 기록 참조). 에뮬레이터 검증은 수행하지 않음.

## 작업 기록 (최신이 위, 지우지 않음)

### 2026-11-03 — 관측 기반 PRD 개정 제안
- 사용자 요청: 친구들이 구체적인 PRD 변경을 보고 의견을 줄 수 있도록 두 저장소에 각각 PR 제출.
- 변경: 충전량 증가 미확인·SoC 기반 계산 평균으로 제품 표현을 수정하고 관측 충분성, 취소 집계, 표본/조회 상태와 검증 기준을 제안했다. 기존 수식과 임계값은 유지한다.
- 다음 단계: 장호연·이상준·황준용: 관측 충분성 제외와 감점 적용 범위, 취소 반영 계약 검토. 합의 후 API/UiState·앱 문구를 구현 PR로 분리하고 장호연이 제출 원고·DOCX 갱신.
- 범위: PRD와 본인 handoff. 코드·서버·제출 DOCX는 수정하지 않았다.
- 검증: 문서 diff·상대 링크·수치 일관성 점검. JDK 17 `bash ./gradlew assembleDebug test` 통과(176 tests, 실패 0, 오류 0; debug/release 합계). 문서 요구사항의 실차 효과는 미검증.

### 2026-11-03 — 설정에서 알림 허용 후 미확인 횟수 초기화
- PR #7/#18 최신 리뷰의 선택 제안 반영: 설정에서 허용하고 돌아와도 미확인 횟수 2가 남던 동작을 재현(임시 Robolectric 검사에서 기대 0, 실제 2로 실패).
- `refresh()`에서 허용을 확인하면 횟수를 메모리·UI prefs 모두 0으로 초기화. 기존 실제 선택 이력은 유지하고, 자동 설정 실행·기록 동작·주행 제한은 바꾸지 않음.
- 검증: 두 앱 `assembleDebug test :app:lintDebug` 통과. 임시 Robolectric 권한 UI 검사 각 3개 통과. 미확인 2회 → 설정 허용 → 복귀 시 저장 횟수 0 → 다시 끄고 복귀 → 새 권한 요청 호출 및 횟수 1부터 재시작을 확인. 실제 Pleos 확인은 남음.
- 추가 PR·이슈 점검: 시연 도구 #5/#15는 머지 완료. 새 차슐랭 #8은 이상준 삭제 재시도, 크루즈 #19~#21은 이민하/황준용 업로드·철회 순서·TTS 수명 이슈이며 UI 요청은 없음.

### 2026-11-03 — 전체 PR·이슈 재점검 및 알림 예외 후속
- 두 저장소 PR 17개(열림·머지 전체), 이슈 6개와 리뷰·인수인계를 재확인. 담당 #9·#13과 이전 UI PR은 완료. 현재 남은 타 담당: 시연 도구 PR #5/#15 handoff 충돌·리허설(장호연), #12 실제 서버 삭제(황준용), 차슐랭 삭제 재시도(이상준), 약관 전문 확정 후 UI 문구 교체.
- 이전 UI PR 최종 리뷰의 알림 이력 소실 예외를 두 앱에 동일 보완. 미확인 결과가 반복되면 명시적 설정 버튼 제공, 첫 창 미선택 후 재요청과 진행 중 라벨은 유지. 카운트 영속화·화면 재진입·설정 자동 실행 없음도 확인 대상.
- 검증: JDK 17 `assembleDebug test :app:lintDebug` 통과. 새 회귀 테스트 2개(누락 이력→2회 결과 후 설정, 확정 결과 초기화)를 각 앱에 추가. 임시 Robolectric UI 검사 각 3개 통과(기존 거부→설정·복귀, 첫 창 닫기→재요청, 미확인 결과 횟수 저장·화면 재진입 후 설정 버튼·탭 전 설정 실행 없음). 실제 Pleos 검증은 수행하지 않음.

### 2026-11-03 — 추가 선택 리뷰 반영
- 새 리뷰: 두 PR 모두 머지 가능. 크루즈 `208e175`는 팀원이 실제 x86 Pleos 릴리스 시연 흐름 확인까지 통과(PR #16 댓글 참조).
- 알림: 요청 전에 이력을 저장하지 않고, 허용 또는 rationale로 선택 결과가 확인된 뒤 기록. 첫 창 미선택 닫기는 다시 요청 가능. 요청 진행 상태를 저장해 시스템 창 뒤의 버튼이 "알림 설정"으로 바뀌지 않게 함. 최초 취소·실제 반복 거부·진행 중 라벨 회귀 테스트 각 3개 추가(두 앱 동일).
- 검증: 임시 Robolectric에서 설정 진입/복귀와 첫 창 미선택 결과 뒤 [알림 허용] 유지·거부 이력 없음·재요청을 확인. app debug/release 각각 76개 테스트, 빌드·앱 lint 확인.


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
