# ============================================================
#  Mecanica DBO - backup do banco PostgreSQL
#
#  Gera <banco>_AAAA-MM-DD_HHmm.backup (pg_dump formato custom, comprimido),
#  grava o resultado em backup.log e apaga backups com mais de 30 dias.
#
#  A senha NAO fica aqui: vem do %APPDATA%\postgresql\pgpass.conf.
#  Veja scripts\LEIA-ME-backup.md.
#
#  Uso:
#    powershell -ExecutionPolicy Bypass -File scripts\backup-banco.ps1
#    powershell -ExecutionPolicy Bypass -File scripts\backup-banco.ps1 -PastaDestino D:\backups
#
#  (Texto sem acentos de proposito: o PowerShell 5.1 do Windows le .ps1
#   sem BOM como ANSI e estragaria os acentos.)
# ============================================================

param(
    [string]$PastaDestino = 'C:\MecanicaDBO\backups',
    [string]$Banco        = 'mecanica_dbo',
    [string]$Servidor     = 'localhost',
    [int]   $Porta        = 5432,
    [string]$Usuario      = 'postgres',
    [string]$PgBin        = '',
    [int]   $DiasRetencao = 30
)

$ErrorActionPreference = 'Stop'

function Escrever-Log([string]$mensagem) {
    $linha = '{0:yyyy-MM-dd HH:mm:ss}  {1}' -f (Get-Date), $mensagem
    Write-Host $linha
    Add-Content -Path (Join-Path $PastaDestino 'backup.log') -Value $linha -Encoding UTF8
}

function Achar-PgDump {
    if ($PgBin) {
        $exe = Join-Path $PgBin 'pg_dump.exe'
        if (-not (Test-Path $exe)) { throw "pg_dump.exe nao encontrado em $PgBin" }
        return $exe
    }
    # Versao mais nova instalada (pasta com o maior numero).
    $achado = Get-ChildItem 'C:\Program Files\PostgreSQL\*\bin\pg_dump.exe' -ErrorAction SilentlyContinue |
        Sort-Object { [int]($_.Directory.Parent.Name -replace '\D', '') } -Descending |
        Select-Object -First 1
    if (-not $achado) { throw 'pg_dump.exe nao encontrado em C:\Program Files\PostgreSQL\*\bin. Informe -PgBin.' }
    return $achado.FullName
}

New-Item -ItemType Directory -Force -Path $PastaDestino | Out-Null

try {
    $pgDump  = Achar-PgDump
    $arquivo = Join-Path $PastaDestino ('{0}_{1:yyyy-MM-dd_HHmm}.backup' -f $Banco, (Get-Date))

    Escrever-Log "Iniciando backup de '$Banco' em $arquivo"

    # -w: nunca pergunta senha (no Agendador de Tarefas ficaria travado esperando).
    $ErrorActionPreference = 'Continue'
    $saida = & $pgDump -h $Servidor -p $Porta -U $Usuario -w -Fc -f $arquivo $Banco 2>&1 | Out-String
    $codigo = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'

    if ($codigo -ne 0) {
        if (Test-Path $arquivo) { Remove-Item $arquivo -Force }
        throw "pg_dump terminou com codigo $codigo. Saida: $($saida.Trim())"
    }
    if (-not (Test-Path $arquivo) -or (Get-Item $arquivo).Length -eq 0) {
        throw 'pg_dump nao gerou o arquivo (ou gerou vazio).'
    }

    $tamanhoKb = [math]::Round((Get-Item $arquivo).Length / 1KB, 1)
    Escrever-Log "OK: backup concluido ($tamanhoKb KB)."

    # Limpeza so depois de um backup bem-sucedido: se o backup falhar por
    # semanas, os antigos continuam la.
    $limite = (Get-Date).AddDays(-$DiasRetencao)
    Get-ChildItem -Path $PastaDestino -Filter "$($Banco)_*.backup" |
        Where-Object { $_.LastWriteTime -lt $limite } |
        ForEach-Object {
            Remove-Item $_.FullName -Force
            Escrever-Log "Removido backup antigo: $($_.Name)"
        }

    exit 0
}
catch {
    Escrever-Log "ERRO: $($_.Exception.Message)"
    exit 1
}
