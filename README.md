# 차슐랭 (Chachelin)

차량이 직접 확인한 충전 기록으로 충전소를 평가하는 Pleos Connect 앱.

- 핵심 신호: Vehicle SDK EV 배터리(SoC·충전 상태·충전구), NaviHelper 충전 사업자 목록
- 실측 메모: [docs/vehicle-data-findings.md](docs/vehicle-data-findings.md) (에뮬레이터에서 SoC는 자동으로 오르지 않음, 콘솔로 주입)

## 개발

- 팀 역할·일정·규칙: 공통 저장소 `hyundaiapp-common`의 `docs/team-collaboration-plan.md`
- 공통 모듈: 이 저장소와 `hyundaiapp-common`을 같은 폴더 아래 나란히 clone하면 `../hyundaiapp-common/core`를 `:core`로 포함한다. 제출 전에는 `core/`를 이 저장소에 subtree로 넣는다.
- 빌드: `./gradlew assembleDebug` (JDK 17, `local.properties`에 `sdk.dir`)
