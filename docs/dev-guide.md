# 팀 개발 안내 — 차슐랭 (Chachelin)

팀 내부용 문서다. 심사용 안내(실행 방법·테스트 환경·APK 위치·오픈소스 고지)는 [README.md](../README.md)에 있다. 원래 README에 있던 내용을 2026-11-05에 이 파일로 옮겼다.

## 시작하기 (처음 한 번)

1. 개발 환경: [docs/dev-env-setup-log.md](dev-env-setup-log.md) — Android Studio, JDK 17, Pleos 에뮬레이터(`Pleos_Connect_2_1_0`)
2. 두 저장소를 **같은 폴더 아래 나란히** clone (`.../Chachelin`, `.../CruiseBreaker`)
3. Android Studio로 `Chachelin` 폴더 열기 → Gradle Sync → 에뮬레이터 켜고 ▶ Run `app`
   - CLI: `./gradlew assembleDebug` / `./gradlew installDebug` / 테스트 `./gradlew test`
4. 앱이 켜지면 **S7 첫 실행 동의** → 동의·차종·[시작하기] → **S1 근처 충전소**(debug·release 같음). 디버그 빌드는 S1 제목 "근처 충전소"를 **길게 누르면** 개발 메뉴가 열린다 → **신호 모니터**에서 `vehicle=READY`, `navi=READY`, SoC 값이 보이면 정상
5. 값 바꿔 보기: `bash tools/vconsole.sh up` 후 `bash tools/vconsole.sh put 'ev-battery?field=charge_port_open&value=true'` → 신호 모니터의 충전구가 `켜짐`으로 바뀜

## 코드 구조

```
app/src/main/java/com/hyundaiapp/chachelin/
├─ AppContainer.kt      객체 모음(수동 DI). 신호·저장소·판정기를 여기서 만든다
├─ domain/              판정 로직 (이상준)
│  ├─ ChargeSessionTracker.kt   F1~F4 충전 세션 상태 기계  ← TODO
│  ├─ ChargeSession.kt          세션·결과 모델, ChargeRules(PRD 5.3 기준값)
│  └─ ChargeMath.kt             계산 평균 kW 계산(예시 테스트 있음)
├─ data/                저장소 인터페이스 + Fake + Supabase 실제 구현(황준용)
├─ ui/                  화면 (길민종) — 화면은 UiState만 보고 그린다
│  ├─ UiStates.kt       S1~S7 화면 상태 계약 (이상준 ↔ 길민종 합의)
│  ├─ PreviewData.kt    미리보기 샘플
│  ├─ ChachelinNav.kt   화면 목록·이동, 개발 메뉴
│  ├─ stations/         S1 예시: Repository → ViewModel → UiState → Screen
│  └─ debug/            신호 모니터
core/                   공통 차량·내비 신호 (황준용) — core/README.md
tools/                  vconsole.sh(콘솔 조작), demo-charge.sh·demo-drive.sh(시연 시나리오), build-proposal-docx.py(기획서), sync-core.sh, vtest.sh (장호연)
```

## 담당별 첫 작업

| 담당 | 시작 지점 |
|---|---|
| 이상준 | `domain/ChargeSessionTracker.kt`의 TODO. `FakeVehicleSignals`로 단위 테스트부터(`app/src/test/...`) |
| 길민종 | `ui/UiStates.kt` + `PreviewData.kt`로 S1~S7 화면을 `@Preview`에서 먼저 만들기. `ui/theme/Theme.kt` 교체 |
| 황준용 | `data/`의 인터페이스로 서버 구현, `core/README.md`의 "다음에 할 일" |
| 장호연 | `tools/vconsole.sh`로 시연 시나리오(PRD 7절) 스크립트 |

## 규칙

- **작업을 마칠 때마다 인수인계 문서 필수**: `docs/handoff/<영문-이름>.md` (양식·규칙: [docs/handoff/README.md](handoff/README.md)). PR에 없으면 머지하지 않는다
- **PR 리뷰는 Claude, 머지는 장호연만** 한다(작성자·에이전트는 머지 금지). 리뷰 기준: [docs/pr-review-guide.md](pr-review-guide.md)
- 각자 자기 담당 경로·브랜치에서만 작업한다
- 팀 역할·일정·규칙: [docs/team-collaboration-plan.md](team-collaboration-plan.md)
- `main` 직접 push 금지 → 브랜치 `feat/...`, `fix/...`, `docs/...`(영문 소문자·하이픈) + PR
- 공통 모듈 `core/`·`tools/`·팀 문서는 두 저장소에 똑같이 들어 있다. 원본은 이 저장소이며 `core/`는 황준용만 수정한다. 동기화: `tools/sync-core.sh ../CruiseBreaker` (확인만: `--check`)
- 퍼미션을 추가하면 `AndroidManifest.xml`과 `AppContainer`의 `CoreFeature`를 함께 맞춘다
- 이 앱은 DO를 선언하지 않는다(정차 전용, 주행 중에는 플랫폼이 막음)
