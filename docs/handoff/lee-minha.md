# 인수인계 — 이민하 (크롱 로직, 이 저장소에서는 시연 영상 도구)

담당 경로: 이 저장소에는 담당 경로가 없다(크롱 로직 담당). 여기서 한 일은 시연 영상용 공통 도구·문서뿐이다. 크롱 쪽 기록은 크롱 저장소 `docs/handoff/lee-minha.md`

## 현재 상태 (2026-11-13 기준, 매번 덮어쓰기)

- 한 일 요약: 시연 영상용 녹화 스크립트 `tools/demo-video.sh`와 영상 시나리오 [docs/demo-video-scenario.md](../demo-video-scenario.md)를 브랜치에 올렸다(두 앱 공통, 크롱 저장소에도 같은 파일)
  - `bash tools/demo-video.sh chachelin`: 첫 실행 동의(cr1) → 충전구 열림 → 자동 기록 → 40→80% → 인증 충전 완료 → 별 5개·태그 2개·평가 보내기 → 홈 1건(cr2) → 충전 실패 기록 → 내 기록 → 점수순 목록 → 시흥동·대왕판교IC·E-pit 상세 → 길안내(cr3). 화면 터치·차량 값 주입·녹화·주입 시각 기록(`events.tsv`)·블렌더에 붙일 화면 캡처를 한 번에 한다
  - 11-13 확인: 에뮬레이터 + 제출 release APK(`apk/SamgakGorae_Chachelin_release.apk`) + 실제 서버(시연 데이터 모드)에서 전체 녹화가 끝까지 통과
- 진행 중 브랜치·PR: `feat/demo-video-script-20261113`. `tools/`는 장호연 담당이라 main에 넣지 않았다 → 장호연 확인 필요
- 다음 할 일(우선순위 순):
  1. 블렌더 컷 렌더, 녹화와 이어 붙이기(저장소 밖 `demo-video/` 폴더)
- 막힌 점·질문(누구에게):
  - 장호연: 이 브랜치를 머지할지, 영상 제작용으로만 둘지 결정. 머지하면 `tools/sync-core.sh --check`가 통과하도록 크롱 브랜치도 같이
- 주의(함정·직접 겪은 이상한 동작):
  - **pm clear로 앱 데이터를 지우면 그 설치가 서버에 올린 기록이 남는다.** 스크립트는 시작·끝에 [내 기록] → [동의 철회]를 눌러 지운다(`WITHDRAW=0`이면 남김)
  - 길안내를 시작하면 내비에 경로가 남는다 → 스크립트가 내비의 [안내 종료]를 누른다
  - 평가를 보낸 뒤에는 충전을 시작한 화면(홈)으로 돌아온다. 홈에 "내 충전 기록 1건 · 최근 기록 … 인증"이 보인다
  - 실패 대기 35초는 녹화하지 않는다(cr3은 충전구를 닫기 직전부터). 그때 화면의 경과 시간은 시연 배속 때문에 "1시간 6분"으로 보인다
  - Git Bash에서 `MSYS_NO_PATHCONV=1`을 스크립트 전체에 걸면 `vconsole.sh`의 `curl -o /dev/null`이 깨진다 → adb 호출에만 건다
  - `adb screenrecord`는 소리를 못 담는다. 에뮬레이터 시계는 PC와 다르다(앱의 날짜·시각 표시가 그 기준)
- 확인 방법: 에뮬레이터 실행 → `adb install -r apk/SamgakGorae_Chachelin_release.apk`(서명이 다른 debug가 깔려 있으면 먼저 지움) → `bash tools/demo-video.sh chachelin > log 2>&1` → `build/demo-video/chachelin/cr1.mp4`·`cr2.mp4`·`cr3.mp4`·`events.tsv`, `build/demo-video/assets/chachelin_*.png`

## 작업 기록 (최신이 위, 지우지 않음)

### 2026-11-13 (이민하, Claude 에이전트와 작업 — 시연 영상 시나리오·녹화 스크립트)
- 한 일: `tools/demo-video.sh`(새 파일, 크롱·차슐랭 공통), `docs/demo-video-scenario.md`(새 파일). 에뮬레이터에서 차슐랭 전체 녹화 통과. 에뮬레이터의 debug 차슐랭을 지우고 제출 release APK를 설치함
- 결정한 것과 이유(이민하): 영상에서 충전 장면이 먼저, 충전소 고르기가 나중 — 에뮬레이터 위치가 고정이라 충전은 항상 "시흥동 주민센터"로 잡혀, 다른 곳으로 길안내한 뒤 충전하면 이름이 어긋난다. 블렌더 → 녹화 전환은 편당 3번, 블렌더 영상에는 자막 없음
- 리허설이 서버에 남긴 것: 스크립트를 고치기 전 pm clear로 지운 설치 1개의 기록(시흥동 주민센터 인증 1·실패 1)이 서버에 남았다 → 촬영 전 `server/supabase/clear-rehearsal-data.sql` 필요. 그 뒤 실행분은 동의 철회로 지움
- 바꾼 파일: `tools/demo-video.sh`, `docs/demo-video-scenario.md`, `docs/handoff/lee-minha.md`(새 파일)
