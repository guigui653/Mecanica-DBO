# ============================================================
#  Mecanica DBO - restaura um .backup num banco de OUTRO nome
#
#  Por seguranca, recusa restaurar em 'mecanica_dbo' (o banco real).
#  Para voltar o banco real a partir de um backup, veja o LEIA-ME-backup.md:
#  e um procedimento manual, de proposito.
#
#  Uso:
#    powershell -ExecutionPolicy Bypass -File scripts\restaurar-banco.ps1 `
#        -Arquivo C:\MecanicaDBO\backups\mecanica_dbo_2026-10-29_1830.backup `
#        -BancoDestino mecanica_dbo_copia
#
#  Se o banco destino ja existir, o script para. Use -Substituir para
#  apagar e recriar o banco destino (nunca o mecanica_dbo).
#
#  (Texto sem acentos de proposito: veja o comentario no backup-banco.ps1.)
# ============================================================

param(
    [Parameter(Mandatory = $true)][string]$Arquivo,
    [Parameter(Mandatory = $true)][string]$BancoDestino,
    [switch]$Substituir,
    [string]$Servidor = 'localhost',
    [int]   $Porta    = 5432,
    [string]$Usuario  = 'postgres',
    [string]$PgBin    = ''
)

$ErrorActionPreference = 'Stop'

$PROTEGIDOS = @('mecanica_dbo', 'postgres', 'template0', 'template1')

function Achar-Bin {
    if ($PgBin) { return $PgBin }
    $achado = Get-ChildItem 'C:\Program Files\PostgreSQL\*\bin\pg_restore.exe' -ErrorAction SilentlyContinue |
        Sort-Object { [int]($_.Directory.Parent.Name -replace '\D', '') } -Descending |
        Select-Object -First 1
    if (-not $achado) { throw 'pg_restore.exe nao encontrado em C:\Program Files\PostgreSQL\*\bin. Informe -PgBin.' }
    return $achado.DirectoryName
}

function Rodar([string]$exe, [string[]]$argumentos) {
    $ErrorActionPreference = 'Continue'
    $saida = & $exe @argumentos 2>&1 | Out-String
    $codigo = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    return @{ Codigo = $codigo; Saida = $saida.Trim() }
}

try {
    if ($PROTEGIDOS -contains $BancoDestino.ToLower()) {
        throw "Recusado: '$BancoDestino' e um banco protegido. Restaure com outro nome (ex.: mecanica_dbo_copia)."
    }
    if ($BancoDestino -notmatch '^[a-z0-9_]+$') {
        throw "Nome de banco invalido: '$BancoDestino'. Use so letras minusculas, numeros e _."
    }
    if (-not (Test-Path $Arquivo)) { throw "Arquivo nao encontrado: $Arquivo" }

    $bin  = Achar-Bin
    $conn = @('-h', $Servidor, '-p', "$Porta", '-U', $Usuario, '-w')

    $r = Rodar (Join-Path $bin 'psql.exe') ($conn + @('-d', 'postgres', '-At', '-c',
        "SELECT 1 FROM pg_database WHERE datname = '$BancoDestino'"))
    if ($r.Codigo -ne 0) { throw "Nao consegui conectar no PostgreSQL: $($r.Saida)" }
    $existe = $r.Saida -eq '1'

    if ($existe -and -not $Substituir) {
        throw "O banco '$BancoDestino' ja existe. Rode de novo com -Substituir para apagar e recriar."
    }
    if ($existe) {
        Write-Host "Apagando o banco '$BancoDestino'..."
        $r = Rodar (Join-Path $bin 'dropdb.exe') ($conn + @($BancoDestino))
        if ($r.Codigo -ne 0) { throw "dropdb falhou: $($r.Saida)" }
    }

    Write-Host "Criando o banco '$BancoDestino'..."
    $r = Rodar (Join-Path $bin 'createdb.exe') ($conn + @($BancoDestino))
    if ($r.Codigo -ne 0) { throw "createdb falhou: $($r.Saida)" }

    Write-Host "Restaurando $Arquivo..."
    $r = Rodar (Join-Path $bin 'pg_restore.exe') ($conn + @('-d', $BancoDestino, '--no-owner', $Arquivo))
    if ($r.Codigo -ne 0) { throw "pg_restore terminou com codigo $($r.Codigo): $($r.Saida)" }

    Write-Host "OK: '$BancoDestino' restaurado a partir de $Arquivo"
    exit 0
}
catch {
    Write-Host "ERRO: $($_.Exception.Message)"
    exit 1
}
