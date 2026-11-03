# 이 PC의 Supabase 터미널 연결

2026-11-02: 공식 GitHub supabase/cli v2.122.0 Windows amd64 zip을 `%LOCALAPPDATA%/hyundai-dev/supabase`에 설치하고 공식 checksums.txt의 SHA-256과 일치함을 확인했다. 시스템 PATH·전역 패키지·방화벽 설정은 변경하지 않았다. `supabase init`으로 이 디렉터리의 config.toml을 생성했으며 원격 설정은 변경하지 않았다.

**연결 완료:** 사용자가 PowerShell에서 login/link 완료 후 에이전트도 원격 SQL 조회에 성공했다. 두 수동 적용 버전의 실제 반영과 함수 내용 일치를 검증한 뒤 표준 이력을 repair했고, migration list의 LOCAL/REMOTE 두 행 일치 및 db push --dry-run의 upToDate=true를 확인했다. 아래 로그인·최초 이력 정합 절차는 이 PC에서 다시 실행할 필요 없다.

## 사용자가 한 번 진행할 것

일반 PowerShell에서:

```powershell
Set-Location 'C:\Users\ghkdw\dev\hyndai\hyundaiapp-chachelin\server'
$supabaseCli = Join-Path $env:LOCALAPPDATA 'hyundai-dev\supabase\supabase.exe'
& $supabaseCli login
```

열리는 브라우저에서 Supabase 로그인/CLI 연결 절차를 직접 완료한다. 터미널에서 확인 코드를 요구하면 로컬 터미널에 입력한다. 토큰·코드·비밀번호를 채팅에 보낼 필요는 없다.

이후 같은 PowerShell에서:

```powershell
& $supabaseCli link --project-ref qbdikckaqroxbpacvtqo
```

프로젝트 생성 때 정한 DB 비밀번호를 요구하면 프롬프트에 직접 입력한다. CLI가 비밀번호를 생략할 수 있다고 표시하면, 우선 Management API 기반 SQL 조회만 연결할 경우 빈 입력으로 생략할 수 있다. 별도 원격 터미널/SSH/포트 개방과 Docker 설치는 필요 없다.

`Finished supabase login`, `Finished supabase link` 또는 그에 해당하는 성공 메시지를 확인한 뒤 연결됐다고 알려 준다. 계정 토큰과 DB 비밀번호는 로컬 CLI 자격 증명 저장소에 맡기며 Git이나 명령줄 인수에 넣지 않는다. 공식 CLI는 네이티브 자격 증명 저장을 우선 사용하고, 사용할 수 없으면 사용자 홈의 파일로 대체할 수 있다.

## 연결 후 에이전트가 진행할 것

1. 아래처럼 Management API 경유로 파일을 실행해 기존 적용 버전·테이블·시드 수·권한 확인.

```powershell
& $supabaseCli db query --linked --project-ref qbdikckaqroxbpacvtqo --file supabase/verify.sql
```

2. **확인 후에만** 수동 적용된 두 migration을 CLI 표준 이력에 applied로 등록한다. 자체 pleos_private.migration_versions는 CLI 표준 supabase_migrations.schema_migrations와 별개이므로 자동으로 이력이 맞지 않는다.

```powershell
& $supabaseCli migration list --linked
# 원격 상태와 파일 내용의 적용을 검증한 뒤 실행할 명령:
& $supabaseCli migration repair 20261102000100 20261102000200 --status applied --linked
& $supabaseCli migration list --linked
& $supabaseCli db push --linked --dry-run
```

3. 다음 변경부터 새 migration을 만들고 테스트 후 dry-run과 db push로 적용한다. 임의 SQL 조회도 db query --file로 실행할 수 있다.

initial SQL 재실행, `db reset`, `config push`는 하지 않는다. 특히 config.toml은 CLI 기본값으로 생성한 로컬 설정이며 원격 Auth 설정을 덮어쓰는 용도로 검토하지 않았다. 연결 후 원격 변경은 migration 이력 등록 2건뿐이며 기존 업무 데이터는 변경하지 않았다.

공식 문서: [로그인·연결](https://supabase.com/docs/reference/cli/supabase-login), [migration 이력 정합](https://supabase.com/docs/reference/cli/supabase-migration-repair).
