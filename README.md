# 차슐랭 (Chachelin)

차량이 직접 확인한 충전 기록으로 충전소를 평가하는 Pleos Connect 앱.

- 핵심 신호: Vehicle SDK EV 배터리(SoC·충전 상태·충전구), NaviHelper 충전 사업자 목록
- 실측 메모: [docs/vehicle-data-findings.md](docs/vehicle-data-findings.md) (에뮬레이터에서 SoC는 자동으로 오르지 않음, 콘솔로 주입)

## 개발

- 팀 역할·일정·규칙: [docs/team-collaboration-plan.md](docs/team-collaboration-plan.md)
- 공통 모듈 `core/`·`tools/`·팀 문서는 두 앱 저장소에 똑같이 들어 있다. 원본은 차슐랭 저장소이며 황준용만 수정한다. 동기화: 차슐랭에서 `tools/sync-core.sh ../CruiseBreaker` (확인만: `--check`)
- 빌드: `./gradlew assembleDebug` (JDK 17, `local.properties`에 `sdk.dir`)
