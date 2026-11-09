# 차슐랭 (Chachelin)

차량이 직접 확인한 충전 기록으로 충전소를 평가하는 Pleos Connect 앱. 팀 삼각고래 · 일반 부문.

- 기획: [PRD.md](PRD.md)
- **시연 순서(확정)**: [docs/demo-scenario.md](docs/demo-scenario.md) — 이 순서대로 누르면 그대로 동작해야 한다
- 서버·인증·데이터 흐름·운영: [공통 서버 설명서](docs/server-overview.md)
- 핵심 신호: Vehicle SDK EV 배터리(SoC·충전 상태·충전구), NaviHelper 위치·충전 사업자·길안내
- 실측 메모: [docs/vehicle-data-findings.md](docs/vehicle-data-findings.md) (에뮬레이터에서 SoC는 자동으로 오르지 않음, 콘솔로 주입)

## 심사용 안내 (GitHub 제출물)

| 항목 | 내용 |
|---|---|
| 앱 이름 | 차슐랭 (Chachelin) — 팀 삼각고래, 일반 부문 |
| 한 줄 소개 | 차량이 직접 확인한 충전 기록(SoC·충전구·시간)으로 충전소를 평가해, "사용 가능"이라던 충전기 앞에서 허탕 치는 일을 줄이는 정차 전용 Pleos Connect 앱 |
| APK 위치 | `apk/SamgakGorae_Chachelin_release.apk` (서명된 release 빌드, 시연 데이터 모드·시연 판정 기준, 11-09 빌드) |
| 패키지 | `com.hyundaiapp.chachelin` |
| 기획서 | [docs/proposal.md](docs/proposal.md) (접수양식 02 항목과 같은 내용), 제품 요구사항 [PRD.md](PRD.md) |
| 시연 순서 | [docs/demo-scenario.md](docs/demo-scenario.md) — 약 3분, 이 순서대로 누르면 그대로 동작한다 |

### 실행 방법

1. Pleos Connect 에뮬레이터 v2.1.0을 켠다(시스템 이미지 `Pleos Connect v2.1.0 APIs`, AVD 프로필 `Pleos_Connect_v2_0` 2560×1440). 에뮬레이터 사용자 ID는 10이다.
2. APK를 설치한다: `adb install -r apk/SamgakGorae_Chachelin_release.apk`
3. 앱을 켜면 **S7 첫 실행 동의** → 위치정보 [동의]·개인정보 [동의]·차종 선택·[시작하기] → **홈** → [근처 충전소](또는 왼쪽 이동 막대 [충전소]) → **S1 근처 충전소**(시드 충전소 10곳). 처음부터 다시 보려면 `adb shell pm clear --user 10 com.hyundaiapp.chachelin`
4. 충전 장면은 에뮬레이터 Virtual Console로 차량 값을 주입해 재현한다(Git Bash, 저장소 루트):
   - `bash tools/vconsole.sh up` (포트 포워딩·하트비트 시작)
   - `bash tools/demo-charge.sh success` → 충전구 열림 → SoC 40→80% → 닫힘 → **S3 충전 기록 중 → S4 인증 충전 완료·평가**
   - `bash tools/demo-charge.sh fail` → SoC 변화 없이 닫힘 → **S5 충전 실패 기록**
   - 끝나면 `bash tools/vconsole.sh down`
5. 시연 고지: 에뮬레이터는 충전 중 SoC가 자동으로 오르지 않아 콘솔로 주입하고, 화면의 충전 시간·kW는 시연 배속(×120, 15초 = 30분)이며, 충전소·기록은 시연용 시드 데이터(판교 반경 3km 10곳)다. 영상·PT에서도 같은 내용을 밝힌다.

### 테스트 환경 및 필요한 설정

- 확인한 환경: Windows 11, Android Studio 2025.3.2(번들 JDK 21; JDK 17 이상), Android Gradle Plugin 8.9.2, Kotlin 2.1.20, Android Emulator 36.4, Pleos Connect 에뮬레이터 v2.1.0(API 34 `pleos_car_emulator`)
- 서버: Supabase 시연 프로젝트. 설정값은 APK에 들어 있어 **추가 설정 없이** 실행되며, 인터넷 연결이 필요하다. 서버에 닿지 않으면 기록은 앱 안의 대기열에 남았다가 다시 보낸다. 설정값·동작은 [docs/server-overview.md](docs/server-overview.md)
- 권한: `pleos.car.permission.CAR_ENERGY`(SoC·충전 상태·충전구), `NAVI_ROUTE`·`NAVI_ROUTE_SEARCH`(위치·충전소 검색·길안내), `INTERNET`, `FOREGROUND_SERVICE`(`specialUse`, 충전 기록 서비스), `RECEIVE_BOOT_COMPLETED`, `POST_NOTIFICATIONS`. 설치 후 별도의 adb 권한 부여는 필요 없다
- 주행 중 제한: DO(`distractionOptimized`)를 선언하지 않는 **정차 전용** 앱이라 주행 중에는 플랫폼이 화면을 막는다
- 소스에서 빌드: 저장소 루트의 [`supabase-public.properties`](supabase-public.properties) 두 줄을 `local.properties`에 복사한 뒤 `./gradlew assembleDebug` / `./gradlew installDebug`. 복사하지 않으면 서버 없이 Fake 저장소로 돌아가며 화면 흐름은 같다. 단위 테스트는 `./gradlew test`(core·app). release 빌드는 `bash tools/build-release.sh`(서명 키 필요)
- debug 빌드와 제출 release 빌드는 화면 흐름·판정 기준이 같다(둘 다 시연 데이터 모드). debug에만 홈 제목 "차슐랭"이나 S1 제목을 길게 누르면 열리는 개발 메뉴(신호 모니터)가 있다

### 오픈소스 고지

| 구성 요소 | 라이선스 |
|---|---|
| AndroidX (Jetpack Compose BOM·Material 3, Activity Compose, Lifecycle) | Apache License 2.0 |
| Kotlin 2.1.20, kotlinx-coroutines 1.9.0 | Apache License 2.0 |
| Pleos Playground SDK (Vehicle 2.3.2, NaviHelper 3.0.5) | 현대자동차그룹 Pleos Connect 개발 환경 제공, 공모전·개발자 약관에 따름 |
| 테스트 전용: JUnit 4.13.2 (EPL 1.0), org.json 20240303 (Public Domain) | — |

앱 소스 코드와 문서의 권리는 팀 삼각고래에 있으며 공모전 약관(지식재산권 조항)을 따른다. 위 구성 요소 외에 다른 서드파티 코드는 포함하지 않았다.

## 팀 개발 안내

팀 내부 개발 절차(처음 시작하기, 코드 구조, 담당별 첫 작업, 규칙)는 [docs/dev-guide.md](docs/dev-guide.md)에 있다.
