# ============================================================
#  Mecanica DBO - liga o sistema (sem janela)
#
#  Roda sozinho quando o Windows entra (tarefa "Mecanica DBO - Sistema").
#  Le as senhas guardadas pelo instalar.ps1 e liga o backend, que tambem
#  entrega as telas em http://127.0.0.1:8080.
#
#  Registro: C:\MecanicaDBO\logs\sistema.log
# ============================================================

$ErrorActionPreference = 'Stop'
$Pasta = $PSScriptRoot
. (Join-Path $Pasta 'comum.ps1')

$pastaLogs = Join-Path $Pasta 'logs'
New-Item -ItemType Directory -Force -Path $pastaLogs | Out-Null

function Escrever-Log([string]$mensagem) {
    $linha = '{0:yyyy-MM-dd HH:mm:ss}  {1}' -f (Get-Date), $mensagem
    Add-Content -Path (Join-Path $pastaLogs 'inicializacao.log') -Value $linha -Encoding UTF8
}

try {
    if (Sistema-NoAr) { exit 0 }   # ja esta ligado

    $config = Ler-Config (Join-Path $Pasta 'config\sistema.dat')
    if (-not $config) { throw 'Configuracao nao encontrada. Rode o instalar.ps1 de novo.' }

    $env:APP_PASSWORD           = $config.APP_PASSWORD
    $env:CPF_CHAVE              = $config.CPF_CHAVE
    $env:DB_PASSWORD            = $config.DB_PASSWORD
    $env:SPRING_PROFILES_ACTIVE = 'prod'
    $env:APP_SITE               = ($Pasta -replace '\\', '/') + '/app/site/'
    $env:APP_LOG                = ($Pasta -replace '\\', '/') + '/logs/sistema.log'

    $java = Achar-Java
    Start-Process -FilePath $java `
        -ArgumentList @('-Xmx512m', '-jar', "`"$(Join-Path $Pasta 'app\mecanica-dbo.jar')`"") `
        -WorkingDirectory $Pasta -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $pastaLogs 'console.log') `
        -RedirectStandardError (Join-Path $pastaLogs 'console-erros.log') | Out-Null

    Escrever-Log 'Sistema iniciado.'
    exit 0
}
catch {
    Escrever-Log "ERRO: $($_.Exception.Message)"
    exit 1
}
