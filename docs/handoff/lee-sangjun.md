# 인수인계 — 이상준 (차슐랭 로직)

담당 경로: `app/src/main/java/com/hyundaiapp/chachelin/domain/`, `data/`(인터페이스 합의), ViewModel

## 현재 상태 (2026-10-30 기준, 매번 덮어쓰기)

- 한 일 요약: 아직 시작 전. 시작점은 장호연이 만들어 둔 뼈대(아래).
- 진행 중 브랜치·PR: 없음
- 다음 할 일(우선순위 순):
  1. `domain/ChargeSessionTracker.kt` TODO — PRD 5.3 상태 전이. `FakeVehicleSignals`로 단위 테스트 먼저
  2. S3·S4·S5 ViewModel(`ui/UiStates.kt`의 상태를 내보냄) — 길민종과 계약 합의
  3. 충전소 매칭(`StationRepository.nearest`, 150m)·길안내(`NaviControl.requestRoute`)
- 막힌 점·질문(누구에게): 없음
- 주의(함정·직접 겪은 이상한 동작):
  - 에뮬레이터 SoC는 충전 중에도 자동으로 안 오름 → 콘솔로 주입
  - 위치는 판교 부근 고정, 모의 주행 없음
  - `getEvBatteryCapacity`는 `CAR_ENERGY`만으로 "need Permission" → 배터리 용량은 차종 프리셋(S7) 값을 쓸 것
  - 시연용 기준값은 `ChargeRules.DEMO`(디버그 빌드 자동 적용)
- 확인 방법: 앱 실행 → 개발 메뉴 → 신호 모니터. 값 바꾸기는 `bash tools/vconsole.sh up` 후 `put '...'`(README "시작하기")

## 작업 기록 (최신이 위, 지우지 않음)

### 2026-10-30
- 장호연: 파일 생성. 첫 작업은 README "담당별 첫 작업" 기준으로 적어 둠.
