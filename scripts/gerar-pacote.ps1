# ============================================================
#  Mecanica DBO - gera o pacote de instalacao (rode no SEU PC)
#
#  Monta a pasta pacote\MecanicaDBO com tudo o que o computador da
#  oficina precisa: o sistema pronto (backend + telas), os scripts de
#  backup, o instalador e uma copia NOVA do banco.
#  Depois e so copiar essa pasta para o pendrive.
#
#  Precisa, neste PC: JDK 21 (o do IntelliJ serve), Node.js e o
#  PostgreSQL (para o backup do banco).
#
#  Uso (na pasta do projeto):
#    powershell -ExecutionPolicy Bypass -File scripts\gerar-pacote.ps1
#    powershell -ExecutionPolicy Bypass -File scripts\gerar-pacote.ps1 -SemBanco
#
#  -SemBanco: o pacote vai sem copia do banco (atualizacao de um
#  computador que ja tem o sistema; os dados de la nao mudam).
# ============================================================

param(
    [switch]$SemBanco,
    [string]$PastaBackups = 'C:\MecanicaDBO\backups'
)

$ErrorActionPreference = 'Stop'
$raiz  = Split-Path $PSScriptRoot -Parent
$saida = Join-Path $raiz 'pacote\MecanicaDBO'

function Passo([string]$texto) {
    Write-Host ''
    Write-Host "== $texto" -ForegroundColor Cyan
}

function Rodar([string]$exe, [string[]]$argumentos, [string]$pasta, [string]$etapa) {
    Push-Location $pasta
    try {
        & $exe @argumentos
        if ($LASTEXITCODE -ne 0) { throw "$etapa falhou (codigo $LASTEXITCODE). Veja as mensagens acima." }
    } finally {
        Pop-Location
    }
}

try {
    Passo 'Conferindo o Java (JDK 21)'
    if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) {
        # O IntelliJ guarda os JDKs que baixa em C:\Users\<voce>\.jdks
        $jdk = Get-ChildItem (Join-Path $env:USERPROFILE '.jdks') -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match '21' -and (Test-Path (Join-Path $_.FullName 'bin\javac.exe')) } |
            Select-Object -First 1
        if (-not $jdk) { throw 'JDK 21 nao encontrado. Defina a variavel JAVA_HOME com a pasta do JDK 21.' }
        $env:JAVA_HOME = $jdk.FullName
    }
    Write-Host "JDK: $env:JAVA_HOME"

    if (-not (Get-Command npm -ErrorAction SilentlyContinue)) { throw 'Node.js (npm) nao encontrado. Instale com: winget install OpenJS.NodeJS.LTS' }

    Passo 'Compilando o backend e rodando os testes (alguns minutos na primeira vez)'
    Rodar (Join-Path $raiz 'mecanica-dbo-v2\mvnw.cmd') @('-q', 'clean', 'package') (Join-Path $raiz 'mecanica-dbo-v2') 'A compilacao do backend'
    $jar = Get-ChildItem (Join-Path $raiz 'mecanica-dbo-v2\target') -Filter '*.jar' |
        Where-Object { $_.Name -notlike '*.original' } | Select-Object -First 1
    if (-not $jar) { throw 'O .jar do backend nao foi gerado.' }

    Passo 'Gerando as telas (Angular)'
    $front = Join-Path $raiz 'mecanica-dbo-front'
    Rodar 'npm' @('ci', '--no-audit', '--no-fund') $front 'A instalacao das dependencias do frontend'
    Rodar 'npx' @('ng', 'build') $front 'O build do frontend'
    $site = Join-Path $front 'dist\mecanica-dbo-front\browser'
    if (-not (Test-Path (Join-Path $site 'index.html'))) { throw "As telas nao foram geradas em $site." }

    $backup = $null
    if (-not $SemBanco) {
        Passo 'Fazendo um backup novo do banco'
        & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'backup-banco.ps1') -PastaDestino $PastaBackups
        if ($LASTEXITCODE -ne 0) { throw 'O backup do banco falhou. Veja C:\MecanicaDBO\backups\backup.log.' }
        $backup = Get-ChildItem $PastaBackups -Filter 'mecanica_dbo_*.backup' |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1
    }

    Passo "Montando o pacote em $saida"
    if (Test-Path $saida) {
        # So apaga se for mesmo um pacote gerado por este script.
        if (-not (Test-Path (Join-Path $saida 'instalar.ps1'))) { throw "$saida existe e nao parece um pacote. Apague ou mova a pasta na mao." }
        Remove-Item $saida -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path (Join-Path $saida 'app\site'), (Join-Path $saida 'scripts') | Out-Null

    Copy-Item $jar.FullName (Join-Path $saida 'app\mecanica-dbo.jar')
    Copy-Item (Join-Path $site '*') (Join-Path $saida 'app\site') -Recurse
    foreach ($nome in 'backup-banco.ps1', 'backup-nuvem.ps1', 'configurar-senha-backup.ps1', 'restaurar-banco.ps1', 'LEIA-ME-backup.md') {
        Copy-Item (Join-Path $PSScriptRoot $nome) (Join-Path $saida 'scripts')
    }
    Copy-Item (Join-Path $PSScriptRoot 'instalacao\*') $saida

    if ($backup) {
        New-Item -ItemType Directory -Force -Path (Join-Path $saida 'banco') | Out-Null
        Copy-Item $backup.FullName (Join-Path $saida 'banco')
    }

    Passo 'Pronto'
    Write-Host "Pacote: $saida" -ForegroundColor Green
    if ($backup) {
        Write-Host "Banco incluido: $($backup.Name)"
        Write-Host 'ATENCAO: o pacote tem os dados dos clientes. Depois de instalar, apague a pasta do pendrive.' -ForegroundColor Yellow
    } else {
        Write-Host 'Sem copia do banco (atualizacao).'
    }
    Write-Host 'Copie a pasta MecanicaDBO inteira para o pendrive e siga o LEIA-ME-INSTALACAO.md.'
    exit 0
}
catch {
    Write-Host ''
    Write-Host "ERRO: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
