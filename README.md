# 차슐랭 (Chachelin)

차량이 직접 확인한 충전 기록으로 충전소를 평가하는 Pleos Connect 앱. 팀 삼각고래 · 일반 부문.

- 기획: [PRD.md](PRD.md)
- 핵심 신호: Vehicle SDK EV 배터리(SoC·충전 상태·충전구), NaviHelper 위치·충전 사업자·길안내
- 실측 메모: [docs/vehicle-data-findings.md](docs/vehicle-data-findings.md) (에뮬레이터에서 SoC는 자동으로 오르지 않음, 콘솔로 주입)

## 시작하기 (처음 한 번)

1. 개발 환경: [docs/dev-env-setup-log.md](docs/dev-env-setup-log.md) — Android Studio, JDK 17, Pleos 에뮬레이터(`Pleos_Connect_2_1_0`)
2. 두 저장소를 **같은 폴더 아래 나란히** clone (`.../Chachelin`, `.../CruiseBreaker`)
3. Android Studio로 `Chachelin` 폴더 열기 → Gradle Sync → 에뮬레이터 켜고 ▶ Run `app`
   - CLI: `./gradlew assembleDebug` / `./gradlew installDebug` / 테스트 `./gradlew test`
4. 앱이 켜지면 **개발 메뉴**(디버그 빌드에만 보임) → **신호 모니터**에서 `vehicle=READY`, `navi=READY`, SoC 값이 보이면 정상
5. 값 바꿔 보기: `bash tools/vconsole.sh up` 후 `bash tools/vconsole.sh put 'ev-battery?field=charge_port_open&value=true'` → 신호 모니터의 충전구가 `켜짐`으로 바뀜

## 코드 구조

```
app/src/main/java/com/hyundaiapp/chachelin/
├─ AppContainer.kt      객체 모음(수동 DI). 신호·저장소·판정기를 여기서 만든다
├─ domain/              판정 로직 (이상준)
│  ├─ ChargeSessionTracker.kt   F1~F4 충전 세션 상태 기계  ← TODO
│  ├─ ChargeSession.kt          세션·결과 모델, ChargeRules(PRD 5.3 기준값)
│  └─ ChargeMath.kt             실측 평균 kW 계산(예시 테스트 있음)
├─ data/                저장소 인터페이스 + Fake(시드 데이터). 서버 구현은 황준용
├─ ui/                  화면 (길민종) — 화면은 UiState만 보고 그린다
│  ├─ UiStates.kt       S1~S7 화면 상태 계약 (이상준 ↔ 길민종 합의)
│  ├─ PreviewData.kt    미리보기 샘플
│  ├─ ChachelinNav.kt   화면 목록·이동, 개발 메뉴
│  ├─ stations/         S1 예시: Repository → ViewModel → UiState → Screen
│  └─ debug/            신호 모니터
core/                   공통 차량·내비 신호 (황준용) — core/README.md
tools/                  vconsole.sh(콘솔 조작), sync-core.sh, vtest.sh (장호연)
```

## 담당별 첫 작업

| 담당 | 시작 지점 |
|---|---|
| 이상준 | `domain/ChargeSessionTracker.kt`의 TODO. `FakeVehicleSignals`로 단위 테스트부터(`app/src/test/...`) |
| 길민종 | `ui/UiStates.kt` + `PreviewData.kt`로 S1~S7 화면을 `@Preview`에서 먼저 만들기. `ui/theme/Theme.kt` 교체 |
| 황준용 | `data/`의 인터페이스로 서버 구현, `core/README.md`의 "다음에 할 일" |
| 장호연 | `tools/vconsole.sh`로 시연 시나리오(PRD 7절) 스크립트 |

## 규칙

- 팀 역할·일정·규칙: [docs/team-collaboration-plan.md](docs/team-collaboration-plan.md)
- `main` 직접 push 금지 → 브랜치 `feat/...`, `fix/...`, `docs/...`(영문 소문자·하이픈) + PR
- 공통 모듈 `core/`·`tools/`·팀 문서는 두 저장소에 똑같이 들어 있다. 원본은 이 저장소이며 `core/`는 황준용만 수정한다. 동기화: `tools/sync-core.sh ../CruiseBreaker` (확인만: `--check`)
- 퍼미션을 추가하면 `AndroidManifest.xml`과 `AppContainer`의 `CoreFeature`를 함께 맞춘다
- 이 앱은 DO를 선언하지 않는다(정차 전용, 주행 중에는 플랫폼이 막음)
