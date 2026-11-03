# 시연 데이터 날짜 갱신

시드 날짜는 최초 migration 실행 시점에 고정된다. 시간이 지나면 차슐랭 최근 7일 실패와 크루즈 최근 28일 위험도에서 빠진다. 시연 전에 관리자가 다음 수동 절차를 실행한다. 자동 keep-alive나 무료 플랜 운영 대책은 이 변경에 포함하지 않는다.

## 실행

본인 CLI 인증과 프로젝트 link를 마친 뒤 **차슐랭 server/**에서 실행한다([CLI 설정](cli-setup.md)). 앱 개발자 전체에게 관리자 권한을 줄 필요는 없다.

```powershell
$supabaseCli = Join-Path $env:LOCALAPPDATA 'hyundai-dev\supabase\supabase.exe'
# 읽기 전용 상태 확인
& $supabaseCli db query --linked --file supabase/demo-status.sql
# 날짜 갱신: 원래 시드를 검증한 뒤 한 트랜잭션으로 처리
& $supabaseCli db query --linked --file supabase/refresh-demo-dates.sql
# 결과 검증
& $supabaseCli db query --linked --file supabase/demo-status.sql
& $supabaseCli db query --linked --file supabase/verify.sql
```

CLI 경로가 다르면 본인의 supabase 실행 파일 경로를 쓴다. 대시보드 SQL Editor에서도 같은 파일을 순서대로 실행할 수 있다. 원래 두 migration을 다시 실행하거나 migration 이력을 지우거나 db reset하지 않는다.

## 결과와 범위

- 최신 시드 충전 종료는 **어제 정오(한국 시간)**, 최신 시드 통과는 어제로 맞춘다. 충전 시작/종료를 같은 간격으로 이동해 충전 시간과 SoC/kW/결과를 유지한다. 리뷰 내용은 유지하고 작성 시각을 해당 세션 종료에 맞춘다.
- 크루즈 통과 날짜와 해제 시간을 같은 일수만큼 이동한다. 생성된 event_date는 DB가 다시 계산한다. 같은 한국 날짜에 다시 실행하면 날짜가 더 이동하지 않는다.
- 고정된 시드 소유자 230개(CHACHELIN 30/CRUISEBREAKER 200), 충전 300, 해제 400, 통과 2,000건인지 먼저 검증한다. SEED·owner_user_id 없음·demo-v1·ACTIVE를 모두 확인한다. 하나라도 다르면 전체를 중단하므로 임의로 조건을 완화하지 말고 시드 상태를 확인한다.
- 이 소유자들의 시연 기록만 변경한다. LIVE/SIMULATED 및 다른 소유자의 기록, 인증 정보, 설치·충전소·세션 ID, 후기 내용은 그대로 둔다. 날짜가 기본키인 크루즈는 선택된 행을 임시 보관한 후 같은 트랜잭션 안에서 다시 넣어 키 충돌을 피한다.
- 정상 갱신 후 demo-status는 latest_seed_pass_date=한국 날짜의 어제, seed_passes_28d=2000, seed_events_28d=400, seed_failures_7d>0을 반환한다. verify의 기존 시드 건수와 migration 이력은 유지된다. 앱을 debug로 실행해 목록/상세를 다시 열어 확인한다.
- 운영 스크립트이며 신규 migration이 아니다. 적용된 migration SQL과 함수 fingerprint는 변경하지 않는다. 이번 PR은 실행 방법과 로컬 검증을 추가한 것이며 원격 데이터 갱신은 실행하지 않았다.

로컬 검증: `node --test tests/database.test.mjs`가 시드를 60일 노후화한 뒤 갱신하여 위험도 복구, 최근 실패, 반복 실행, 실제 앱 데이터 보존, 불완전 시드의 중단을 확인한다.
