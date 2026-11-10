# 인수인계 — 장호연 (팀장·총괄)

담당 경로: `tools/`, `apk/`, `README.md`, `PRD.md`, `docs/`, 릴리스. 총괄로서 전체 기반(초기 core·앱 뼈대)도 만들었음

## 현재 상태 (2026-11-09 기준, 매번 덮어쓰기)

- 한 일 요약:
  - **11-05 시연 흐름 확정**: 확정 시나리오 `docs/demo-scenario.md`(이 순서대로 누르면 그대로 동작해야 함). 앱은 debug·release 모두 S7→S1로 시작, 개발 메뉴는 S1 제목 길게 누르기, 제출 release도 시연 판정 기준(`DEMO_RULES`). 에뮬레이터 + 실제 서버로 처음부터 끝까지 확인. 차슐랭 PR #35 → `1047949`, 크루즈 PR #47 → `0823124` **머지 완료**, release APK `apk/` 커밋(아래 작업 기록)
  - 두 앱 기반 완료, `main`에 반영. 에뮬레이터에서 신호 수신 확인
    - core v0.2.1(초안)
    - 앱 구조·UiState 계약
    - Fake 저장소
    - 개발 메뉴·신호 모니터
  - 기획서 제출본 완성: 원고 `docs/proposal.md` → 접수양식에 채운 `docs/submission/chachelin-application.docx`(9쪽, Word로 열림 확인)
    - 재생성: `python tools/build-proposal-docx.py docs/submission/application-form-template.docx docs/proposal.md "<앱명>" docs/submission/chachelin-application.docx`
    - 01 참가신청서의 대표자·구성원 연락처, 참가 단위·부문 체크, 작성일, 04 서명은 아직 빈칸(손으로 기입)
  - 충전 판정·업로드 재시도·S2~S7 ViewModel·기록 서비스·동의 철회(이상준 PR #3)와 core v0.4.0 debug SDK 진단(황준용 PR #4)이 `main`에 들어옴(11-02, main `6bab9d0`). 머지된 main에서 `assembleDebug test` 통과(디버그 테스트 80개, 실패 0), core는 크루즈브레이커와 같음
  - 아직 없는 것: 서버(황준용 — Supabase로 확정(11-02, PC 호스팅 안 함). 황준용이 이미 작업했고 곧 커밋 예정), release 서명, 앱이 뒤에 있을 때 충전 결과 알림, S2 실측 속도 분포
- 진행 중 브랜치·PR:
  - 장호연 `fix/core-startup-resilience` — **11-04 머지 완료**(차슐랭 #30 → `0aa4032`, 크루즈 #42 → `9ff1318`). core v0.5.2 시작 크래시 방어 + `build-release.sh` 키 형식 검사(크루즈 쪽에는 안내 쿨다운 수정도). core(황준용)·`AppContainer`(이상준)를 장호연 결정으로 고쳤으므로 두 사람에게 알릴 것(황준용 확인 코멘트 없음)
  - 열린 PR #37(길민종 `feat/charge-start-notification-20261109`, 11-09 확인, 마지막 머지 #39 → `16547de`). #37은 main에 맞춰 다시 올리는 중(충돌 2파일). 열린 이슈 #12·#13. GitHub 아이디: 이상준 `wklee316`, 황준용 `Aodlf123`
  - 11-02 머지 때 승인한 것(장호연): `ui/` 2개 파일 수정(이상준, 길민종에게 알려 줄 것), `tools/windows-*.ps1`·`app/build.gradle.kts` 한 줄(황준용). 팀 문서 2절 담당표에 `AppContainer`·`ChachelinApp`·`RecordingService`·매니페스트를 이상준 담당으로 적는 일은 아직
  - 시연 배속 ×120(디버그 빌드만)·SoC 주입은 PT에서 밝혀야 함 → PRD 7절에 적기
  - 길민종 `feat/ui-screens` — 차슐랭 https://github.com/hoyeoni1/hyundaiapp-chachelin/pull/1 · 크루즈브레이커 https://github.com/hoyeoni1/hyundaiapp-cruisebreaker/pull/1 — **11-01 머지 완료**(Claude 리뷰 `✅ 머지 가능` 후 장호연 지시로 merge commit). 후속은 리뷰 코멘트의 "다음에 챙길 것"
  - 길민종 `fix/ui-review-followups` (리뷰 후속, PR 2번) — 차슐랭 https://github.com/hoyeoni1/hyundaiapp-chachelin/pull/2 · 크루즈브레이커 https://github.com/hoyeoni1/hyundaiapp-cruisebreaker/pull/2 — **11-01 머지 완료**(자동 리뷰 `✅ 머지 가능` 후 장호연 지시). 남은 것: 에뮬레이터 확인(길민종 Mac 불가 → 에뮬레이터 PC 필요), 이상준 질문 4개, 이민하 합의 3개(S4 자동 기록 토글·안내 거리·차속 없음)
  - 장호연 `feat/demo-scripts`(두 저장소) — 11-02 main merge(인수인계 충돌은 main 쪽으로 해결) + 판정 규칙 v2·현재 앱에 맞게 고쳐 PR. `demo-drive.sh` 기본은 고정 위치 시연(위치 재생 없이 지금 앱으로 동작, 이민하 확인 순서와 같음), `route`는 앱 위치 재생 모드가 생긴 뒤. 가짜 콘솔·가짜 adb로 값 순서만 확인했고 **에뮬레이터 실행은 아직**(이상준이 `demo-charge.sh`는 에뮬레이터에서 확인함)
  - 기타: 기획서(`docs/proposal.md`, `docs/submission/`)는 팀장 결정으로 `main`에 바로 push
- 리뷰·머지: PR 리뷰는 Claude(정기 작업 "삼각고래 PR 리뷰", 매일 09~23시 매시), 머지는 장호연만(`docs/pr-review-guide.md`)
- 다음 할 일(우선순위 순, 11-09 점검 기준. 서명 11-13, 발송 11-14, 사이트 마감 11-15):
  1. **#39 머지 후속(시작 화면 S1 → 홈)**: 문서는 고침(main `4a7b6d1`). #41(`demo-charge.sh` 주석) 머지 완료(`197eabb`). release APK 재빌드·리허설 통과(11-09). **11-10 #42(홈 뒤로 버튼 숨김)까지 넣어 다시 빌드·커밋**. #37이 머지되면 APK 다시 빌드·짧게 재확인. 길민종에게 `ui/` 수정 허용 알리기
  1-2. **열린 PR #37(길민종, 충전 시작 알림)**: 충돌 해결·버튼 홈 이동 완료, 11-09 재리뷰 `❓`(코드 문제 없음). **장호연 결정: 이상준 확인을 기다림**(예외 허용 안 함) → 확인되면 머지 → PRD 5.4·결과 알림 반영 + release APK 재빌드. 11-09 #42 머지 후 `CONFLICTING` → 길민종에게 main 재병합 요청 코멘트(`ChachelinNav.kt` 두 줄 양쪽 유지·`gil-minjong.md`). 재푸시되면 재리뷰
  1-3. **피드백 5·7 이번 제출 포함 여부: 이상준 선택 대기**(#39 코멘트). 7을 넣으면 11-11 머지 조건, 문서·DOCX·APK는 장호연
  2. **11-09 회의**: `docs/meeting-2026-11-09-station-data.md` — 충전소가 시드 10곳뿐인데 기획서는 "환경부 API 수집·캐시"로 읽힘(A1 문구 정직화는 꼭), 아파트·집 충전 처리(B1~B5). **11-09 A1 반영 완료**(기획서 5·7절 시드 문구, B1·B2는 출시 적용 규칙으로 적음, DOCX 재생성). **11-10 결정 전달받음**: DB에 없는 충전 장소는 개인 장소(비공개, 본인만)로 등록, 요청 시 검토 후 DB 등록, 아파트도 동일. A2(일괄 수집)는 이번 제출에서 안 함. 회의 문서 6절·PRD 5.3 반영. **기획서 반영은 7번 칸 기입 때 함께**(11-10 장호연 결정). 남은 것: 구현 여부·담당
  3. 주최 측 문의: **11-10 답 받음**: 심사용 GitHub 계정은 "추후 공지사항으로 안내" → 공지사항 확인을 이어 가고, 나오면 저장소(아직 PRIVATE) 초대·접근 확인(쓰기 권한·공개 전환 질문은 답 없음). **11-10 장호연 결정: 마감 전까지 공지를 기다림 — 미리 공개 전환하지 않음**. 시연 영상은 길이·형식 기준 없음, 파일·링크 모두 가능 → 촬영을 막는 것 없음. 출시 준비 3~6번은 1차 합격(11-23) 뒤 발송
  4. 시연 영상 촬영(`docs/demo-scenario.md` 순서) — **11-10 장호연 제작 중**. 촬영 직전 리허설 기록 정리는 `server/supabase/clear-rehearsal-data.sql`. **11-10 GitHub 주소·충전소 결정은 기획서에 반영·DOCX 재생성 완료.** 남은 것: 영상 링크가 오면 7번 칸 시연 영상 1곳 채우고 DOCX 재생성(앱명 인자 `"차슐랭 (Chachelin)"`)
  5. ~~01 참가신청서 기입~~ → **11-10 두 DOCX에 채움**(장호연이 차슐랭에 Word로 입력, 크루즈는 같은 값 복사. 이상준 소속·이메일은 빈칸 그대로, 04 동의 체크·성명까지). **11-10 장호연 결정: 연락처가 든 DOCX는 저장소에 올리지 않음.** 채운 제출본은 팀 폴더(git 아님) `submission-filled/<앱>-application-filled.docx`에 두고, 저장소 `docs/submission/`의 DOCX는 01·04가 빈 판으로 유지. 7번 칸 등으로 재생성하면 저장소 판을 커밋한 뒤, 같은 방법(아래 작업 기록)으로 01·04를 옮긴 새 제출본을 `submission-filled/`에 다시 만든다. **메일로 보낼 파일은 `submission-filled/`의 것**. 남은 것: 04 본인 서명·서명일자 → 5명 서명(11-13) → 11-14 메일 2통 발송(전원 CC, 제목에 앱명: `[차량용 앱 개발 공모전] 앱명_팀명_대표자명`, 설명회 20:24). 사이트 접수 마감 11-15(일)
  6. ~~서명 키 백업~~ → **11-10 장호연 백업 완료** — 끝남
  7. 약관(B01): 황준용 답(Supabase 리전·요청 로그)이 오면 `docs/terms-draft.md`와 `ui/TermsText.kt`의 2곳 고치기(판 번호를 바꾸면 AppContainer `termsVersion`도)
  8. 알릴 것: 황준용에게 core v0.5.2 변경 — 확인 코멘트 아직 없음
  9. 제출 필수 아님: 이슈 #12·#13(이상준·황준용 UI 감사 계약)
  - ~~Supabase 11-07 SQL(`20261107000100_demo_names.sql`) 적용~~ → 황준용이 11-09 공용 Supabase에 적용 완료(크루즈 이슈 #37 완료 코멘트, 11-10 확인) — 끝남
- 막힌 점·질문: 없음. "크루즈브레이커 DO 차단"은 10-31 재확인 결과 차슐랭 화면을 착각한 것(크루즈브레이커 `docs/background-freeze-experiment.md` "함께 확인한 것"에 기록, 이민하 질문 답변 완료)
- 주의:
  - gh는 브라우저 로그인이라 계정 전체 권한이다. 두 저장소로 줄이려면 fine-grained 토큰(Contents 읽기, Pull requests 읽기·쓰기)으로 다시 로그인
  - 결정 사항: 부문 **일반**, 팀명 **삼각고래**, APK는 사이트 규칙(`팀명_앱이름_release.apk`)대로 영문 팀명을 넣어 `apk/SamgakGorae_Chachelin_release.apk`(10-30 변경)
  - `getEvBatteryCapacity`는 `CAR_ENERGY`만으로 권한 부족 → 차종 프리셋 사용
  - Virtual Console은 하트비트가 없으면 쓰기가 503 → `tools/vconsole.sh up` 먼저
  - 이 PC는 python이 `py -3`(3.14)으로 있음(DOCX 재생성 가능, 11-03 확인)
  - 이 PC의 CLI 빌드는 JAVA_HOME이 없음 → `export JAVA_HOME="E:\AndroidStudio\jbr"`(JDK 21, Gradle 빌드·테스트 통과 확인)
- 확인 방법: `./gradlew assembleDebug test` / 시연 전체는 `docs/demo-scenario.md` 순서대로 / 신호 모니터는 S1 제목 길게 누르기 → 개발 메뉴

## 작업 기록 (최신이 위, 지우지 않음)

### 2026-11-10 — 기획서 7번 칸 GitHub 주소 기입, DOCX 재생성, 채운 제출본 다시 만듦(장호연 "저장소는 링크 넣어주고")
- `docs/proposal.md` 7절 GitHub 저장소 칸에 저장소 주소 기입(저장소는 아직 PRIVATE, 심사용 계정 공지 대기). 시연 영상 칸은 장호연이 나중에 줌 → 그대로 【제출 전 기입】
- 충전소 회의 결정을 기획서에 반영(7번 칸과 함께 하기로 한 것): 5절 판정 규칙 표 "공용이 아닌 충전소" 줄 → "목록에 없는 충전 장소(출시 적용)"(개인 장소 비공개 등록, 요청 시 팀 검토 후 목록 등록, 아파트 입주민 전용 포함), 5절 개인정보 첫 줄도 같은 내용으로
- `build-proposal-docx.py`로 DOCX 재생성(저장소 판, 01·04 빈칸) → 커밋. 팀 폴더 `submission-filled/`의 채운 제출본은 01·04를 새 DOCX에 다시 옮겨 교체(이전 채운 판과 텍스트 비교: 위 문구 변경만 다름, Word로 열림 확인)
- 장호연: 두 `submission-filled/` 파일을 구글 드라이브에 올려 팀원과 최종 확인 예정

### 2026-11-10 — 채운 신청서는 저장소 밖에 두기(장호연 "로컬에만 둠")
- 01·04를 채운 두 DOCX를 팀 폴더 `submission-filled/chachelin-application-filled.docx`·`cruisebreaker-application-filled.docx`로 옮기고(바이트 비교 동일), 저장소의 두 DOCX는 커밋된 판(01·04 빈칸)으로 되돌림. 실수로 `git add`되지 않게 하려는 것
- 제출 메일에는 `submission-filled/`의 파일을 붙인다. 서명도 그 파일에 받는다

### 2026-11-10 — 01 참가신청서·04 동의 정보를 크루즈브레이커 DOCX에도 채움(장호연 "크루즈브레이커도 동일하게")
- 장호연이 Word로 차슐랭 DOCX의 01(참가 단위 팀·부문 일반 체크, 대표자 성명·역할·소속·이메일·휴대폰, 인원 5명, 작성일 2026-11-10, B 구성원 표 5명)과 04(성명 5명·동의 체크)를 채움. 이상준 소속·이메일은 빈칸, 04 본인 서명·서명일자는 빈칸(손 서명)
- 크루즈브레이커 DOCX에 같은 값을 옮김: 두 DOCX의 본문 최상위 요소를 비교해, 차슐랭에서 글이 바뀌었고 크루즈에서 원래 글이 같은 요소(01의 9~10·13~18번 문단, 22번 구성원 표, 75번 동의 표)만 차슐랭 것으로 교체. 앱명 줄 등 앱마다 다른 요소는 건드리지 않음. 텍스트 비교로 차슐랭과 같은 값 확인, Word로 열어 PDF 14쪽 렌더링(1쪽·14쪽 눈으로 확인)
- **커밋 안 함**: 두 DOCX에 팀원 5명 휴대폰·이메일이 들어 있어 저장소(심사용 공개 가능성)에 올릴지 장호연 결정 대기. 로컬 파일만 바뀜
- 재생성 주의: 7번 칸·충전소 결정 반영으로 `build-proposal-docx.py`를 다시 돌리면 템플릿에서 새로 만들어 01·04 입력이 지워진다. 재생성 전 지금 DOCX를 따로 복사해 두고, 재생성 뒤 같은 방법(최상위 요소 텍스트 비교 후 교체)으로 다시 옮길 것

### 2026-11-10 — 장호연 진행 상황 반영: 서명 키 백업·영상 제작·충전소 회의 결정(main 직접 push)
- 장호연 전달(11-10): 서명 키 백업 완료, 시연 영상 제작 중
- 11-09 회의 결정(장호연 전달): DB에 없는 곳에서 한 충전은 **개인 장소로 등록(비공개, 본인만 확인)**, 운전자가 요청하면 팀이 검토해 충전소 DB에 등록. 아파트 등도 같은 방식. 환경부 API 일괄 수집(A2)은 이번 제출에서 안 함
- 반영: `docs/meeting-2026-11-09-station-data.md` 6절(결정), `PRD.md` 5.3 표 한 줄. 기획서(`docs/proposal.md`·DOCX)는 **7번 칸 기입 때 함께 출시 계획으로 반영**(장호연 결정)
- 미정: 이번 제출 구현 여부(출시 계획으로만 적을지), 개인 장소 저장 위치, 요청 검토 담당

### 2026-11-10 — release APK 재빌드(#42 반영, 장호연 "지금 재빌드")
- 제출 APK(`f9875a5`)가 #42(`ee5055d`, 첫 화면 홈 뒤로 버튼 숨김) 이전 빌드였음. #37은 이상준 확인 대기라 기다리지 않고 main `1f281c3`에서 `bash tools/build-release.sh` → `apk/SamgakGorae_Chachelin_release.apk`(24M, 시연 데이터 모드, 서명 확인) 커밋
- 에뮬레이터 `Pleos_Connect_2_1_0`, release 설치·`pm clear` 후 실제 서버: S7 동의 → 홈(**왼쪽 위 뒤로 버튼 없음** 확인) → [근처 충전소] S1 충전소 목록(서버 시연 데이터) → [<] 홈 복귀. 충전 흐름(2~11단계)은 코드 변경 없어 생략
- 메모: 에뮬레이터를 새로 켠 직후에는 S1이 "차량 위치를 아직 받지 못했어요" → 시나리오 문제 해결표대로 `vconsole.sh up` + 내비 앱 한 번 실행 뒤 [다시 시도]로 해결. 촬영 전 같은 순서로 준비할 것
- #37이 머지되면 한 번 더 재빌드

### 2026-11-10 — 인수인계 정정: 서버 시드 이름 SQL 적용 완료 확인(main 직접 push)
- 11-10 상태 점검에서 확인: 황준용이 11-09 `20261107000100_demo_names.sql`을 공용 Supabase에 적용하고 크루즈 이슈 #37을 닫음(11-09 14:59, 완료 코멘트). 이 문서의 "황준용에게 넘김" 줄을 완료로 고침
- 그 밖의 11-10 상태: 차슐랭 #37 여전히 `CONFLICTING`(11-09 재병합 요청 뒤 새 push 없음), 크루즈 #54 열린 채(제출 뒤 머지 방침), 새 PR 코멘트 없음

### 2026-11-10 — 주최 측 문의 답 반영(main 직접 push)
- 장호연이 받은 11-10 주최 측 답: ① 심사용 GitHub 계정은 추후 공지사항으로 안내 예정 ② 시연 영상은 길이·파일 형식 기준 없음(제출 내용을 모두 담으면 됨), 영상 포함 제출 자료는 파일 또는 링크로 제출 가능
- 반영: 두 저장소 `docs/organizer-inquiry-email.md`(+ 팀 폴더 원본) 발송 상태 줄, `docs/team-collaboration-plan.md` 9절 4·5번·10절 체크리스트, 팀 폴더 `contest-analysis-todo.md` 설명회 미답변 ①④, 이 문서 "다음 할 일"
- 장호연 결정(11-10 "공지 기다리기"): 계정 공지가 마감(11-15) 전에 안 나와도 미리 공개 전환하지 않고 공지를 기다림. 공지가 나오면 바로 초대·접근 확인. 영상은 형식 제약이 없어 촬영 대기 이유가 계정 공지와 무관해짐

### 2026-11-09 — 기획서 S1 그림을 리허설 캡처로 교체, DOCX 재생성
- 장호연 "네, 교체". 오늘 리허설 캡처(`screenshots-2026-11-09/demo-03.png`·`demo-05.png`)를 같은 틀(BOX 853,90,2560,1300 → 1280×907)로 잘라 `docs/images/ch-s1-stations.png`·`ch-s2-detail.png`에 덮었는데, **S2는 기존 파일과 바이트까지 동일**(별점 분포가 이미 적용된 상태로 찍혀 있었음) → S1만 바뀜(목록 상단, 점수 3.5~3.3)
- DOCX 재생성(`build-proposal-docx.py`, 그림 8장, zip·rels·content-type 검사 통과). 숨은 WINWORD 2개가 파일을 잠가 1차 실패 → 장호연 허락 후 `Stop-Process`로 종료하고 재생성. Word로 열어 보는 확인은 아직 안 함
- 기획서 그림 교체 후보로 적었던 항목(11-09 리허설 기록)은 이것으로 종료


### 2026-11-09 — 팀 시연 리허설(release APK, 시나리오 1~11단계 전부 통과)
- 장호연 요청("시연 좀 하게 차슐랭 스크립트대로 실행"). 설치된 11-09 제출용 release APK(`f9875a5`)로 `pm clear` 후 S7부터 `docs/demo-scenario.md` 순서대로 Claude가 adb 탭·스크립트로 진행. 1~11단계 모두 문서대로 동작(4단계 길안내 포함: 내비 "시흥동 주민센터 · 2m · 1분", `am start`로 복귀)
- 확인된 값: S2 별점 분포 14/12/8/1/1(11-09 별점 분포 migration 적용됨) → 평가 뒤 5점 15건·인증 48회, 실패 뒤 7일 실패 8회·최근 실패 "11월 9일 22:00"·점수 3.3→3.2. S3 50%·8분·65 kW, S4 40→80%·35분·52 kW. 별을 눌러도 태그 위치 안 밀림
- 메모: `tools/demo-charge.sh`는 PATH에 adb가 없으면 실패 → `ADB=/e/Android/Sdk/platform-tools/adb.exe` 환경변수 필요. `reset`을 돌리면 앱 서랍이 앞으로 올라오므로 그 뒤 `am start`로 앱을 다시 앞으로. 기획서 그림(ch-s1·ch-s2)은 별점 분포 적용 전 캡처라 교체 후보(이번 캡처 `screenshots-2026-11-09/demo-*.png`)


### 2026-11-09 — 차슐랭 #42 머지(장호연 결정), #37 재병합 요청
- 장호연 "네, #42 머지" → #42(길민종, 첫 화면 홈의 헤더 뒤로 버튼 숨김) main `3ef3869`로 머지, 브랜치 삭제. 그 결과 #37이 `CONFLICTING`이 되어 길민종에게 main 재병합 요청 코멘트(충돌 2파일, 해결법 적음). 이상준 확인·PRD 반영은 그대로 남음
- `docs/demo-scenario.md` 5절 "홈에서 뒤로 가면 앱이 닫힌다" → 홈에는 뒤로 버튼이 없고 차량 홈 버튼으로 나간다로 고침
- release APK는 아직 #42 이전 빌드(`f9875a5`). 홈 헤더 버튼 하나 차이라 #37 결론이 난 뒤 한 번에 재빌드하면 됨

### 2026-11-09 — 열린 PR 최종 체크(#42·#37, 크루즈 #54)
- 장호연 요청("PR 최종 체크"). 세 PR 모두 head가 마지막 리뷰 시점과 같음(#42 `ee5055d` ✅, #37 `6c2787d` ❓, 크루즈 #54 `a1e82a5` ❓). 작성자 외 코멘트(이상준·이민하 확인) 아직 없음
- 현재 main(`a61d2d4`, #43 tools·문서 포함) 기준 테스트 머지: #42 단독 충돌 없음, #37 단독 충돌 없음, **#37+#42 함께는 `ui/ChachelinNav.kt`(홈 `onEnableNotifications` 줄 + `onBack = back.takeIf { stackSize > 1 }` 줄 양쪽 유지)·`docs/handoff/gil-minjong.md`(양쪽 기록 유지) 충돌** → 나중에 머지하는 쪽이 main 재병합. 그렇게 푼 상태로 `assembleDebug test` 통과(app debug·release 108, core 32·18, 실패 0)
- 체크리스트: 비밀값·apk·한글 이름 없음, 매니페스트·`core/` 변경 없음. #37은 이상준 담당 파일(`AppContainer`·`MainActivity`·`ChargeNotifications`·`domain/ChargeStartNotices`)을 고친 점과 PRD 미반영(5.4 홈·S3·S7, 116행 결과 알림)이 그대로 → 장호연 결정 사항 유지
- #42를 머지하면 `docs/demo-scenario.md` 68행 "홈에서 뒤로 가면 앱이 닫힌다"를 고쳐야 함(홈 뒤로 버튼이 사라짐) → 아래 11-09 #42 머지 항목에서 처리

### 2026-11-09 — tools PR 머지(장호연 "네, 머지")
- 차슐랭 #43 → main `3cdc4a7`, 크루즈브레이커 #55 → main `ed73f19`(merge commit, 원격 브랜치 삭제). 두 저장소 tools 일치 확인(`sync-core.sh --check`: core 0.5.2)
- 이제 main의 `tools/build-proposal-docx.py`가 그림을 넣는다. DOCX 재생성 명령은 그대로. 남은 것: DOCX Word 렌더링 확인(장호연), 별점 분포 migration 적용 후 차슐랭 S1·S2 재촬영

### 2026-11-09 — 기획서 DOCX 화면 캡처 삽입(A-7, 장호연 "네, 캡처 촬영")
- 제출 release APK(11-09 빌드)를 에뮬레이터에 설치·`pm clear` 후 `docs/demo-scenario.md` 순서로 실행하며 캡처(원본 2560×1440은 팀 폴더 `screenshots-2026-11-09/`). 앱 영역(853,90)~(2560,1300)만 잘라 1280px 폭 PNG로 `docs/images/`에 저장: 차슐랭 8장(S7·홈·S1·S2·S3·S4·S5·S6), 크루즈 5장(S5·S2·S1 평소·S1 배너·S3)
- `tools/build-proposal-docx.py`에 `![설명](images/파일.png)` 그림 삽입 지원 추가(PNG, 칸 너비 맞춤, 설명은 아래 작은 글씨, rels·content-type 자동). **tools/는 PR 규칙** → 브랜치로 올려 장호연 머지 필요. 크루즈브레이커에는 `sync-core.sh`로 맞춤
- `docs/proposal.md` 3·4·7절에 그림 줄 추가(캡션에 release APK·에뮬레이터·시드 데이터 명시) → DOCX 재생성(차슐랭 8장 880KB, 크루즈 5장 358KB). XML·관계·content-type 검증 통과. Word 렌더링 확인은 아래 참고
- 캡처 중 확인한 것: 원격 DB에 11-07 이름 migration은 들어가 있음(S1에 "(시연)" 지명, 크루즈 S2 "판교IC 진입로"). **11-09 별점 분포 migration은 아직**(S1 점수 3.5·3.4·3.3) → 황준용 적용 뒤 S1·S2 두 장만 다시 찍어 교체 권장

### 2026-11-09 — 기획서 문구 수정 A-1~A-6 반영(장호연 "네, 문구 수정 시작", 검토 보고서 6절 A항)
- 차슐랭 `docs/proposal.md`·`PRD.md`: 충전소 데이터 출처 정직화(5절 SDK 표 "미연동·시드 10곳", 3절 비교표, 구조 표, 7절 시연 안내), 미사용 신호 2줄 정리(충전 스위치 → F4 확장, 충전 사업자 삭제), 공용 아닌 충전소(이용자 제한·집 반경) 규칙을 출시 적용으로 추가, 사업성 재구성(현대차그룹 내부 활용을 2단계로·CPO는 4단계, MAU 표를 국토부 신규 등록 22.1만×국산 60% 기준으로 숫자화, Retention 근거, 확장에 실패 판정 보조 신호·순간 충전 전력·입주민 모드)
- 크루즈브레이커 `docs/proposal.md`·`PRD.md`: 사고 통계를 도로공사 5년 69.8%(서울신문 2022-09)로 교체, "정상 해제 제외" 규칙(경로 안내 중 다음 진출·요금소 1km 이내면 기록 안 함, 2차 PT까지 구현) + IC·요금소 반박 대응 문단, SDK 표에 NaviHelper 경로 정보 행, 콜드스타트 계산, 사업성 1순위를 그룹 ADAS 개입 데이터로, 안전 고지 한 줄
- 두 DOCX 재생성(`tools/build-proposal-docx.py`), 새 문구 포함 확인. **커밋·푸시는 아직**(장호연 확인 뒤 main 직접 push)
- **근거 최종 검토(장호연 요청, 같은 날)**: 외부 출처 3건 원문 확인 → 세계일보 2026-01(국토부: 2025 신규 EV 22.1만, 누적 89.9만) OK / 서울신문 2022-09(도로공사 5년 69.8%) OK / 헤럴드경제 63.5%는 원문 확인 실패 → **KAMA 결산(파이낸셜뉴스 2026-01: 22만 177대, 국산 57.2%)과 제조사별 등록(뉴스와 2026-02: 기아 60,609·현대차 55,461)으로 교체**, "그룹 EV 연 13만 추정"을 실측 11.6만으로 바꿈. SDK 주장은 `vehicle-data-findings.md`로 대조 → TBT에 `TURN_TOLL`·"IC 방면" 실측 있음(OK), 휴게소는 TBT에 없어 규칙에서 뺌, 도로공사 IC·요금소 좌표 공개 데이터는 미확인이라 "출처는 출시 전 확정"으로 완화, 연료 신호는 에뮬레이터 미검증 표기, "충전 사업자 어디에도 없다"는 과장이라 "사업자를 가리지 않고 차량 쪽에서 본 결과"로 고침, 크루즈 안전 고지는 S5에만 있어 "설정" 삭제, 콜드스타트 계산을 구간 분배 가정 명시로 다시 씀. DOCX 2개 재생성
- 요청할 것: 이민하에게 `docs/disengage-rules.md`에 "정상 해제 제외" 규칙 추가 + `DisengageDetector` 구현(2차 PT까지), 황준용에게 원격 migration 적용(11-07 이름·11-09 별점)

### 2026-11-09 — 두 작품 비판적 검토 보고(심사위원 관점, 장호연 요청)
- 팀 폴더 `hyundai-app/contest-critical-review-2026-11-09.md` 작성(11-04 검토 후속). 문서 전수 + 코드 감사 에이전트 2개(읽기 전용) + `gradlew test --offline` 재실행(차슐랭 262·크루즈 133, 실패 0)
- 결론: 둘 다 1차 통과 유력, 수상은 경합(크루즈 상한 높음·차슐랭 하한 높음). 11-14 전 필수: 기획서 DOCX 그림 0장 → 화면 캡처 삽입, 차슐랭 충전소 데이터 출처 문구 정직화(회의 안건 A1), 크루즈 IC·톨게이트 정상 해제 방어 문단, 시드 보강(점수 분포·경계 구간), 영상 촬영, 저장소 공개 기준
- 2차 PT 전: 크루즈 NaviHelper 경로 정보로 IC 진출 해제 제외, 차슐랭 충전 스위치 신호로 실패 판정 정교화, 두 에뮬레이터 시연
- 코드 변경 없음. 상세·담당·기한은 보고서 6~7절

### 2026-11-09 — 피드백 5·7 기간 내 구현 검토 → 이상준이 선택(장호연 "상준이가 선택하는걸로")
- 7(속도 태그 → 실측): 약 1일. `stations.max_kw`·`Station.maxKw` 이미 있음(S2 미표시), 서버 변경 불필요(리뷰 RPC는 태그 부분집합 허용). S2 표시(길민종)·S4 태그 6→4·테스트(이상준)·S2 상위 태그에서 FAST/SLOW 거르기, 문서·APK는 장호연. 11-11 머지 조건, #37 다음
- 5(텍스트 코멘트): STT SDK "준비 중"·에뮬레이터 미검증 → 불가에 가까움. 키보드 입력은 서버 칸·RPC(황준용)·약관 판 번호·S4·S2 공개 표시가 같이 바뀌어 2~3일·4명, "키보드 없음" 차별점과 충돌
- #39에 선택 요청 코멘트(이상준 멘션, 체크박스 4개). **이상준 답이 오면** 그대로 진행: 7을 넣으면 길민종·이상준 작업 → 머지 → 기획서 F5·4절·확장 계획 줄·PRD S4·demo-scenario 8단계·DOCX·APK·리허설. 안 넣으면 지금 문서 그대로

### 2026-11-09 — #39 시연 피드백 5·7 결정 반영(장호연 "5번 상준 추천안으로, 7번도 확장계획에")
- 5(리뷰 텍스트 코멘트): 이번 제출 제외, 확장 계획 "음성 후기(STT)". 7(속도 태그 → 실측): 확장 계획 "S2 계산 평균 kW vs 충전기 최대 kW", 이번 제출은 태그 유지·PT에서 차량 계산 kW 강조
- 차슐랭 main 직접 push `f6ac2b9`: `docs/proposal.md` 6절 "서비스 확대 계획" 아래 "기능 확장(다음 버전)" 2줄, `PRD.md` 6.4, 기획서 DOCX 재생성(새 문구 들어간 것 확인)
- #39에 결정 답변 코멘트(이상준 멘션). 앱 코드 변경 없음

### 2026-11-09 — README 제출 항목 점검(장호연 "네, 고쳐 줘")
- 두 저장소 README에 제출 요구 4항목(앱 이름·한 줄 소개, 실행 방법, 테스트 환경·설정, APK 위치)이 모두 있음 확인. GitHub Release는 없음(APK 위치로 충족)
- 크루즈브레이커는 최신(APK가 마지막 코드 커밋 뒤). 차슐랭 README 3곳 수정, main 직접 push `c6542b6`: APK "11-07 빌드" → 11-09, 실행 방법 3번 [시작하기] → 홈 → S1, 개발 메뉴는 홈·S1 제목 길게 누르기

### 2026-11-09 — 차슐랭 #37 재리뷰(코멘트만, 머지 안 함)
- 길민종이 main 병합·충돌 해결·`충전 알림 켜기`를 새 홈으로 옮김(head `6c2787d`, `MERGEABLE`). main `f9875a5`에 병합해 `assembleDebug test` 통과(266개, 실패 0)
- 에뮬레이터(디버그, 실제 서버): 홈 버튼 → 권한 창 → 허용 후 버튼 숨김, 앱이 뒤에 있을 때 시작 알림 1건, 충전 중에 누르면 S3(시연 값 부제), 끝난 뒤 누르면 S4. 확인 뒤 에뮬레이터는 `apk/` release로 되돌림(앱 데이터 초기화됨)
- 판정 `❓`: 코드 문제 없음. 남은 것은 이상준 담당 경로 확인(또는 장호연 예외)과 PRD 반영. 머지하면 PRD 5.4·결과 알림 설명 고치고 release APK 재빌드
- 확인 중 설치 실패로 탭 몇 번이 크루즈브레이커 첫 실행 화면에 들어갔지만, 약관 창만 열렸고 동의는 저장되지 않음(화면으로 확인)

### 2026-11-09 — release APK 재빌드 + 홈 흐름 리허설(장호연 "네, 지금 해 줘")
- main `197eabb`(#39·#41 포함)에서 `bash tools/build-release.sh` → `apk/SamgakGorae_Chachelin_release.apk`(24M, 시연 데이터 모드, 서명) 커밋
- 에뮬레이터 `Pleos_Connect_2_1_0`, release 설치·`pm clear` 후 실제 서버로 `docs/demo-scenario.md` 순서: S7 동의 → **홈**(막대·카드 정상) → [근처 충전소] S1 → 시흥동 S2 → `success`: S3(막대 숨김) → S4 → 5점·빠름·대기 없음 → S2 인증 44→45 → `fail`: S5 → [닫기] S2 7일 실패 6→7 → 막대 [내 기록] S6 인증·실패 2건 → 막대 [홈] "2건"·최근 기록 줄. 모두 통과. 4단계 길안내는 변경 없어 생략
- 본 것(버그 아님): ① S1 점수가 대부분 3.5·3.4로 비슷 → 11-09 별점 분포 migration이 원격에 아직 안 들어감(황준용 적용 대기). 적용 뒤 영상 촬영 ② 홈(루트)에도 왼쪽 위 [<]가 있어 누르면 앱이 닫힘 — 의도대로지만 길민종에게 숨길지 물어볼 것 ③ 리허설 기록(시흥동 인증 1·실패 1)이 서버에 남음 → 촬영 전 `server/supabase/clear-rehearsal-data.sql`
- `docs/demo-scenario.md` 머리 메모를 "리허설 통과"로 고침

### 2026-11-09 — 열린 PR 충돌 확인(코멘트·수정 없음)
- 차슐랭 #37(head `0fb9904`) `CONFLICTING`: #39 머지 뒤 새 커밋 없음. 충돌은 `ui/ChachelinNav.kt` 2곳(`LaunchedEffect(openCharging)` 자리, `ScreenContent` 구조로 옮길 S1 알림 버튼·S3/S4 `showDemoValues`)과 `docs/handoff/gil-minjong.md`. 장호연 결정: **길민종이 풀 때까지 기다림**(할 일은 PR 코멘트에 있음)
- 크루즈 #54 `MERGEABLE`, head `7e5b01a`(리뷰 코멘트에 적힌 `69fde5e` 뒤 캐시 3줄 커밋). 제출 뒤 머지할 때 이 커밋까지 재확인

### 2026-11-09 — 차슐랭 #41 머지(장호연 "네, 머지해 줘")
- `tools/demo-charge.sh` 전제 주석(S1 → 홈) 한 줄. 헤드 `e02d4bd` 확인 후 merge commit → main `197eabb`, 브랜치 삭제. 주석만이라 빌드 영향 없음

### 2026-11-09 — #39 후속: 시연 시나리오·PRD를 홈 시작 화면에 맞춤(장호연 "네, 고쳐 줘")
- 머지된 main 코드(`ChachelinNav.kt`·`HomeScreen.kt`)로 실제 흐름 확인 후 main 직접 push `4a7b6d1`: `docs/demo-scenario.md`(머리 갱신 메모, 준비 6, 1·2·11단계, 복구 표 S4 행, 빌드 메모: 시작 화면 홈·개발 메뉴는 홈 "차슐랭" 또는 S1 제목 길게 누르기), `PRD.md`(5.4에 홈 행·S7 설정 재진입, 7절 1번, 211행)
- 충전 흐름 뒤 돌아가는 화면은 그대로(S4 [평가 보내기]·S5 [닫기] → S2)라 6~10단계는 그대로 둠
- `tools/demo-charge.sh` 전제 주석 한 줄은 `tools/` PR 규칙대로 차슐랭 PR #41(`docs/demo-charge-home-comment`)로 올림 — 장호연 머지 필요
- 남은 것: release APK 재빌드(`bash tools/build-release.sh`) → 새 흐름으로 에뮬레이터 리허설. #37이 다시 올라와 머지되면 한 번 더

### 2026-11-09 — 차슐랭 #39 머지(장호연 "네, #39 먼저")
- 순서 결정: #39·#37을 두 순서로 시험 머지 → 어느 쪽이든 `ui/ChachelinNav.kt` 2곳·`docs/handoff/gil-minjong.md` 충돌. #39(화면 구조를 `ScreenContent`+전환 애니메이션으로 바꿈)가 커서 먼저 넣고 #37을 그 위에 맞추기로 함. 버튼 `충전 알림 켜기`는 S1이 아니라 새 홈에 두기로 함
- #39 헤드 `d8e886d`(리뷰한 #40 머지) 그대로 확인 후 merge commit → main `16547de`. 길민종 `ui/` 수정은 장호연 결정으로 허용(길민종에게 알릴 것). 머지 후 main `assembleDebug test` 통과
- #37에 코멘트: main 병합·충돌 해결·버튼을 홈으로·이상준 확인 요청
- **시작 화면이 바뀜**(동의 후 S1 → 홈): `docs/demo-scenario.md` 1단계, PRD 5.4 화면 목록·211행, `tools/demo-charge.sh` 주석 고치기, release APK 재빌드·리허설은 아직

### 2026-11-09 — 차슐랭 #40 머지(장호연 "네, 머지해 줘")
- 리뷰한 헤드 2e6fef5 그대로 확인 후 #39 브랜치 `fix/demo-feedback-20261109`로 머지(d8e886d). main·release APK는 그대로
- 이제 #39에 홈 차종 갱신 버그 수정이 들어 있음. #39는 main에 충돌 없이 합쳐짐. 남은 판단: 시연 흐름 변경(동의 후 S1 → 홈)·#37과 `ChachelinNav.kt` 충돌·demo-scenario·PRD 갱신 → 차슐랭 회의에서 결정

### 2026-11-09 — PR 재확인: 차슐랭 #40 신규·#37/#54 갱신(코멘트만)
- 차슐랭 #40(길민종, #39 후속: 홈 차종 갱신·홈 뒤로 버튼, base=#39 브랜치) `✅ 머지 가능`: 빌드·테스트 212 통과. #39 버그 해결. `VehicleProfileStore`는 이상준 경로라 이상준 확인. #39는 여전히 `❓`
- 차슐랭 #37: 새 커밋은 main 병합 + 인수인계 문구뿐 → `❓` 그대로(코멘트 추가 안 함)
- 크루즈 #54 새 커밋 69fde5e: 정차 중에만 표시, S1·헤드업 억제 제거(11-05 결정 유지), 설정 OFF면 루프 안 돎, PRD 변경·로컬 경로 삭제. 빌드·테스트 188 통과 → `❓`(새 권한 `SYSTEM_ALERT_WINDOW`·이민하 확인), 권고는 제출 뒤 머지. 출시 준비 문의에 오버레이 권한 허용 여부 추가 권장

### 2026-11-09 — PR 머지: 차슐랭 #38 · 크루즈 #53(장호연 "네, 머지해 줘")
- 리뷰한 헤드 그대로 확인 후 머지: 차슐랭 #38(84dcabd → 4968d87, 서버 별점 분포 migration·문서), 크루즈 #53(8549c6b → 503d721, 황준용 인수인계)
- 앱 코드 변경 없음 → release APK 재빌드 불필요. **원격 DB 적용은 아직**: 황준용이 `db push --linked --dry-run`(11-07 이름 + 11-09 별점) → `db push --linked` → `refresh-demo-dates.sql` → `verify.sql`(버전 6, 시드 325). 적용 확인 후 크루즈 이슈 #37 닫기

### 2026-11-09 — 새 PR 5건 리뷰(코멘트만, 머지 안 함)
- 차슐랭 #38(황준용, 시드 별점 분포 SQL) `✅ 머지 가능`: DB 테스트 20 통과. 머지 뒤 황준용이 11-07 이름 + 별점 migration 원격 적용(→ 크루즈 이슈 #37 닫기). 적용 후 시드 최고 평균 약 4.3이라 영상 내레이션 숫자 확인
- 크루즈 #53(황준용, 인수인계 문서) `✅ 머지 가능`
- 차슐랭 #37(길민종, 충전 시작 알림) 새 커밋 4bfc38b(ui만: 알림 켜기 버튼, "시연 값" 부제) 확인 → `❓` 그대로
- 차슐랭 #39(이상준, 홈 화면·왼쪽 막대·전환 애니메이션·설정 재진입) `❓`: 빌드·테스트 통과. 버그 1건(설정에서 차종 바꿔도 홈 "내 차" 그대로 — `onboarded` StateFlow 재방출 안 됨). 길민종 `ui/` 경로, 시연 흐름 변경(동의 후 S1 → 홈), #37과 `ChachelinNav.kt` 충돌. 머지하면 demo-scenario·PRD 5.4·200·211행·`tools/demo-charge.sh` 주석·APK 재빌드·리허설
- 크루즈 #54(길민종, 내비 위 오버레이) `❓`, **제출 뒤로 미루기 권고**: 빌드·테스트 통과. 카드가 떠 있으면 S1·헤드업을 막아 안내가 사라질 수 있음, 주행 중 최대 5줄(여러 줄 금지 위반), 새 권한 `SYSTEM_ALERT_WINDOW` 앱마켓 위험, PRD에 작성자 로컬 경로·+132줄, 이민하 담당 파일 승인 없음, 11-05 결정(S1 전면 전환) 변경

### 2026-11-09 — 주최 측 문의 메일 발송 기록(main 직접 push)
- 장호연 11-09: 제출 관련 1·2번(심사용 GitHub 계정, 시연 영상 길이·형식)만 발송 완료, 답 대기. 출시 준비 3~6번(FGS·부팅 수신, DO, 시동 off, 크루즈 해제 값)은 1차 합격(11-23) 뒤 발송
- 메일 문서 머리·팀 문서 9절·"다음 할 일"에 반영

### 2026-11-09 — 문의 메일 2번(저장소·APK 경로) 뺌, 6문항(main 직접 push)
- 장호연 지적: 제출 안내 "제출 전 확인해 주세요"가 APK를 이미 답함 — release APK, 100MB 넘으면 Releases, README에 APK 위치. 두 저장소 README에 4항목(앱 이름·실행 방법·테스트 환경·APK 위치) 다 있고 APK는 24MB·42MB라 `apk/` 그대로 OK
- 저장소 주소는 설명회대로 메일 본문 + 접수양식 7번 칸에 적으면 됨 → 문항 뺌. 남은 것: 제출 2(심사용 계정·영상 형식) + 출시 준비 4

### 2026-11-09 — 문의 메일 재점검: 설명회 녹취·코드 대조, 7문항(main 직접 push)
- 장호연 요청("SDK 심사 관련은 영상 스크립트도 뜯어봐서 다시 검토, 정말 우리 앱에 필요한 것인지 확인"). Claude가 10-28 설명회 녹취 `docs/transcript.txt` 966줄 전체와 두 앱 매니페스트·코드를 대조
- 설명회에 답이 있어 뺌: 두 작품 제출(복수 출품은 메일 제목에 앱명으로 구분, `[차량용 앱 개발 공모전] 앱명_팀명`), 퍼미션 사전 협의(공모전 개발 단계는 권한 신청·승인 불필요, 매니페스트 선언만. 승인은 앱마켓 배포 심사 때). 필요 없어 뺌: 위치기반서비스(공모전 단계 서버엔 시드·시연 데이터뿐), 탑재 차종
- 남김: 제출 3(심사용 계정, 저장소·APK 경로 기재 위치, 영상 형식) + 출시 준비 4(포그라운드 서비스·부팅 수신, DO, 시동 off, 크루즈 해제 값). 코드에서 실제로 쓰는지 확인함(DO는 크루즈만)
- 함께 알게 된 것(제출에 반영할 것): **11-14 접수 메일은 작품별 2통, 제목에 앱명** — 예 `[차량용 앱 개발 공모전] 차슐랭_삼각고래_장호연`. 수정 제출은 제목 앞 `[수정 제출]` + 전체 자료·서명 동의서 다시 + 전원 CC. 외부 API·네트워크 사용 가능. 실차 시연 없음(에뮬레이터 검증)
- 두 저장소와 팀 폴더 원본 같은 내용. 발송은 장호연이 직접

### 2026-11-09 — 문의 메일에 심사용 GitHub 계정 문항 다시 넣음(main 직접 push)
- 장호연 요청("그럼 문의해야겠네"). 사이트에 계정 공지가 없어 메일 2번으로 다시 넣음: 언제·어디에 공지되는지 + 개인 계정 비공개 저장소는 초대 시 쓰기 권한이 같이 가는데 그대로 초대할지, 공개 전환이 나은지. 10문항 → 11문항
- 두 저장소 `docs/organizer-inquiry-email.md`와 팀 폴더 원본을 같은 내용으로 맞춤. 팀 문서 9절 참조 줄도 고침. 발송은 장호연이 직접

### 2026-11-09 — GitHub 제출 안내 대조·심사용 계정 확인
- 장호연 요청(사이트 "GitHub 저장소 제출 안내" 붙여넣음). Claude가 두 저장소를 안내와 대조: `apk/` 위치·파일명(`SamgakGorae_*_release.apk`)·서명 release·용량(24MB·42MB, GitHub 100MB 한도 안이라 Releases 불필요)·README 4항목(앱 이름·한 줄 소개, 실행 방법, 테스트 환경·설정, APK 위치) 모두 맞음. README의 APK 빌드 날짜만 낡아서 고침(차슐랭 11-07, 크루즈 11-09)
- 공모전 사이트(메인·FAQ·공지·가이드 전 페이지·Q&A)를 다시 훑음: **심사용 GitHub 계정은 아직 어디에도 공지되지 않음**. 공지는 여전히 3건(유의사항·모집 공고·설명회)이고, FAQ·제출 안내에는 "공지된 심사용 계정"이라는 문구만 있음
- 참고: 저장소는 둘 다 PRIVATE, 협업자는 팀원 5명(hoyeoni1 admin, 나머지 write). 개인 계정 저장소라 심사용 계정도 쓰기 권한으로 초대됨(안내문 경고와 같음) → 공지가 나오면 초대 후 그 계정으로 접근 확인, 또는 공개 전환 결정
- 지원·제출 페이지 재확인: 시연 영상·GitHub·APK는 **선택 제출, 제출 수준에 따라 가산점**

### 2026-11-09 — 최신 상태 취합(main 직접 push)
- 11-07 밤 이후 새 커밋은 회의 안건 문서(053fac1)뿐. 새 PR: 차슐랭 #37(길민종, 충전 시작 알림 — Claude 리뷰 `❓ 장호연 판단 필요`), 크루즈 #52(길민종, S5 고지 강조 — `✅ 머지 가능`)
- 장호연 11-09 정리: 심사용 계정은 공지 대기, Supabase SQL은 황준용에게 넘김, 영상 촬영 대기, 키 백업은 장호연 직접, 팀 공유 완료 → "다음 할 일" 다시 씀

### 2026-11-07 — 11-09 회의 안건 문서: 충전소 데이터 출처·아파트/집 충전(main 직접 push)
- 장호연 요청("중요한 포인트, 낼모레 회의할 수 있게 문서로"). Claude가 기획서·PRD·서버 migration·앱 코드를 대조해 `docs/meeting-2026-11-09-station-data.md` 작성, `docs/README.md`에 링크
- 핵심: 기획서는 "환경부 API 서버 수집·캐시"라고 적었지만 수집 코드 없음, 충전소는 시드 10곳(`source='SEED'`)뿐. 판정이 150m 거리만 봐서 입주민 전용 아파트 충전기·집 근처 공용 충전소를 구분 못 함(위치 노출·점수 오염)
- 기획서·코드는 고치지 않음. 회의에서 정할 것: A1(문구 정직화)/A2(실데이터 수집), B1~B5 중 구현 범위, 기획서 수정 범위, PT 답변

### 2026-11-07 — 주최 측 문의 메일 최종 점검(두 저장소 + 팀 폴더 원본, main 직접 push)
- 장호연 요청("보내기 전 공식 사이트·FAQ·SDK 문서 전부 다시 검토"). Claude가 Chrome으로 공식 사이트(메인·FAQ 23문항·공지 3건·참가 가이드 6쪽)와 Pleos 문서(SDK Overview, App Review Guidelines 3쪽, TTS 권한·앱 등록)를 읽고 대조
- 12문항 → 10문항: 뺌 = 심사용 계정(공지 대기), 저장소 개인 계정명(FAQ가 개인 계정 저장소 전제), Vehicle Control SDK(Vehicle SDK에 차량 제어 포함). 새로 = Vehicle SDK 퍼미션 사전 협의(심사 가이드 "사전 협의·승인된 경우에만 선언"), 저장소 주소·APK 경로 입력처. 위치기반서비스는 공모전 단계만 묻도록 좁힘. 대응표와 근거 링크는 `docs/organizer-inquiry-email.md`
- 사이트 확인 사항: 접수 마감 **11-15(일)**, 1차 합격 20팀은 PT에서 실제 동작 시연 필수, 수정 제출 제목 `[수정제출] …`
- 남은 것: **장호연이 직접 발송**(팀원 4명 CC)

### 2026-11-07 — 리허설 후속 PR #36 머지(→ b677108) + release APK 재커밋
- 장호연 지시("네, 머지해줘")로 Claude가 진행. 충돌 없음, merge commit. 브랜치 `fix/rehearsal-20261107` 삭제
- 머지된 main에서 `bash tools/build-release.sh` → `apk/SamgakGorae_Chachelin_release.apk` 다시 커밋(시연 데이터 모드, 서명). `aapt`로 아이콘 포함 확인, 에뮬레이터 설치·실행 크래시 0, 독에 새 아이콘 표시
- 남은 것: 촬영 직전 SQL Editor 적용(`server/deploy.md` "11-07 추가"), 적용 뒤 크루즈 #37 닫기, 팀원에게 알릴 것(PR 본문 "담당 경로")

### 2026-11-07 — 리허설 후속 수정(브랜치 `fix/rehearsal-20261107`, 두 저장소 PR)
- 장호연 지시("다 고쳐서 올려줘"). Claude가 작업. **PR 머지는 장호연**, 머지 뒤 두 저장소 `bash tools/build-release.sh` → `apk/` 재커밋 필요(아이콘이 제출 APK에 들어가려면)
- **런처 아이콘(두 앱)**: `res/mipmap-anydpi-v26/ic_launcher(_round).xml`(적응형) + `res/drawable/ic_launcher_foreground.xml`(벡터) + `res/values/colors.xml`(배경 = 대표색), 매니페스트 `android:icon`/`roundIcon`. 차슐랭 = 레드 배경 흰 번개 + 별, 크루즈 = 앰버 배경 위가 끊긴 속도계. PNG 없음(minSdk 28). 에뮬레이터 앱 목록에서 확인
  - 참고: Pleos 독(아래 바 오른쪽 2칸)은 **최근에 쓴 앱** 자리다. 11-07 리허설 때 독의 로봇 아이콘 2개는 우리 앱이 아니라 예전 샘플 앱 `com.example.pleosprobe`·`pleoshello`였음 → 시연 에뮬레이터에서는 두 샘플 앱을 지우는 게 깔끔
- **S4 태그 밀림(차슐랭 `ui/review/ReviewScreen.kt`, 길민종 경로)**: "N점 선택" 줄을 항상 그림(별 전 "별을 눌러 평가해 주세요"). 에뮬레이터에서 별 전후 태그 위치가 같은 것 확인
- **서버(황준용 경로, 장호연 결정)**: `server/supabase/migrations/20261107000100_demo_names.sql`(시드 이름만: 크루즈 구간 → "판교IC 진입로"(#37), st-005~010 → 지명 + "(시연)"), `server/supabase/clear-rehearsal-data.sql`(수동: SIMULATED 기록만 삭제, 설치는 유지). `server/deploy.md` "11-07 추가" 절에 적용 순서. `tests/database.test.mjs`에 2개 추가 → 19개 통과. **원격 적용은 장호연이 SQL Editor로(촬영 직전)**
- 검증: `assembleDebug test :app:lintDebug` 통과, DB 테스트 19/19
- **알릴 것**: 길민종(ReviewScreen), 황준용(server migration·스크립트·테스트), 이상준(매니페스트 아이콘 줄)

### 2026-11-07 — 제출 APK 리허설(에뮬레이터 + 실제 서버, 시나리오 화면 단위 점검)
- 장호연 지시("실제 시연이라 생각하고 화면 단위로 검토"). Claude가 Pleos 에뮬레이터(36.4.10)에 **커밋된 release APK 2개**를 설치해 두 `docs/demo-scenario.md`를 1단계부터 끝까지 진행. 스크린샷: 팀 폴더 `screenshots-2026-11-07/`(ch-*, cb-*)
- 주의: 에뮬레이터에 debug 빌드가 깔려 있으면 release는 서명이 달라 `INSTALL_FAILED_UPDATE_INCOMPATIBLE` → debug를 지우고 설치해야 함
- **결과: 두 시나리오 모든 단계가 문서대로 동작**(크래시·막힘 없음, 실제 서버 읽기·쓰기 정상)
  - 차슐랭: S7 동의 → S1 10곳(약 8초) → S2 → 길안내 팝업 → 내비 → 복귀 → S3(SoC 40→80) → S4(40%→80%·35분·53 kW) → 평가 → S2 점수 3.2→3.3·인증 40→41 → 실패 S3(측정 중) → S5 → S2 7일 실패 5→6 → S6 인증·실패 각 1건
  - 크루즈: S5(동의·알림·차속 권한 창 "위치 정보") → S2(40%·500) → 내비 앞 + 스크립트 → 앱이 내비 위로 올라와 큰 배너 + 음성 → 10초 뒤 내비 복귀 → 90 km/h 해제 기록, 40 km/h 해제 기록 안 함 → 정차 시 앱이 올라와 S3(1회·1곳·1건) → S2. 앱을 앞에 둔 3'도 정상
- **발견한 것(심각한 순)**
  1. 두 앱 런처 아이콘 없음(`aapt`: `icon=''`) — 독에 똑같은 기본 로봇 아이콘 2개
  2. ~~크루즈: 내비가 앞일 때 앱 화면이 음성보다 3~5초 늦게 올라옴~~ → **정정(같은 날)**: 스크린샷 간격(2.5초)과 시각을 잘못 맞춘 것. 시스템 로그로 재측정하니 안내 20:43:57.925 → Pleos 홈 `showTaskView` 57.962 → `onStart` 58.064 → 그리기 준비 58.454, 내비 복귀 10초 뒤. 팝업(앱 올리기)은 설계대로 동작, 문제 아님
  3. 차슐랭 S4: 별을 누르면 "5점 선택" 줄이 생겨 태그 버튼이 아래로 밀림 → 별 직후 태그를 누르면 빗나감(리허설에서 실제로 빗나감). 라벨 자리를 미리 비워 두면 해결(길민종 UI)
  4. 공유 서버에 리허설 기록 누적(위 "다음 할 일" ②)
  5. 차슐랭 S1: 10곳 점수 대부분 3.4·인증 26회로 같아 "점수순"이 안 보임(PRD 5절 "시드가 비슷함"과 같은 문제). 이름도 "(시연)"·"시연 충전소 08"·"가상 충전소"가 섞임
  6. 차슐랭 길안내: 목적지가 2 m(1분)라 장면이 약하고 지도 배경이 흰색 — 영상에서는 0.9 km 충전소로 길안내를 보여 주는 편이 나음(시나리오가 허용)
  7. 크루즈 S2: 구간 이름이 서버 시드 "시연 지점 (고정 위치 앞 약 400m)"(#37). 처음 몇 초는 거리 칸이 "-"(위치 받기 전). 해제 1건을 올려도 해제율 40% 그대로라 "해제 수 +1"이 화면에 안 보임
  8. 차슐랭 최근 실패 시각이 정각(20:34 → 20:00) — 서버 `recent_failure_hour_ms` 설계대로, 시나리오 문구만 고침
- 고친 것(문서): 두 `docs/demo-scenario.md`에 3·8번 메모(차슐랭 8·10단계, 크루즈 기대 시간). 코드는 손대지 않음

### 2026-11-07 — 머지 끝난 원격 브랜치 삭제
- 장호연 지시("네, 삭제해 줘"). Claude가 진행
- `origin/main`에 모두 들어간 것(`git merge-base --is-ancestor`)만 골라 원격 브랜치 27개 삭제. 열린 PR 없음. 이제 원격은 `main`만 남음. 지운 브랜치는 GitHub PR 화면의 "Restore branch"로 되살릴 수 있음

### 2026-11-07 — 장호연 지시("할 수 있는 작업 전부")로 제출 준비 정리(두 저장소, 문서만 main 직접 push)
- **새로 clone 빌드 확인**: 두 저장소를 빈 폴더에 새로 clone(`local.properties` 없이 `ANDROID_HOME`만) → `./gradlew assembleDebug assembleRelease test` 성공. 테스트 실패 0 — 차슐랭 app debug·release 각 99, core 32 / 크루즈 app debug·release 각 83, core 32. 키가 없으면 Fake 저장소로 빌드됨(README "소스에서 빌드" 설명과 같음). `tools/sync-core.sh --check` 일치(core 0.5.2)
- 주최 측 문의 메일에 12번(위치기반서비스사업 신고 필요 여부) 추가, 팀 문서 9절 9번 추가. 두 저장소 `docs/` 사본 + 팀 폴더 원본 `organizer-inquiry-email.md` 맞춤. **발송은 장호연이 직접**
- 팀 문서 2절 담당 경로에 앱 연결 파일(`AppContainer`·`*App`·`RecordingService`·매니페스트 → 차슐랭 이상준 / 크루즈 이민하) 줄 추가, 10절 체크리스트에 APK·core 일치 체크
- `docs/handoff/README.md` 담당자 표에 GitHub 아이디 칸 추가(hoyeoni1·Aodlf123·wklee316·minalee22·giljong)
- 머지 끝난 로컬 브랜치 삭제(`git branch -d`, 머지 안 된 건 지우지 않음). 원격 브랜치는 그대로
- 차슐랭 기획서 DOCX는 지금 `docs/proposal.md`로 다시 만들어도 본문 차이 0(최신 확인)

### 2026-11-07 — 최신 상태 점검, "다음 할 일" 갱신(두 저장소, main 직접 push)
- 장호연 요청("최신 상태 체크 후 남은 작업 보고" → "갱신해 줘"). Claude가 진행, 문서만 바꿈
- 확인: 로컬 main이 2커밋 뒤라 pull. 열린 PR 없음, 열린 이슈 #12·#13, 저장소 PRIVATE, `local.properties` Supabase 키 있음, 서명 키 있음, release APK가 마지막 코드 커밋 뒤라 최신, `docs/proposal.md` 【제출 전 기입】 2곳 남음
- "다음 할 일"을 제출 준비 위주로 다시 씀(끝난 Supabase 키·적용 항목 빼고 키 백업·브랜치 정리·이상준 로딩 상태 추가). 열린 PR 줄도 고침

### 2026-11-05 — README 팀 내부 안내를 `docs/dev-guide.md`로 이동(두 저장소, main 직접 push)
- 배경: 심사위원이 GitHub 링크로 들어오면 README의 팀 내부 안내(담당별 첫 작업·규칙, 팀원 실명 포함)를 먼저 보게 됨 → 장호연 "옮겨 줘"
- 한 일(Claude): README의 `## 시작하기`부터 끝(`코드 구조`·`담당별 첫 작업`·`규칙`)까지를 **`docs/dev-guide.md`**로 그대로 옮기고(링크는 `docs/` 기준으로 고침), README에는 `## 팀 개발 안내` 한 줄 링크만 남김. `docs/README.md` 목차 맨 위에 항목 추가. 크루즈 README도 같음
- 확인: README·PRD·docs에서 README 절을 가리키는 링크 없음(grep). 문서만 바꿈. README에 남은 실명 없음(`docs/handoff/` 파일명·내용의 실명은 그대로 — 접수양식 실명 금지는 양식에만 적용)
- 바꾼 파일: `README.md`, `docs/dev-guide.md`(신설), `docs/README.md`, 이 문서

### 2026-11-05 — README 심사용 안내 추가(두 저장소, main 직접 push)
- 배경: 11-05 제출 요건 재검토(`contest-analysis-todo.md` 3.3)에서 README에 GitHub 가산점 필수 4항목(앱 이름·한 줄 소개, 실행 방법, 테스트 환경·필요 설정, APK 위치) 중 첫 항목만 있고 오픈소스 고지(약관 7조 7항)가 없음을 확인 → 장호연 "써 줘"
- 한 일(Claude): README 맨 위 `## 시작하기` 앞에 **"심사용 안내 (GitHub 제출물)"** 절 — 항목 표(앱 이름·한 줄 소개·APK 위치·패키지·기획서·시연 순서), 실행 방법(에뮬레이터 v2.1.0, `adb install`, 동의 흐름, `pm clear --user 10`, `vconsole.sh`·`demo-charge.sh` 시연, 시연 고지), 테스트 환경·필요 설정(확인 환경, 서버 설정 APK 내장, 권한 목록, DO 미선언, `supabase-public.properties` 복사·Fake 폴백, debug=release 흐름·개발 메뉴는 debug만), 오픈소스 고지 표(AndroidX·Kotlin·Pleos SDK·테스트 라이브러리, 소스 권리는 팀). 그 아래 "팀 내부 개발 안내" 구분선. 기존 내용은 그대로. 크루즈 README도 같은 구성(`demo-drive.sh`, `CAR_SPEED` 부여, DO 선언, TTS)
- 확인: 코드 기준으로 적음 — 개발 메뉴는 `BuildConfig.DEBUG`에서만(`ChachelinNav`·`CruiseBreakerNav`), 권한은 매니페스트, 의존성은 `build.gradle.kts`, APK 경로는 11-05 커밋된 파일. 문서만 바꿈(빌드 영향 없음)
- 남은 결정(장호연): ① README 아래쪽 팀 내부 안내와 `docs/handoff/`에 팀원 실명이 남음(접수양식 실명 금지는 양식에만 적용) — 그대로 둘지, 제출 전 내부 안내를 `docs/dev-guide.md`로 옮길지 ② 비공개 저장소: 심사용 계정이 11-13까지 미공지면 public 전환할지 ③ 숏폼(특별상) 제작 여부 ④ 팀 내 IP·상금 배분 합의 문서(약관 6.3) ⑤ `versionName` 1.0으로 올릴지(지금 0.1, 올리면 release 다시 빌드)
- 바꾼 파일: `README.md`, 이 문서

### 2026-11-05 — 시연 흐름 PR 2개 머지(차슐랭 #35 → 1047949, 크루즈 #47 → 0823124) + release APK 커밋
- 장호연 지시(AskUserQuestion 답 "머지하고 release 커밋")로 Claude가 진행. 두 PR 모두 merge commit, 충돌 없음
- 머지된 main에서 `bash tools/build-release.sh` → `apk/SamgakGorae_<App>_release.apk` 서명 빌드·커밋(시연 데이터 모드 + 시연 판정 기준 + 실제 흐름 시작). 머지 전 브랜치 빌드로 에뮬레이터 시연 확인함(위 항목)
- **알릴 것(장호연 → 팀원)**: 길민종 — `ui/` 변경(`ChachelinNav`·`StationListScreen`·`CruiseBreakerNav`·`InitialScreen`·`ScreenPolicy`·`RiskListScreen`·공통 `CarComponents.onTitleLongPress`), 개발 메뉴는 목록 제목 길게 누르기. 이상준 — `AppContainer.chargeRules`(`DEMO_RULES`)·`StationListViewModel` 위치 타임아웃, 첫 충전 S4 20초 지연 1회 관찰(재현 안 됨) 확인 요청. 황준용 — core 0.5.2 `VehicleSdkSource.subscribeEv` SoC 재조회 한 줄(두 저장소 동일)
- 남은 열린 PR: 없음

### 2026-11-05 — 시연 흐름 확정·실제 흐름 시작 화면(브랜치 `feat/demo-flow-20261105`, 두 저장소)
- 장호연 목표("실제 앱이 시연 가능한 상태로 끊김 없이 동작, 사용자 시나리오를 픽스하고 그대로 쓰면 동작")로 Claude가 작업. 장호연 결정 4가지: ① 시작 화면은 debug·release 모두 실제 흐름(S7→S1), 개발 메뉴는 숨은 진입 ② 제출 release도 시연 판정 기준 ③ 크루즈는 경로 재생 없이 고정 위치 ④ 차슐랭 먼저
- 앱: `ChachelinNav` 루트가 동의 전 S7·동의 후 S1(debug도 같음). 개발 메뉴는 S1 제목 길게 누르기(`ScreenHeader.onTitleLongPress`, 공통 부품이라 크루즈 `CarComponents.kt`도 동일). `BuildConfig.DEMO_RULES`(debug true, release는 `supabase.demo`와 같이) → `ChargeRules.DEMO`. S1 위치 대기 15초 타임아웃 → 오류 + [다시 시도](#12 일부). 화면 스택 `rememberSaveable`(액티비티 재생성에도 유지)
- core 0.5.2(황준용 경로, 장호연 결정으로 수정 → 황준용에게 알릴 것): `VehicleSdkSource.subscribeEv`에서 리스너 등록 뒤 SoC를 한 번 더 조회. 앱 시작 직후 스크립트가 넣은 SoC 40을 놓쳐 시작 SoC가 80으로 잡히던 문제(11-04 기록) 방지. `sync-core.sh`로 크루즈에 동기화
- 문서: **`docs/demo-scenario.md` 신설(확정 시연 순서·기대 시간·복구표)**, PRD 5.3·7절, README, `tools/demo-charge.sh`·`demo-drive.sh` 머리 주석
- 확인(에뮬레이터, 실제 서버 시연 데이터 모드, 앱 데이터 지우고 처음부터): S7 동의 → S1 10곳 → S2 시흥동 주민센터 → [길안내] 내비 경로 → 앱 복귀 → `demo-charge.sh success` → S3 자동 → S4 자동(닫은 뒤 1~2초) → 별 5·태그 2·[평가 보내기] → S2 인증 +1·5점 +1 → `fail` → S5 → [닫기] → S2 7일 실패 +1 → S6 인증·실패 줄. 제목 길게 누르기 → 개발 메뉴 → [뒤로] → S1. `assembleDebug test` 통과(app 99, core 32/18)
- **1회 관찰·재현 안 됨**: 설치 직후 첫 충전에서 충전구를 닫은 뒤 S4가 약 20초 늦게 뜨고 그동안 S2 아래 [충전 결과 확인하기]만 보임. 이후 4회는 1~2초. 복구는 그 버튼. 원인 미확인(이상준 확인 요청: `ChargeSessionTracker.resolveStation` 첫 조회 실패 → 5초 재시도 가능성)
- 담당 경로 참고: `ui/`(길민종) `StationListScreen`·`CarComponents`·`ChachelinNav`, 로직(이상준) `AppContainer`·`StationListViewModel`, `core/`(황준용), `app/build.gradle.kts`를 장호연 결정으로 고침. PR 머지 뒤 세 사람에게 알릴 것
- 추가 확인(11-05 오후): 앱 데이터를 지우고 길안내 왕복까지 같은 경로로 2회 더 돌려도 S4는 1초 안에 뜸(위 20초 지연은 재현 안 됨). `tools/build-release.sh`로 서명 release APK를 만들어 에뮬레이터에 설치(기존 debug는 `adb uninstall` 뒤) → S7→S1→충전→S4까지 같은 흐름, 제목 길게 눌러도 개발 메뉴 안 열림, 크래시 0. `apk/`는 머지 뒤 main에서 다시 빌드해 커밋할 것(지금은 로컬에만 있음)
- 남은 것: PR 머지(장호연) → `bash tools/build-release.sh`로 release 재빌드(판정 기준이 바뀜) → `apk/` 커밋. 아이콘 2종(독에서 두 앱 구분 불가, 11-04 검토 #8)

### 2026-11-05 — 길민종 Pleos 검수 후속 PR 2개 머지(차슐랭 #34 → bdfaa3b, 크루즈 #46 → 3105a8d)
- 장호연 지시로 Claude가 진행. 두 PR 모두 base가 main이라 그대로 merge commit으로 머지(충돌 없음)
- 머지 전 리뷰: 00:13 자동 리뷰 `✅` + 09:40 재검토 `✅`. PR 브랜치 `assembleDebug test --rerun-tasks` 통과(차슐랭 app 99·core 32/18, 크루즈 app 82·core 32/18, 실패 0). 두 PR의 `CarAlertDialog` `widthIn(min = 64.dp)`가 같은 내용이라 함께 머지해 두 앱 `CarComponents.kt` 동일 유지
- 내용: 차슐랭 목록 스크롤 보존(`SaveableStateProvider`, 철회 시 `removeState`), 충전·실패 헤더 정렬. 크루즈 선택 구간·스크롤 보존, debug 정차 S1 미리보기 헤더(`canShowDrivingPreviewBack`, 주행·UX 미확인 시 꺼짐, 테스트 추가), 자동 기록 설명 복원
- 후속(타 담당): 이상준 — 내 충전 기록 최초 로딩에 빈 상태가 먼저 보임(`HistoryUiState`에 loading 계약 없음). 황준용 — 크루즈 #37 서버 시드 이름 migration
- 남은 열린 PR: 없음

### 2026-11-04 — 길민종 대표색 PR 2개 머지(차슐랭 #33 → 683a22f, 크루즈 #45 → 633996d)
- 장호연 지시로 Claude가 진행. 두 PR base를 `fix/car-header-back-20261104`(이미 머지됨)에서 `main`으로 바꾼 뒤(변경 파일 7개·5개 그대로) merge commit으로 머지
- 머지 전 정기 리뷰: PR 브랜치 `assembleDebug test` 통과(차슐랭 242개·크루즈 204개, 실패 0), 필수 항목 통과, `✅ 머지 가능` 코멘트. 머지된 main에서 두 저장소 core 동일, 공통 UI 파일은 Primary 값·주석만 다름
- 후속(선택, 길민종): 차슐랭 S1 카드 "7일 실패" 0회 숨김이 PRD 124·59행과 다름(기획 판단), S4 "인증 충전 완료" 카드·확인 팝업 주 버튼의 레드. 크루즈 S1 주행 화면의 "크루즈 작동"·"안내할 구간 없음"·"차량 연결됨"이 Warning 주황과 거의 같은 앰버(흰색/회색으로 바꾸거나 Warning을 더 붉게)
- 남은 열린 PR: 없음

### 2026-11-04 — 대표색 PR 수정 요청(차슐랭 #33, 크루즈 #45)
- 장호연 결정: 길민종이 색 의미 문제를 이 PR에서 고친 뒤 머지. PR에 @giljong 요청 코멘트: 차슐랭 S4 인증 카드·S5 [닫기]를 레드 말고 흰색·회색, 크루즈 설정 "동의 철회"를 토글 앰버와 구분, S1 "차량 연결됨" 점 회색
- base 변경(`fix/car-header-back-20261104` → main)은 에이전트 권한 확인에서 막혀 못 함 → 길민종에게 요청. **머지 전에 base가 main인지 꼭 확인**
- 다음: 길민종 수정·재리뷰 요청 오면 재검토

### 2026-11-04 — 길민종 대표색 PR 검토(차슐랭 #33 레드, 크루즈 #45 앰버)
- 한 일: main에 합쳐 `assembleDebug assembleRelease test lint` 통과(248개·212개, 실패 0). 서버 연결 debug로 두 앱 주요 화면 캡처. 코드는 머지 가능, `🟡` 코멘트
- **머지 전 필수**: 두 PR base가 아직 `fix/car-header-back-20261104`(이미 머지됨) → base를 main으로 바꾼 뒤 머지(충돌 없음)
- 색 의미 확인 요청: 차슐랭 S4 "인증 충전 완료" 카드가 레드라 오류처럼 보임, S5 실패 팝업 [닫기]가 레드. 크루즈 설정 "동의 철회"가 켜진 토글과 같은 앰버라 구분 안 됨, S1 "차량 연결됨" 점 앰버. 이 PR에 넣을지 후속으로 할지는 장호연 판단
- 참고: 서버 연결 빌드의 크루즈 S2 구간 이름은 아직 옛 "시연 지점…"(서버 이름 migration #37 대기)

### 2026-11-04 — 길민종 PR 4개 머지(줄바꿈 차슐랭 #31 → d55423d·크루즈 #43 → ceaf56e, 헤더 뒤로 차슐랭 #32 → 0cb7d7b·크루즈 #44 → 554e9ee)
- 장호연 지시로 줄바꿈 PR 먼저 머지. 헤더 뒤로 브랜치에 main을 합쳐 `gil-minjong.md` 충돌 정리(현재 상태는 뒤로 PR 쪽, 작업 기록 양쪽 유지, dd6519e·f016f78) 후 머지
- 머지된 main: `assembleDebug assembleRelease test lint` 통과(248개·212개, 실패 0), core·tools 동일, 충돌 표시 없음
- 남은 열린 PR: 길민종 대표색(차슐랭 #33, 크루즈 #45) 검토 전

### 2026-11-04 — 길민종 PR 4개 검토(줄바꿈 차슐랭 #31·크루즈 #43, 헤더 뒤로 차슐랭 #32·크루즈 #44)
- 한 일: 두 PR을 함께 main에 합쳐 `assembleDebug assembleRelease test lint` 통과(248개·212개, 실패 0), core·tools 동일. 에뮬레이터: 두 앱 모든 정차 화면 헤더 [뒤로] → 이전 화면, 루트에서 [뒤로] → 앱 닫힘. 네 PR에 `✅ 머지 가능` 코멘트
- 머지 순서: 줄바꿈 PR 먼저 → 헤더 뒤로 PR은 `gil-minjong.md`만 충돌(현재 상태는 뒤로 PR 쪽, 작업 기록 양쪽 유지)
- 참고: 크루즈 S3 요약의 뒤로는 기존 설계대로 S2 목록. 에뮬레이터 주행 상태가 IDLE로 남으면 S1만 보임(기어 D→P)
- 다음: 장호연 머지 지시 대기

### 2026-11-04 — 시작 크래시 방어 PR 머지(차슐랭 #30 `0aa4032`, 크루즈 #42 `9ff1318`)
- 한 일(Claude 에이전트, 장호연 "리뷰 후 머지" 지시): 두 PR 모두 Claude 리뷰 `✅ 머지 가능`(ed8dee7·d7821af) 확인. 둘 다 `docs/handoff/jang-hoyeon.md` "작업 기록"에서만 main과 충돌 → 두 항목 모두 남기는 쪽으로 origin/main을 브랜치에 합쳐 push. 합친 브랜치에서 `assembleDebug test` 통과(차슐랭 246개, 크루즈 210개, 실패 0), `sync-core.sh --check` 일치(core 0.5.2). 차슐랭 #30 → 크루즈 #42 순서로 merge commit
- 담당자: 크루즈 #42는 이민하가 `AppContainer`·`RiskAnnouncer` 변경에 동의 코멘트, `docs/disengage-rules.md` 문구도 main에 반영(1ee2539). core(황준용)·차슐랭 `AppContainer`(이상준)는 확인 코멘트 없이 팀장 결정으로 머지 → 알릴 것
- 리뷰의 선택 제안(안 고침, 필요하면 차슐랭 core에서): `EncryptedBackendStore.read()`가 일시적 I/O 오류도 손상으로 봄 → `IOException`은 다시 던지거나 원본을 `.corrupt`로 보관; `status` 첫 값 `local-storage-reset`은 워커가 돌면 바로 덮임
- 확인: 머지 뒤 두 main(`0aa4032`·`9ff1318`) `assembleDebug test` 통과(차슐랭 246개, 크루즈 210개, 실패 0), `sync-core.sh --check` 일치
- 다음: 키가 들어왔으니 `bash tools/build-release.sh`(두 저장소) 후 `apk/` 커밋, 서버 연결 상태 전체 리허설. 11-04 검토 보고서 2장 나머지(#1 주최 측 문의 메일, #4 시연 빌드 전략, #5 크루즈 ScreenPolicy null, #7 차슐랭 S1 위치 타임아웃 #12, #8 아이콘·allowBackup)

### 2026-11-04 — core v0.5.2 시작 크래시 방어 PR 검토(차슐랭 #30, 크루즈 #42)
- 검토 중 22:35에 장호연 계정으로 두 PR이 머지됨(0aa4032, 9ff1318). 머지된 main의 코드는 Claude가 검토·시험한 것과 같음(문서만 다름)
- 확인: main에 합쳐 `assembleDebug assembleRelease test lint` 통과(246개·210개, 실패 0), core·tools 두 저장소 동일. 에뮬레이터: 구형 형식 키로 빌드 → 앱이 죽지 않고 `SupabaseConfig` 로그(키 값 없음), 정상 키로 되돌리면 정상. 크루즈 `demo-drive.sh` 40초 간격 두 번 → 두 번 다 안내 음성
- 참고 의견(PR 코멘트): 복호화 실패는 `read()`가 null을 돌려 `status`가 `local-storage-reset`이 아니라 `idle`로 시작(경고 로그로만 보임). 상태 파일 손상 시 동의 철회 삭제 대기도 사라질 수 있음 → 황준용·이상준 인지 필요
- 이 PC `local.properties`는 정상 키로 되돌려 둠(바꿨다 되돌린 것 확인)

### 2026-11-04 — 시작 크래시 방어(core v0.5.2), 브랜치 `fix/core-startup-resilience`
- 배경: 11-04 두 작품 전체 검토(`hyundai-app/project-review-2026-11-04.md`)에서 공통 core에 "켜자마자 죽는" 경로 2개 확인 → 장호연 "수정 시작" 지시
- 한 일(core, 원본 차슐랭 → `tools/sync-core.sh ../CruiseBreaker` 동기화, `--check` 일치):
  - `BackendConfig.fromBuildConfigOrNull(...)`: URL 비면 null(Fake), 형식 오류(http·구형 anon JWT·공백·따옴표)면 `onInvalid`에 이유(키 값 제외)만 알리고 null. `require` 메시지도 붙임. 앞뒤 공백·줄바꿈 trim
  - `EncryptedBackendStore.read()`: 형식 오류·복호화 실패를 예외로 올리지 않고 `Log.w` 뒤 null → 빈 상태로 시작, 다음 write가 덮어씀
  - `SupabaseBackend`: `loadState()`가 읽기 예외·JSON 오류면 빈 상태로 시작하고 `status` 첫 값을 `local-storage-reset`으로(보내지 못한 작업·철회 대기는 그 경우 사라짐 → 로그에서 보임)
  - 테스트 2개 추가(`SupabaseBackendTest`): 읽기 예외·깨진 JSON·정상 각각의 첫 status, 잘못된 설정 4종이 null이 되고 메시지에 키가 안 새는지
  - `app/.../AppContainer.kt`(이상준 담당, 장호연 승인): `BackendConfig(...)` 직접 생성 → 팩토리 + `Log.e("SupabaseConfig", …)`
  - `tools/build-release.sh`: `supabase.url`이 `https://`, `publishableKey`가 `sb_publishable_`로 시작하는지 빌드 전에 검사(틀리면 중단)
  - `core/VERSION` 0.5.2, `core/README.md` 변경 기록
- 결정한 것과 이유: 설정·상태 오류는 "죽지 않고 서버 없이 뜬다"로 통일. 심사위원 에뮬레이터에서 크래시가 가장 나쁜 결과이고, 어차피 복구 불가능한 상태이기 때문. 로그(`SupabaseConfig`, `SupabaseSync`=`local-storage-reset`, `EncryptedBackendStore`)로 원인은 남김
- 테스트 결과: `./gradlew :core:testDebugUnitTest :app:testDebugUnitTest --offline` 통과 — debug 124개(core 32 = 30 + 새 2, app 92), 실패 0. `bash -n tools/build-release.sh` 통과. 에뮬레이터 실행은 아직 — 서버 키가 들어오면 서버 모드 리허설 때 함께 확인
- 바꾼 파일: `core/src/main/java/com/hyundaiapp/core/backend/{BackendTransport,EncryptedBackendStore,SupabaseBackend}.kt`, `core/src/test/.../SupabaseBackendTest.kt`, `core/VERSION`, `core/README.md`, `app/src/main/java/com/hyundaiapp/chachelin/AppContainer.kt`, `tools/build-release.sh`, 이 문서

### 2026-11-04 — 텍스트 후속(차슐랭 #28, 크루즈 #40)·Supabase 공개 설정(차슐랭 #29, 크루즈 #41) 머지
- 머지: 크루즈 #40 → 32ad976, 차슐랭 #29 → a874fca, 크루즈 #41 → ff29863. 차슐랭 #28은 GitHub가 PR head를 옛 커밋(a0d13d5)으로 잡아 머지 API가 계속 거절해서, 검토한 브랜치 끝(97d55cb)을 로컬에서 merge commit(9ea62b4)으로 main에 push → PR MERGED 처리됨
- 황준용 #29/#41 검토: `supabase-public.properties`는 `sb_publishable_` 공개 키(Secret 아님). 이 PC 두 저장소 `local.properties`에 두 줄 추가(Git 제외 확인). debug 빌드로 차슐랭 S1 → `SupabaseSync: connected`, 서버 시연 데이터 표시. 대왕판교IC 휴게소 2.7점·7일 실패 5회로 맨 아래 → **11-03 migration 2개 실제 서버 적용 확인**
- 최종 main(키 포함)에서 `assembleDebug assembleRelease test lint` 통과(242개·204개, 실패 0), `tools/` 동일
- 남은 것: 키가 들어왔으니 `bash tools/build-release.sh`(두 저장소) 후 `apk/` 커밋, 서버 연결 상태 전체 리허설(크루즈 포함). 황준용: 시드 구간 이름 새 migration(크루즈 #37), 본인 handoff, Supabase 리전·요청 로그 답(약관 【확인 필요】 2곳). 앱 막 켠 직후 충전 시연 시작 SoC 문제(이상준 확인 필요)

### 2026-11-04 — 길민종 텍스트·이모지 후속 PR 검토(차슐랭 #28, 크루즈 #40)
- 한 일: 두 PR을 main에 합쳐 `assembleDebug assembleRelease test lint` 통과(242개·204개, 실패 0), 충돌 없음. 에뮬레이터에서 S3 세로 가운데 배치·S4 전환, 약관 전문 줄바꿈 확인. 두 PR에 `✅ 머지 가능` 코멘트
- 참고 의견(막지 않음): 이제 한글끼리만 묶어 `ID만`·`32분`처럼 숫자·영문+한글 단어가 줄 끝에서 갈릴 수 있음 → 한글과 글자·숫자 사이도 묶도록 넓히기 제안
- **따로 발견(이 PR과 무관)**: 앱을 막 설치·실행한 직후 `demo-charge.sh success`를 돌리면 충전 시작 SoC를 이전 값(80%)으로 잡아 변화 0 → 충전 끝나도 S4/S5가 안 뜨고 개발 메뉴로 돌아감. 앱이 떠 있는 상태로 다시 돌리면 40→80 정상. 시연 영상 찍을 때는 앱을 켜고 몇 초 뒤에 스크립트 실행. 원인(기록 서비스 시작 시 SoC 초기값)은 이상준 담당 쪽에서 확인 필요
- 다음: 장호연 머지 지시 대기

### 2026-11-04 — demo-drive.sh 시드 이름 PR 머지(차슐랭 #27 → 68460a1, 크루즈 #39 → 8f02c88)
- 장호연 지시로 머지. GitHub 502·"merge already in progress"로 한 번 걸려 REST로 다시 머지함. 두 저장소 `tools/` 동일
- 확인(11-04): Supabase 키는 이 PC `local.properties`에 아직 없음(release 서명 값만). 황준용 새 서버 코드·커밋 없음(마지막 활동 11-03). 열린 PR 없음

### 2026-11-04 — demo-drive.sh 시드 이름 맞춤(브랜치 `fix/demo-drive-seed-name`)
- `tools/demo-drive.sh` 14행 설명의 시드 이름을 "시연 지점" → "판교IC 진입로"로(크루즈 #38과 맞춤). 주석 한 줄만, 동작 변경 없음. 두 저장소 `tools/` 동일 확인, `bash -n` 통과
- 남은 것: 서버 시드 이름 새 migration(황준용) 뒤 크루즈 #37 닫기

### 2026-11-04 — 길민종 화면 다듬기 PR 머지(차슐랭 #26 → 85083e7, 크루즈 #38 → 3112f02)
- 장호연 지시로 merge commit. 머지 결과는 PR 브랜치와 문서만 달라 검토 때 빌드·테스트(230개·192개)·에뮬레이터 확인이 그대로 유효
- 차슐랭 #25 자동으로 닫힘. 크루즈 #37은 열어 둠: 황준용 서버 시드 이름 새 migration, 장호연 `tools/demo-drive.sh` 14행 이름 맞추기(두 저장소)
- 열린 PR 없음

### 2026-11-04 — 길민종 화면 다듬기 PR 검토(차슐랭 #26, 크루즈 #38)
- 한 일: 두 PR을 지금 main에 합쳐 `assembleDebug assembleRelease test lint` 통과(테스트 230개·192개, 실패 0), 충돌 없음, `tools/` 동일. 에뮬레이터에서 `demo-charge.sh success`로 S3·S4 확인: S3 카드 안 빈칸 사라짐, S4 별이 열을 채우고 선택 시 주황 별·"N점 선택"·[평가 보내기] 켜짐. 두 PR에 `✅ 머지 가능` 코멘트
- 참고 의견(막지 않음): S3는 카드 아래 화면 절반이 비어 보임 → 영상에서 어색하면 가운데 정렬·SoC 숫자 키우기 검토
- 다음: 장호연 머지 지시 후 머지. #38 머지 뒤 `tools/demo-drive.sh` 14행 이름을 "판교IC 진입로"로(차슐랭 원본 → 크루즈 동기화). 서버 시드 이름은 황준용 새 migration(#37은 열어 둠)

### 2026-11-04 — 길민종 UI PR 4개 머지
- 장호연 "나머지 진행" 지시. Claude 에이전트가 리뷰한 head 고정으로 merge: 팝업 구분선 차슐랭 #22(`efb0ef8`)·크루즈 #34(`7fc8971`) 먼저 → 글자 단계 통일 #23(`a5d9144`)·#35(`7d5fbf3`)
- #23·#35는 `docs/handoff/gil-minjong.md`만 충돌 → `feat/ui-polish`에 main을 merge해 정리(현재 상태는 ui-polish 쪽, 작업 기록은 양쪽 모두 유지, `2fc2940`·`b0d3bad`) 뒤 머지
- 확인: 머지된 main 두 앱 `assembleDebug assembleRelease test lint` 통과(테스트 230, 192), `tools/` 같음, 아래 바 수정 유지(S4 버튼 y 1230). 에뮬레이터에 main debug 설치함
- 열린 PR 없음. 남은 화면 개선은 길민종 이슈 차슐랭 #25·크루즈 #37
- 바꾼 파일: 이 문서

### 2026-11-04 — 화면 검토, 개선 요청 이슈(차슐랭 #25, 크루즈 #37)
- 장호연 지시("화면 개선할 거 검토", "길민종에게 1~4 요청"). Claude 에이전트가 길민종 UI PR 4개 적용본을 에뮬레이터에 설치해 전 화면 확인: 터치 64dp 이상·글자 잘림 없음
- 요청(담당 길민종, assign): 차슐랭 #25 = S3 충전 중 카드 늘어남·빈칸, S4 별점 크게. 크루즈 #37 = S3 0·0·0 요약 상태 문구, 시드 구간 이름 "시연 지점(…)" 바꾸기(서버 새 migration은 황준용과)
- 시연 메모: 크루즈는 P를 받아야 정차 화면이 열림 → 시연 중 `tools/vconsole.sh up` 유지. 꺼진 뒤 다시 열면 P여도 주행 화면에 머묾(기어를 다시 P로 보내면 풀림)
- 에뮬레이터에는 지금 UI PR 적용본 debug가 설치돼 있음(저장소는 main)
- 바꾼 파일: 이 문서

### 2026-11-04 — 아래 바 수정 머지(차슐랭 #24, 크루즈 #36), 실제 시연 흐름 재확인
- 장호연 "Yes, merge both" 지시. Claude 에이전트가 head 고정 merge: 차슐랭 `742caa3`, 크루즈 `6e527f7`. 머지된 main 두 앱 `assembleDebug assembleRelease test lint` 통과
- 에뮬레이터(Fake)에서 머지된 main debug로 처음부터: S7·S5 동의 → [시작하기](이제 눌림) → 차슐랭 `demo-charge.sh success` → S4 별 5개·태그 → [평가 보내기] 성공 → `fail` → S5 실패 화면, 크루즈 `demo-drive.sh` → 안내 배너 → 주행 요약 1회·1곳. 스크린샷은 로컬 `screenshots-2026-11-04/`
- 알게 된 것: (1) 에뮬레이터를 새로 켜면 앱 동의 상태가 없을 수 있음 → 시연 전 개발 메뉴 S7/S5에서 동의·[시작하기] 먼저(안 하면 기록 서비스가 안 돌아 충전이 기록되지 않음). (2) `tools/vconsole.sh up`을 파이프(`| tail`)로 부르면 하트비트 때문에 끝나지 않음. (3) Git Bash에서 `MSYS_NO_PATHCONV=1`을 켠 채 시연 스크립트를 돌리면 curl `-o /dev/null`이 실패(exit 23)
- 남은 것: 길민종 UI PR 차슐랭 #22·#23, 크루즈 #34·#35 머지 결정(#22·#34 먼저)
- 바꾼 파일: 이 문서

### 2026-11-04 — 길민종 UI PR 4개 검토, 아래 시스템 바 버그 수정 PR
- 장호연 지시("지금은 어때"). Claude 에이전트가 작업
- 길민종 PR 차슐랭 #22(팝업 구분선)·#23(글자 단계 통일), 크루즈 #34·#35: 둘씩 main에 합쳐 빌드·테스트·lint 통과(230, 192), 에뮬레이터 화면 확인 → ✅ 머지 가능 코멘트. **머지 순서: #22·#34 먼저**, #23·#35는 `docs/handoff/gil-minjong.md`만 충돌하니 길민종이 main merge로 정리
- **발견(기존 버그, 시연 막힘)**: Pleos 아래 시스템 바(y 1300~1440)가 앱 창 아래를 덮어 맨 아래 고정 버튼이 안 보이고 안 눌림 — 차슐랭 S4 [평가 건너뛰기]·[완료], S7 [시작하기], S2 [길안내], 크루즈 S5 [시작하기], S3 버튼. inset은 오지 않고 `screenHeightDp`(1210dp)만 바를 뺀 값
- 수정 PR: 차슐랭 #24, 크루즈 #36(`fix/bottom-bar-inset`, `MainActivity` 루트 높이를 `screenHeightDp`로 제한). 버튼 y 1370 → 1230 확인, 빌드·테스트·lint 통과. **장호연 머지 결정 대기**
- 바꾼 파일: 이 문서(코드는 PR 브랜치)

### 2026-11-04 — 길민종 약관 검수 PR 머지, #14 닫음
- 장호연 "Yes, do all three" 지시. Claude 에이전트가 head 고정으로 merge commit: 차슐랭 #21(`675c167`), 크루즈 #33(`2dfe5e7`). 둘 다 `docs/handoff/gil-minjong.md`만 변경(약관 화면 검수 통과 기록)
- 차슐랭 #14 닫음(D01 유지 결정, D02 #19, B01 #19·#32, 화면 검수 통과)
- 남은 열린 이슈: 차슐랭 #12·#13(UI 감사 개선, 제출 필수 아님)뿐. 열린 PR 없음
- 바꾼 파일: 이 문서

### 2026-11-04 — 해결된 이슈 3개 닫음
- 장호연 지시("그거 닫아줘"). Claude 에이전트가 근거 코멘트와 함께 닫음: 차슐랭 #8(#10으로 해결), 크루즈 #20(#22로 해결), 크루즈 #25(이민하가 정한 닫는 조건 충족: #31 S5 안내, #32 약관 책임 제한)
- 남은 열린 이슈: 차슐랭 #14(길민종 화면 검수만 남음), #12·#13(UI 감사 개선, 제출 필수 아님)
- 바꾼 파일: 이 문서

### 2026-11-04 — 이상준 PR 차슐랭 #20 머지
- 장호연 "Yes, merge #20" 지시. Claude 에이전트가 리뷰한 head(`a999bfd`) 고정으로 merge commit `86d04f3`(충전소 조회 실패 재조회 + 결과 알림)
- 확인: 머지된 main에서 `assembleDebug assembleRelease test lint` 통과(테스트 184, 실패 0)
- 후속(이상준): 위치를 끝내 못 받는 세션의 재시도 끝 정하기, 서버 키 생기면 Wi-Fi 끈 충전 리허설
- 바꾼 파일: 이 문서

### 2026-11-04 — 최신 pull 뒤 재검토
- 장호연 지시("최신 pull 받아서 한번 더 검토"). Claude 에이전트가 작업
- 두 저장소 main은 머지 뒤 새 커밋 없음(차슐랭 `64a6327`, 크루즈 `82f2f09`). 새 것은 이상준 차슐랭 PR #20(충전소 조회 실패 재조회 + 결과 알림)뿐
- PR #20: 지금 main과 합쳐도 충돌 없음, `assembleDebug assembleRelease test lint` 통과(테스트 184, 실패 0) → ✅ 머지 가능 재확인 코멘트(https://github.com/hoyeoni1/hyundaiapp-chachelin/pull/20#issuecomment-6622771682). 후속: 위치를 끝내 못 받는 세션의 재시도 끝 정하기, 서버 키 생기면 오프라인 충전 리허설
- **남은 것(장호연)**: PR #20 머지 결정. 이슈 닫기(크루즈 #25·#20, 차슐랭 #14·#8)
- 바꾼 파일: 이 문서

### 2026-11-04 — PR 차슐랭 #19·크루즈 #32 머지
- 장호연 "Yes, merge both" 지시. Claude 에이전트가 리뷰한 head(`f0f8b2e`, `2a60341`) 고정으로 merge commit: 차슐랭 `f876a69`, 크루즈 `547fdc2`. 차슐랭 #17 자동으로 닫힘
- 확인: 머지된 main에서 두 앱 `assembleDebug assembleRelease test lint` 통과, `tools/` 두 저장소 같음
- 못 한 것: 크루즈 #25 닫기(에이전트 동작이 권한 확인에서 막힘) → 장호연이 닫기
- 바꾼 파일: 이 문서

### 2026-11-03 — 시연 가능 상태 마무리(PR 차슐랭 #19, 크루즈 #32)
- 장호연 지시("할 수 있는 거 다 해 줘, 두 앱이 구동·운영·시연 가능한 상태"). Claude 에이전트가 작업
- 한 일: 약관 전문 앱 연결(`ui/TermsText.kt`), 리뷰 태그 쌍 배타(#17, 차슐랭), 크루즈 S5 문구, `simulation_time_scale` 버그 수정(차슐랭), release 서명 설정 + `tools/build-release.sh`
- **서명 키 생성**: 저장소 밖 `C:/Users/H/.samgakgorae/samgakgorae-release.jks`(비밀번호는 같은 폴더 README.txt, 두 저장소 `local.properties`의 `release.*`에도 들어감). **이 폴더를 백업할 것**
- 확인: 두 앱 `assembleDebug test lint` 통과. 에뮬레이터(Fake)에서 `demo-charge.sh success/fail`, `demo-drive.sh` 전 장면 통과, 약관 창·태그 규칙 화면 확인, 서명된 release APK 설치·실행(크래시 0)
- 남은 것: PR #19·#32 머지 → 황준용에게 Supabase 키·migration 받기 → 서버 연결 리허설 → `bash tools/build-release.sh`로 제출 APK 만들어 커밋
- 바꾼 파일: 이 문서(코드는 PR)

### 2026-11-03 — 약관(B01) 값 확정
- 장호연 결정("메일만, 나머지 추천대로"): 시행일 11-14, 판 번호 유지, 서비스 종료 30일 안 삭제, 철회 표시는 서비스 종료 때 삭제, 보호책임자 장호연, 연락처 hoyhoyhoyeoni@gmail.com(전화번호는 공개되므로 안 넣음). 크루즈: 28일 지난 기록 따로 안 지움, 책임 제한에 고의·중과실 예외, S5 문구 "기록에는 좌표 대신 약 150m 구간 번호만 저장하고"로 변경(길민종)
- 한 일: 두 저장소 `docs/terms-draft.md`를 확정본으로(【확정 필요】 0개). 남은 【확인 필요】는 Supabase 리전·요청 로그(황준용). #14에 길민종 연결 요청
- 바꾼 파일: `docs/terms-draft.md`, 이 문서

### 2026-11-03 — Supabase PR #10·#22 머지, 최종 점검
- 한 일(Claude 에이전트와 함께, 장호연 "Yes, merge both", "전체 프로젝트 확인해서 최종보고"): #10(`fc9b2e7`)·#22(`eb06822`) 머지(GitHub 502 한 번 뒤 head 재확인 후 머지). 차슐랭 #9·크루즈 #12 자동 종료
- 확인: 두 main `assembleDebug assembleRelease test lint` 통과(차슐랭 208, 크루즈 192), core·tools 두 저장소 동일. 열린 PR 0건
- 확인: #8(철회 삭제 재시도)·#20(철회 뒤 늦은 업로드)은 머지된 core·서버로 해결(근거 테스트 통과). 닫기는 자동 승인에서 막혀 장호연이 닫을 것
- 발견: 두 저장소 PRIVATE, `apk/` 폴더·release 서명 설정 없음, 이 PC `local.properties`에 Supabase 키 없음 → "다음 할 일"에 반영
- 바꾼 파일: 이 문서

### 2026-11-03 — Supabase PR #10·#22 남은 수정 직접 반영(황준용 브랜치)
- 장호연 결정("다 진행해줘", 브랜치는 황준용 PR에 바로, DB 적용은 장호연이 대시보드에서). Claude 에이전트가 작업
- 반영: release 시연 데이터 모드 기본(`supabase.demo`), 점수 공식 migration `20261103000100`, 실패 잦은 시연 충전소 st-004 migration `20261103000200`, refresh SQL 갱신, `consent_version` = 약관 초안 판 번호, #10 README 충돌 해결(main merge)
- 확인: PGlite 17개, 두 앱 `assembleDebug assembleRelease test lint` 통과(208, 192). 전체 lint 통과 = #9는 #10 머지로 해결
- **남은 것(장호연)**: #10·#22 머지 → 대시보드에서 두 migration 적용(`server/deploy.md` "11-03 추가 migration", verify 기대 charge_sessions 325) → 시연 리허설
- 바꾼 파일(이 문서 외는 PR 브랜치): `app/build.gradle.kts`, `AppContainer.kt`, `core/.../BackendTransport.kt`·`SupabaseBackend.kt`·테스트, `server/` migration 2개·refresh·테스트·문서, `docs/server-overview.md`

### 2026-11-03 — 길민종 PR 머지(차슐랭 #18, 크루즈 #31)
- 한 일(Claude 에이전트와 함께, 장호연 "Yes, merge both" 지시): 차슐랭 #18(길민종 인수인계만, `8b697d1`), 크루즈 #31(S5 보조 안내, `abee7f2`) 머지. 크루즈 main `assembleDebug test :app:lintDebug` 통과
- 크루즈 #25: S5 안내는 반영됨. 책임 제한 문구는 약관 초안 4절에 있음 → 약관 확정 뒤 닫기
- 바꾼 파일: 이 문서

### 2026-11-03 — B01 약관 전문 초안, 최신 PR·이슈 점검
- 한 일(Claude 에이전트와 함께, 장호연 "초안 써 줘" 지시):
  - `docs/terms-draft.md` 두 앱 약관 초안(승인 전). 코드와 서버 PR #10 `d9fbcf6`·#22 `da25446`의 실제 처리에 맞춤: 설치마다 무작위 UUID의 해시만 서버로, 좌표 대신 충전소 ID·150m 격자 번호, 충전소 밖 충전은 기기에만, 철회 시 즉시 삭제, 철회 표시만 서버에 남음, TTS 기기 내. 크루즈는 #25 책임 제한 문구 포함
  - 확정 필요: 판 번호·시행일·보유 기간·연락처·보호책임자·책임 제한 표현. 확인 필요: Supabase 리전(국외 이전 고지), 요청 로그의 위치 보관, 위치기반서비스 신고 여부(주최 측 문의 권장)
  - 발견: 서버 `consent_version`이 `development-v1` 고정(황준용), 크루즈 S5 "정확한 좌표 대신 구간 번호를 보내고"는 근처 조회가 좌표를 보내는 것과 다름(길민종)
  - 점검: Supabase PR #10·#22는 06:20 이후 새 커밋 없음 — 시연 모드 release 설정·점수 공식·실패 잦은 시드 미반영, #10은 README 충돌. 길민종 새 PR 차슐랭 #18(인수인계만), 크루즈 #31(S5 보조 안내, 빌드·테스트 166개·app lint 통과) 리뷰
- 고침: 이 문서의 11-03 작업 기록들이 줄바꿈 대신 "역슬래시 n" 글자로 한 줄씩 들어가 있던 것을 복구(내용은 그대로)
- 바꾼 파일: `docs/terms-draft.md`, 이 문서

### 2026-11-03 — 이슈 #14 D01·D02 결정
- 장호연 결정("Yes, both as proposed", 길민종 최소안): **D01** 동의는 각 [동의]에서 즉시 저장, 필수 두 개면 첫 실행 완료(코드 변경 없음). **D02** 빠름/느림·대기 없음/대기 있음은 쌍마다 최대 1개, 고장 칸·깨끗함은 독립, 소급 없음
- 한 일: 두 앱 PRD(차슐랭 S4·S7, 크루즈 S5)에 기록. #14에 결정 코멘트. D02 구현 이슈 #17(이상준·길민종) 생성
- 남은 것(장호연): B01 약관 전문 작성 → 길민종에게 전달
- 바꾼 파일: `PRD.md`, 이 문서

### 2026-11-03 — 길민종 PRD PR #15 머지
- 한 일(Claude 에이전트와 함께, 장호연 "Yes, merge all three" 지시): 리뷰 뒤 새 커밋 `8852209`(띄어쓰기, capacityKwh 설명 반영, 인수인계) 확인 → 머지(`309eb81`). main 빌드·테스트 176개·app lint 통과
- PRD 9절 "검토 제안(미구현)"이 생김. 제출 전 구현 대상 아님(범위는 장호연·담당자 합의)
- 남은 결정(장호연): 이슈 #14 D01·D02·B01
- 바꾼 파일: 이 문서

### 2026-11-03 — 길민종 PRD PR #15 재리뷰
- 한 일(Claude 에이전트와 함께, 장호연 "continue"): #15 새 head `17cba44` → ✅ 머지 가능. 본문 그대로 두고 미구현 제안을 "9. 검토 제안(미구현)"으로 모음. 선택 제안: #10 머지 뒤 `capacityKwh`는 늘 비므로 차종 프리셋만 쓰인다고 덧붙이기
- 남은 결정(장호연): 이슈 #14 D01·D02·B01
- 바꾼 파일: 이 문서

### 2026-11-03 — 시연 스크립트 용어 PR #16 머지
- 한 일(Claude 에이전트와 함께, 장호연 "Yes, merge both" 지시): PR #16 머지(`8c032cb`, 크루즈 #28과 같은 내용). 두 저장소 `tools/` 같음 확인
- 바꾼 파일: 이 문서

### 2026-11-03 — 용어 결정 반영(실측→계산 평균, 충전 실패 유지)
- 장호연 결정("Yes, as recommended", 길민종 PRD PR #15 검토 결과): "실측 평균/속도" → **"계산 평균"**(충전구 개방 시간 기준 계산값이라서). "충전 실패"는 유지하고 정의(SoC가 오르지 않은 시도, 원인 미확정)를 밝힘. 슬로건·한 줄 소개·11-03 점수 결정 이유 유지
- 한 일: PRD 5.3 "용어" 항목과 F3·FAILED·계산 평균 행, 기획서, README, DOCX 재생성(`ad4d1d9`). 시연 스크립트 문구 PR #16(두 저장소 공통 도구, 크루즈 #28). PR #15에 결정 전달·rebase 요청. 앱 화면은 이미 "계산 평균"이라 할 일 없음
- 남은 결정(장호연): 이슈 #14 D01(동의 저장)·D02(반대 태그)·B01(약관 전문)
- 바꾼 파일: `PRD.md`, `README.md`, `docs/proposal.md`, `docs/submission/chachelin-application.docx`, 이 문서

### 2026-11-03 — 길민종 UI PR #11 머지
- 한 일(Claude 에이전트와 함께, 장호연 "Yes, merge both" 지시): PR #11 머지(`16d3623`). main 빌드·테스트 176개·app lint 통과
- 남은 것: 길민종 PRD PR #15는 인수인계 충돌로 rebase 필요. 장호연 결정 대기(용어·슬로건, 이슈 #14 D01·D02·B01)
- 바꾼 파일: 이 문서

### 2026-11-03 — 최종 검토(길민종 PR 4건)
- 한 일(Claude 에이전트와 함께, 장호연 "최신 받아서 최종 검토 및 이슈 보고" 지시): 길민종 UI PR #11 새 head `7b06522` 확인 → ✅ 여전히 머지 가능(#15보다 먼저). PRD PR #15 → 💬 결정 필요 + 🔧(11-03 점수 결정의 이유·실패 잦은 시드 요청 삭제 복원, 미구현 제안은 본문과 분리, 인수인계 충돌)
- 상태 확인: Supabase PR(차슐랭 #10 `d9fbcf6`, 크루즈 #22 `da25446`)은 07:17 재리뷰 그대로(🔧 남은 것: 제출 release 시연 모드 설정, 점수 공식). main 빌드·테스트 통과(차슐랭 176개, 크루즈 166개), `lint`는 core 3건(CAR_INFO·NAVI_ROUTE_SEARCH)으로 실패 — 차슐랭 #9, PR #10에서 해결됨
- 새 이슈는 만들지 않음(찾은 문제는 모두 열린 이슈에 있음: 차슐랭 #12·#13·#14, 크루즈 #25·#26)
- **장호연 결정 대기**: 길민종 PRD PR(차슐랭 #15, 크루즈 #27)의 슬로건·한 줄 소개 교체, "충전 실패"→"충전량 증가 미확인", "위험 구간"→"해제 다발 구간", "실측"→"계산 평균". 이슈 #14 D01(동의 저장)·D02(반대 태그)·B01(약관 전문)
- 바꾼 파일: 이 문서

### 2026-11-03 — 차슐랭 점수 공식 확정
- 장호연 결정(Claude 추천안 승인): `점수 = 평균 별점 × (1 − 최근 30일 실패율)`. 실패율 = 최근 30일 실패 ÷ (인증 + 실패), [실패 아님] 취소분 제외. 최근 30일 세션 5건 미만이면 감점 없음. 리뷰 5개 미만이면 "평가 중"
- 이유: 리뷰는 인증 충전 뒤에만 뜨므로 실패한 운전자는 별점을 못 남김 → 별점만 쓰면 고장 잦은 곳이 1위가 될 수 있음(예: ★4.8·실패 20% → 3.8 vs ★4.5·실패 0% → 4.5). 인증 수·kW는 가중치 근거가 약해 점수에 넣지 않고 카드에 따로 표시
- 검토했지만 안 고른 것: 별점만(지금 서버 구현, 실패가 순위에 반영 안 됨), 종합 가중 점수(기획서 옛 F6 문구, PT에서 가중치 설명이 어려움)
- 후속: 황준용 Supabase PR(차슐랭 #10)에서 `pleos_nearby_stations` 점수 계산 변경(새 migration) + 시드에 실패 잦은 시연 충전소
- 바꾼 파일: `PRD.md`(5.5 점수 규칙), `docs/proposal.md`(F6, 판정 규칙 표), `docs/submission/chachelin-application.docx`(재생성), 이 문서

### 2026-11-03 — 제출 빌드 데이터 모드 결정
- 장호연 결정: 공모전 제출 release APK는 시연 데이터 모드(시드 + 시뮬레이션)로 빌드. 이유: LIVE는 실제 충전소 데이터가 없어 심사위원 에뮬레이터에서 화면이 빔. PRD 7절에 기록(두 앱)
- 바꾼 파일: `PRD.md`, 이 문서

### 2026-11-03 — Supabase PR 리뷰(차슐랭 #10, 크루즈브레이커 #22)
- 한 일(Claude 에이전트와 함께, 장호연 지시): 두 PR 브랜치 빌드·테스트·lint 통과(200개, 180개, core lint 오류 0), core 동일, 비밀값 없음 확인. 스키마·RPC·core 대기열·철회 흐름 검토 후 두 PR에 리뷰 코멘트
- 결과: 🔧 수정 필요
  - 막는 것: ① 충전소 밖 충전도 서버에 올라감(11-02 결정 위반, S6는 서버 목록만 읽어 기기 저장 병합 필요) ② 시드 날짜가 11-02에 고정 → 크루즈 위험 구간 11-28쯤, 차슐랭 7일 실패 11-09쯤 만료 → 2차 PT에 시연 데이터 없음
  - 장호연 판단: ③ release = LIVE라 제출 APK에 시드가 안 보임(충전소 0개, 위험 구간 0개) → 제출 빌드를 DEMO로 할지 ④ 차슐랭 점수가 평균 별점만(PRD 초안은 실패 감점, 기획서 F6은 실패 반영) ⑤ AppContainer·Gradle·매니페스트 경로 밖 수정(받아도 된다고 봄)
  - 해결되는 것: 차슐랭 #9(CAR_INFO), 크루즈브레이커 #12·#20, 차슐랭 #8·크루즈브레이커 #19의 핵심(core 영속 대기열·철회 ACK)
- 바꾼 파일: 이 문서

### 2026-11-03 — 최종 검토
- 한 일(Claude 에이전트와 함께, 장호연 지시): 열린 PR 0건 확인. main 빌드·테스트(차슐랭 176개, 크루즈브레이커 156개, 실패 0)·앱 lint 통과, 두 저장소 core 동일
- 찾은 것: `:core:lintDebug` 오류 3건. `NAVI_ROUTE_SEARCH` 2건은 앱 매니페스트에 있어 문제 아님. `getEvBatteryCapacity`의 `CAR_INFO`는 어느 앱에도 선언 안 됨 → Pleos 문서상 심사 불통과 사유라 차슐랭 이슈 #9로 황준용에게(호출 제거 권장)
- 바꾼 파일: 이 문서

### 2026-11-03 — PR #7·#18 머지, 시연 리허설, 기획서 DOCX 재생성
- 한 일(Claude 에이전트와 함께, 장호연 "전부 검토 후 진행" 지시):
  - 길민종 알림 권한 복구 PR(차슐랭 #7, 크루즈브레이커 #18): 리뷰 뒤 커밋(설정에서 허용하면 미확인 횟수 초기화) 확인, 브랜치 빌드·테스트 통과(156개, 136개) → 머지
  - 에뮬레이터 리허설(최신 main 디버그 빌드 새로 설치, 동의부터):
    - 차슐랭 `demo-charge.sh success`: S4 "인증 충전 완료 · 시흥동 주민센터 · 40%→80% · 35분 · 53kW" + 별점. `fail`: S5 "충전 실패를 기록했어요". S1 시흥동 인증 21→22, 7일 실패 4→5, S6에 인증·실패 두 줄
    - 크루즈브레이커 `demo-drive.sh`(고정 위치): S5가 동의 전 정차 상태에서 정상 표시(PR #16 UX 제한 OK) → 출발 시 배너 "500m 앞 크루즈 해제 잦은 구간" + TTS 재생 → 90km/h 해제만 기록(`disengage ... speed=90.0`), 40km/h 해제·도착은 기록 안 됨 → P에서 S3 "지난 위험 구간 1곳 · 음성 안내 1회 · 내가 보탠 기록 1건"
  - 기획서 DOCX 재생성(두 앱, 11-03 md 기준). python은 이 PC에 `py -3`로 있었음
- 주의: 리허설 도우미가 [완료] 대신 "인증 충전 완료" 글자를 눌러 첫 리뷰는 제출되지 않았음(앱 문제 아님). `demo-*.sh` 출력을 `| tail`로 받으면 하트비트가 파이프를 잡아 명령이 안 끝남 → 파일로 받을 것
- 바꾼 파일: `docs/submission/*-application.docx`, 이 문서

### 2026-11-03 — 남은 일 점검
- 한 일(Claude 에이전트와 함께, 장호연 지시): 최신 main 받아 남은 일 점검. 오늘 머지된 PR(차슐랭 #5·#6, 크루즈브레이커 #15·#16·#17) 반영 후 main 빌드·테스트 통과(차슐랭 172개, 크루즈브레이커 152개, 실패 0). 두 저장소 core 동일
- 새로 생긴 것: 길민종 PR(차슐랭 #7, 크루즈브레이커 #18), 리뷰 이슈(차슐랭 #8, 크루즈브레이커 #19·#20·#21). Supabase 커밋은 아직 없음
- 위 "다음 할 일"을 지금 상태로 다시 씀
- 바꾼 파일: 이 문서

### 2026-11-03 — 시연 스크립트 도착 장면 수정(PR 리뷰 반영)
- 한 일(Claude 에이전트와 함께, 장호연 지시): Claude PR 리뷰 "수정 필요" 반영. `demo-drive.sh`의 `arrive()`와 `stop`이 100km/h에서 크루즈를 꺼서 도착도 해제로 기록되던 문제 → 차속 0으로 먼저 세운 뒤 해제, 그다음 P
- `wait_until`의 `date +%s%3N`이 GNU date 전용이라는 주석 추가(선택 제안)
- 확인: 가짜 콘솔·가짜 adb로 고정 모드·`stop` 순서 확인(정차 → 해제 → P). 에뮬레이터 리허설은 아직
- 바꾼 파일: `tools/demo-drive.sh`(두 저장소 같은 내용), 이 문서

### 2026-11-03 — 기획서 md 초안 갱신(DOCX 재생성 전)
- 한 일(Claude 에이전트와 함께, 장호연 지시): 두 앱 `docs/proposal.md`에 10-30·11-01 이후 결정(11-02 PRD 반영분, 길민종 PR #6·#16 내용)을 넣음. 양식 칸 구조(## 1.~8.)는 그대로
  - 차슐랭: F5·장면 2에 결과 알림, 판정 규칙에 "충전소 아님은 기기에만"·"표에 없는 경우 기록 안 함"·"조회 실패 시 재조회", 개인정보에 동의 전 기록 안 함·철회 시 서버 즉시 삭제, 7번 칸에 시연 배속 ×120 공개
- 결정한 것과 이유: 기능 번호 F2 빈칸은 PRD·PR·문서가 모두 같은 번호를 써서 기획서만 당기지 않음
- 남은 것: DOCX 재생성(`tools/build-proposal-docx.py`, python 필요), 7번 칸 【제출 전 기입】 2곳(GitHub 주소·시연 영상)
- 바꾼 파일: 두 저장소 `docs/proposal.md`, 이 문서

### 2026-11-02 — 결정 3개 PRD 반영(이상준 질문·시연 배속·서버)
- 한 일(Claude 에이전트와 함께, 장호연 결정): Claude 추천안을 그대로 승인하고 PRD에 반영. 팀원 메시지 초안은 채팅으로 받음(발송은 장호연)
- 결정한 것과 이유:
  1. 차슐랭 PRD 5.3에 없는 경우는 기록하지 않음(이상준 안 승인): 성공도 실패도 아닌 세션이 점수를 흐리지 않게. 단 충전소 **조회 실패**는 "충전소 아님"으로 단정하지 않고 업로드 전에 다시 조회(서버 연결 후 인증 충전 누락 방지)
  2. 앱이 뒤에 있을 때 결과 알림 넣기(누르면 S4/S5, S7에서 알림 권한 요청). 리뷰가 핵심인데 지금은 앱을 열어야만 평가 화면이 보임. 서버 다음 순서(이상준)
  3. 시연 배속 ×120 유지, PT·영상 자막에서 밝힘(실제 속도로는 30분)
  4. 동의 철회 → 가명 차량 ID로 서버 삭제 함수 호출, 그 차량 기록 즉시 완전 삭제, 앱은 성공할 때까지 재시도(두 앱 같은 방식, 크루즈브레이커 이슈 #12)
  5. 충전소 밖 충전(`NOT_AT_STATION`)은 서버에 안 올리고 기기에만(S6 표시). 필요한 데이터만 수집, PRD 5.5 스키마와 일치
- 바꾼 파일: `PRD.md`(5.3·5.4 S7·5.5·7절), 이 문서. 크루즈브레이커 `PRD.md` 5.6·7절도 같은 날 반영

### 2026-11-02 — 시연 스크립트 v2 정리·PR
- 한 일(Claude 에이전트와 함께, 장호연 지시): `feat/demo-scripts`에 main merge(이 문서 충돌은 main 내용으로), 스크립트를 v2와 현재 앱에 맞춤
  - `tools/demo-drive.sh`: 기본 = 고정 위치 시연(출발 → 500m 앞 안내 → 100km/h 해제 기록 → 40km/h 해제는 기록 안 함 → 정차 요약). `route` = 경로 재생(1배속). 차속 권한 `pm grant`를 스크립트가 줌
  - `tools/demo/gyeongbu-north.csv`: 표시만 다시 만듦(안내 500m, 정체 `jam` → 저속 `slow` 300m). 좌표는 그대로. 이 PC에 python이 없어 `make-route.py`를 node로 옮겨 기존 CSV와 똑같이 나오는지 확인한 뒤 생성
  - `tools/demo-charge.sh`: 전제에 S7 동의 먼저, 시연 배속 ×120 추가(이상준 요청)
- 결정한 것과 이유:
  - 정체 장면을 저속 해제로 바꿈: v2에서 정체 필터가 없어지고 "해제 순간 60km/h 이상"만 셈
  - 경로 재생은 1배속 기본: 안내가 3분에 한 번뿐이라 2배속이면 c 구간 안내가 안 나옴. 안내 장면은 a·c 둘만
  - 앱 위치 재생 모드(`DEMO_ROUTE` 브로드캐스트)는 아직 없음 → 지금 시연은 고정 위치 모드로
- 확인: 가짜 콘솔(node)·가짜 adb로 세 모드의 값 순서 확인. 에뮬레이터 실행은 아직
- 바꾼 파일: `tools/demo-drive.sh`, `tools/demo-charge.sh`, `tools/demo/make-route.py`, `tools/demo/gyeongbu-north.csv`, 이 문서

### 2026-11-02 오후 — PR #4·#3 머지(크루즈브레이커 #14 포함)
- 한 일(Claude 에이전트와 함께, 장호연 지시 "merge all"): head가 리뷰한 커밋과 같은지 확인(`--match-head-commit`) 후 merge commit으로 머지. 순서: 차슐랭 #4(`5dc8362`) → 크루즈브레이커 #14(`bdc8f88`) → 차슐랭 #3(`6bab9d0`)
- 확인: 머지된 두 저장소 main에서 `assembleDebug test` 통과(차슐랭 디버그 테스트 80개, 크루즈브레이커 61개, 실패 0). `core/` 두 저장소 동일
- 결정: 자동 리뷰 `❓` 항목(담당 경로 밖 수정)은 모두 승인으로 보고 머지. 크루즈브레이커의 `docs/chachelin-pr-3-review.md`도 그대로 들어감
- 바꾼 파일: 이 문서(코드는 PR 머지만)

### 2026-11-02 — 이상준 `feat/charge-session-logic` 리뷰(PR 전)
- 한 일(Claude 에이전트와 함께, 장호연 지시): PR이 없어 브랜치를 임시 작업 폴더에 받아 `assembleDebug test :app:lintDebug` 실행 + 코드 리뷰. 결과는 장호연에게 채팅으로 보고(PR 코멘트 없음)
- 결과: `🔧 수정 필요` — 컴파일 오류 1건(위 "진행 중"). 그 한 줄만 임시로 고치면 `assembleDebug test :app:lintDebug` 통과(디버그 단위 테스트 55개 실패 0, lint 통과). 판정 규칙은 PRD 5.3과 맞음, 서버로 좌표 안 감, 퍼미션 이유는 커밋 메시지에 있음, 비밀값 없음, 인수인계 문서 갱신됨
- 서버 구현 때 챙길 것(황준용): 동의 철회의 `deleteMine`이 한 번만 시도하고 실패를 삼킴, 철회 때 진행 중 업로드를 기다리지 않고 삭제를 보냄(늦게 도착한 업로드가 남을 수 있음)
- 바꾼 파일: 이 문서
- 이후(같은 날): 이상준이 오류를 고치고 PR #3을 올림. 황준용 core v0.4.0 PR(차슐랭 #4, 크루즈브레이커 #14)도 올라옴. 정기 리뷰 작업이 세 PR 모두에 코멘트를 남김(정상 동작 확인). 위 "진행 중"에 반영

### 2026-11-01 저녁 — 공통 문서 정정·주최 측 문의 추가
- 한 일(Claude 에이전트 호공이와 함께, 장호연 지시):
  - 팀 문서(`docs/team-collaboration-plan.md`, 공통 원본) 4절 1번 "Organization으로 이전(황준용, 11-01까지)"을 10-30 결정(만들지 않음, 개인 계정 유지 — 심사용 계정 권한은 주최 측 문의 4번 답변 후 결정)으로 정정, 2절·8절 Organization 문구도 맞춤. 2절 이민하 담당을 판정 규칙 v2(정체 필터 없앰)에 맞춤. 9절 8번(`TTS_SERVICE` 권한 승인 여부, 이민하 요청) 추가. 크루즈브레이커에도 같은 내용으로 복사
  - `docs/organizer-inquiry-email.md` 11번 추가(두 저장소 같은 내용)
  - `feat/demo-scripts`: main을 merge해 보니(`git merge-tree`) 이 문서에서 충돌 → 멈춤, push·PR 안 함
  - 조사(읽기만): 이 저장소에 이상준·황준용의 원격 브랜치·커밋·PR 없음. 커밋 작성자는 장호연·길민종(giljong)뿐. `core/`·`tools/`·공통 문서는 크루즈브레이커와 같음(파일 해시 비교)
- 바꾼 파일: `docs/team-collaboration-plan.md`, `docs/organizer-inquiry-email.md`, 이 문서. 크루즈브레이커에서 한 일(PR #3~#6 머지, 이슈 #7~#9, PRD·기획서 v2 반영)은 크루즈브레이커 `docs/handoff/jang-hoyeon.md`

### 2026-11-01 — 팀원 작업 확인·길민종 PR 2번 머지
- 한 일(Claude 에이전트와 함께): 두 저장소 fetch. 새 작업은 길민종 `fix/ui-review-followups`(PR 2번)뿐. 이상준·이민하·황준용은 원격 브랜치·커밋 없음, 인수인계 문서도 10-30 이후 변경 없음
- PR 2번: 자동 리뷰 `✅` 확인, head가 리뷰한 커밋과 같은지 확인 후 merge commit으로 머지(차슐랭 `0f17abb`, 크루즈브레이커 `678d7a6`). 머지된 main에서 두 앱 `assembleDebug test` 통과
- 다음: 11-05 중간 점검 전에 이상준·이민하·황준용 진행 상황 직접 확인

### 2026-11-01 — 진행 현황 점검·길민종 UI PR 리뷰
- 한 일(Claude 에이전트와 함께): 두 저장소 fetch(main 변경 없음, 새 브랜치 `feat/ui-screens`). PRD 대비 진행 현황 점검 — 판정 로직(이상준·이민하)·서버(황준용)·포그라운드 서비스는 아직 시작 전, 화면만 PR로 올라옴
- 두 PR 리뷰: PR 브랜치에서 `assembleDebug test` 통과, 필수 6항목 통과 → 둘 다 `✅ 머지 가능` 코멘트
  - 차슐랭 후속: S2 실측 속도 분포 빠짐(UiState 필드 필요, 이상준과), 이상준에게 남긴 질문 4개, 에뮬레이터 확인 아직
  - 크루즈브레이커 후속: 10-30 실험에서 정한 **S5 자동 기록 안내 문구·S4 자동 기록 켜기/끄기가 빠짐**(SettingsUiState 필드는 이민하와), 안내 거리 선택지 500m/1km/2km가 PRD 5.3(0.8~1.2km)과 다름
- 결정한 것과 이유: 빠진 항목은 UiState 합의가 필요한 후속 작업이라 머지를 막지 않음(ViewModel 작업이 이 화면 위에서 시작해야 함)
- 머지: 장호연이 직접 지시해 Claude가 두 PR을 merge commit으로 머지(차슐랭 `b147aa1`, 크루즈브레이커 `2dd99af`). 머지 후 main에서 `assembleDebug test` 통과
- 남은 일: `feat/demo-scripts` PR 올리기. 정기 리뷰 작업이 10-31 PR을 하루 동안 리뷰하지 않았음 → 정기 작업이 실제로 도는지 확인
- 바꾼 파일: 이 문서(코드 변경 없음, PR 코멘트 2개)

### 2026-10-31 — 주최 측 문의 메일 초안
- 한 일(Claude 에이전트와 함께): 9절 1~7번과 `contest-analysis-todo.md` 8절 미확정 항목(심사용 계정, 저장소 주소의 계정명 노출, 시연 영상 형식, 접수 메일 제목, Vehicle Control SDK)을 묶어 메일 초안 10문항 작성
- 결정한 것과 이유: 두 작품 질문을 메일 한 통으로 보냄(문의 창구 하나). 발송은 대표자가 직접 함
- 바꾼 파일: `docs/organizer-inquiry-email.md`(새 파일, 원본은 팀 폴더), `docs/team-collaboration-plan.md` 9절, 이 문서

### 2026-10-31 — "주행 중 DO 차단" 재확인
- 한 일(Claude 에이전트와 함께): 이민하가 물은 "DO 차단 조건"을 에뮬레이터에서 재현. D·80km/h에서 크루즈브레이커는 정차 중 실행 후 주행·주행 중 콜드 스타트·다시 앞으로 세 경우 모두 차단 안 됨(화면 캡처로 확인). DO가 없는 차슐랭은 주행 중 실행하면 "주행 중에는 사용할 수 없습니다" 화면
- 결정한 것과 이유: 10-30 관찰은 차슐랭을 크루즈브레이커로 착각한 것으로 정리(두 앱 다 기본 아이콘이라 독에서 구분이 안 됨). 시연에 S1 주행 화면 포함. 주최 측 문의 6번은 "배포본에서도 DO만으로 허용되는지"로 낮춤
- 주의: 차단 화면이 떠도 `topResumedActivity`는 그 앱으로 나오므로 차단 여부는 화면 캡처로만 판단 가능
- 바꾼 파일: 크루즈브레이커 `docs/background-freeze-experiment.md`·`PRD.md`, 두 저장소 `docs/team-collaboration-plan.md`, 이 문서

### 2026-10-31 — 백그라운드 동결 실험 결과 반영
- 한 일(Claude 에이전트와 함께): 두 저장소 최신 pull, 열린 PR 확인(두 저장소 모두 0개, 리뷰할 것 없음). 이민하 실험 문서(크루즈브레이커 `docs/background-freeze-experiment.md`)의 "장호연 확인 요청" 중 문서 몫 반영
  - 크루즈브레이커 PRD 4절에 "첫 실행 동의 이후"·시동 시 자동 기록, 5.4를 결과(포그라운드 서비스 `specialUse` + 부팅 수신기)로 다시 씀, 8절 동결 행을 남은 위험 중심으로, DO 행에 "재현 안 됨" 메모
  - 두 저장소 `docs/team-collaboration-plan.md` 9절: 1번을 심사 허용 여부로 바꾸고 7번(실차 시동 off 방식) 추가
  - 차슐랭 PRD 8절 "충전 중 생존" 행에 같은 원인·해법 메모(이상준 확인)
- 결정한 것과 이유: 이민하 결정 사항 5개(서비스 타입 `specialUse`, 구현 이민하 PR, S5 동의 후 기본 켜짐·S4에서 끔, S5 안내 문구, PRD 4절 문구)에 이견 없음 → 그대로 진행. 서비스가 `signals.start()`·`disengageDetector.start()`를 소유하는 구조 변경도 동의(코드는 `feat/recording-service` PR에서 리뷰)
- 남은 일: 주최 측 메일 발송(9절 1~7번), DO 차단 조건을 이민하에게 전달
- 바꾼 파일: 크루즈브레이커 `PRD.md`, 차슐랭 `PRD.md`, 두 저장소 `docs/team-collaboration-plan.md`, 이 문서

### 2026-10-30 — 문서는 main에 바로
- 한 일(Claude 에이전트와 함께): CLAUDE.md "작업 방식"에 규칙 추가 — 코드는 브랜치+PR, 문서(PRD·README·CLAUDE.md·docs/)는 PR 없이 main에 바로 push
- 결정한 것과 이유: 문서는 리뷰 대기 없이 바로 반영(장호연 결정). 담당 경로 규칙은 그대로
- 바꾼 파일: `CLAUDE.md`(두 저장소 동일), 이 문서

### 2026-10-30 — APK 파일명에 영문 팀명
- 한 일(Claude 에이전트와 함께): 대회 사이트 지원·제출 방법 페이지 원문 확인 → APK 파일명 규칙이 `팀명_앱이름_release.apk`라 팀명 생략 결정을 되돌림. 기획서 7번 칸·PRD·이 문서를 `SamgakGorae_<앱>_release.apk`로 고치고 DOCX 재생성
- 결정한 것과 이유: 팀명은 로마자 `SamgakGorae`(공지 규칙 + 한글 파일명 금지 둘 다 지킴). GitHub Organization은 만들지 않음(장호연 결정)
- 바꾼 파일: `docs/proposal.md`, `docs/submission/*.docx`, `PRD.md`, 이 문서

### 2026-10-30 — PR 자동 리뷰 켬
- 한 일: 이 PC에 GitHub CLI 설치, `hoyeoni1`로 gh 로그인(브라우저 방식, 계정 전체 권한), 정기 작업 "삼각고래 PR 리뷰" 생성(매일 09~23시 매시 정각)
- 동작: 새 PR·새 커밋만 리뷰 → `C:\Users\H\.claude\pr-review\`의 전용 클론에서 빌드·테스트 → `docs/pr-review-guide.md` 기준으로 PR 코멘트(판정 첫 줄) → 결과를 앱 알림으로 보고. 머지·승인·push는 하지 않음
- 확인: 두 저장소 PR 조회 OK(열린 PR 0개), 전용 클론 빌드·테스트 통과. 실제 PR에 대한 첫 리뷰는 아직 없음

### 2026-10-30 — PR 리뷰·머지 규칙
- 결정: 모든 변경은 PR. 리뷰는 Claude(장호연 계정 에이전트), 머지는 장호연만. 교차 리뷰는 "교차 읽기"(PT 대비, 승인 권한 없음)로 바꿈
- 바꾼 파일: `docs/team-collaboration-plan.md` 2·4·7절, `docs/pr-review-guide.md`(새로 만듦, 리뷰 기준), PR 템플릿, `CLAUDE.md`, README, `docs/handoff/README.md`
- 남은 일: PC에 `gh`(GitHub CLI) 설치·로그인 → 정기 PR 리뷰 작업 설정. 머지 권한을 장호연으로 제한하는 것은 Organization 이전 후 브랜치 보호에서 설정

### 2026-10-30 — 기획서 제출본
- 한 일(Claude 에이전트와 함께): 접수양식 02 파트 1~8번 원고 `docs/proposal.md` 작성, 양식에 자동으로 채우는 `tools/build-proposal-docx.py` 작성, 차슐랭 접수양식 DOCX 생성
- 결정한 것과 이유:
  - 사업성 숫자: 국내 Pleos Connect 탑재 2027년 말 약 23만 대로 추정(그랜저 71,775 + 아반떼 79,335대/년, 2025 현대차 판매 실적). 설치율·사용 비율·Retention 목표는 가정이라고 명시
  - 원고는 마크다운 하나로 관리하고 DOCX는 스크립트로 다시 만든다(손으로 고친 DOCX와 원고가 어긋나지 않게)
  - 두 작품 공동 초안(비교·검토용)은 Claude 문서 https://claude.ai/artifact/MwikFg1WTHiANoDykTUKGc
- 바꾼 파일: `docs/proposal.md`, `docs/submission/`, `tools/build-proposal-docx.py`, 이 문서

### 2026-10-30
- 한 일(Claude 에이전트와 함께):
  - PRD에 부문·팀명 기입
  - core v0.2 설계·구현
    - Vehicle SDK·AAOS 차속·NaviHelper
    - 내비 재연결
    - Fake 신호
    - GeoMath
  - 두 앱 뼈대
    - AppContainer
    - domain/data 뼈대
    - UiStates
    - S1 예시
    - 신호 모니터
  - `tools/vconsole.sh`
  - README "시작하기"
  - 인수인계 규칙(`docs/handoff/`)
- 결정한 것과 이유:
  - 화면은 Compose(UiState로 `@Preview` 먼저 만들 수 있어 UI·로직 병렬 작업에 유리)
  - 판정 로직은 담당자 몫이라 TODO로 남기고, 기준값·모델·예시 테스트만 넣음
  - 차슐랭 DO 선언 제거(정차 전용 앱, PRD 3.2)
  - APK 파일명에서 팀명 제외(한글 파일명 금지 규칙과 맞춤)
- 에뮬레이터 실측:
  - EV·주행·크루즈·차속·위치 수신 OK
  - 내비 앱이 재시작되면 응답이 끊김 → core에서 10초 감시 후 재연결하도록 고침
  - 크루즈브레이커 DO 차단
  - 차속 권한 대화상자가 "위치 정보"로 표시됨
- 바꾼 파일: 거의 전부(커밋 `Add core v0.2 signals and app foundation...`, `Name release APK...`)
