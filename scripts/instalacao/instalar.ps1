# ============================================================
#  Mecanica DBO - instala (ou atualiza) o sistema neste computador
#
#  Rode de dentro da pasta do pacote (a que veio no pendrive):
#    powershell -ExecutionPolicy Bypass -File .\instalar.ps1
#
#  Antes: Java 21 e PostgreSQL 18 instalados (veja LEIA-ME-INSTALACAO.md).
#
#  O que faz:
#   1. Pede as senhas (so na primeira vez) e guarda protegidas pelo Windows.
#   2. Copia o sistema para C:\MecanicaDBO.
#   3. Cria o banco a partir da copia que veio no pacote (so se ainda nao existir).
#   4. Agenda: ligar o sistema quando o Windows entrar + backup as 12h e 18h.
#   5. Cria o atalho "Mecanica DBO" na Area de Trabalho e abre o sistema.
#
#  Pode rodar de novo para ATUALIZAR: mantem as senhas e o banco.
#  Rode com o usuario do Windows que vai usar o sistema (nao precisa ser administrador).
# ============================================================

param(
    [string]$Destino = 'C:\MecanicaDBO',
    [string]$Banco   = 'mecanica_dbo',
    [int]   $PortaPg = 5432
)

$ErrorActionPreference = 'Stop'
$Pacote = $PSScriptRoot
. (Join-Path $Pacote 'comum.ps1')
$arquivoConfig = Join-Path $Destino 'config\sistema.dat'

function Passo([string]$texto) {
    Write-Host ''
    Write-Host "== $texto" -ForegroundColor Cyan
}

# Roda um programa do PostgreSQL; devolve a saida. Erro so se $exigir.
function Pg([string]$programa, [string[]]$argumentos, [switch]$exigir) {
    $ErrorActionPreference = 'Continue'
    $saida = & (Join-Path $script:pgBin "$programa.exe") @argumentos 2>&1 | Out-String
    $script:ultimoCodigo = $LASTEXITCODE
    if ($exigir -and $LASTEXITCODE -ne 0) { throw "$programa falhou (codigo $LASTEXITCODE): $($saida.Trim())" }
    return $saida.Trim()
}

function Pedir-Senha-Nova([string]$pergunta, [int]$minimo) {
    while ($true) {
        $s1 = Texto-De (Read-Host $pergunta -AsSecureString)
        $s2 = Texto-De (Read-Host 'Repita' -AsSecureString)
        if ($s1 -ne $s2) { Write-Host 'As duas nao sao iguais. De novo.' -ForegroundColor Red; continue }
        if ($s1.Length -lt $minimo) { Write-Host "Muito curta: minimo $minimo caracteres." -ForegroundColor Red; continue }
        return $s1
    }
}

function Pedir-Cpf-Chave {
    while ($true) {
        $chave = (Texto-De (Read-Host 'CPF_CHAVE (a do papel, 44 caracteres)' -AsSecureString)).Trim()
        try { $bytes = [Convert]::FromBase64String($chave) } catch { $bytes = @() }
        if ($bytes.Length -ne 32) {
            Write-Host 'Essa chave nao esta no formato certo (44 caracteres, terminando em =). Confira letra por letra.' -ForegroundColor Red
            continue
        }
        return $chave
    }
}

function Pedir-Senha-Postgres {
    for ($i = 1; $i -le 3; $i++) {
        $senha = Texto-De (Read-Host 'Senha do PostgreSQL (a que voce escolheu ao instalar o PostgreSQL)' -AsSecureString)
        $env:PGPASSWORD = $senha
        $ok = Pg 'psql' @('-h', 'localhost', '-p', "$PortaPg", '-U', 'postgres', '-w', '-d', 'postgres', '-tAc', 'select 1')
        if ($script:ultimoCodigo -eq 0 -and $ok -eq '1') { return $senha }
        Write-Host 'Nao consegui entrar no PostgreSQL com essa senha. Tente de novo.' -ForegroundColor Red
    }
    throw 'Senha do PostgreSQL errada 3 vezes. Confira a senha e rode o instalar.ps1 de novo.'
}

function Gravar-PgPass([string]$senha) {
    # Usado pelo backup automatico. Formato: servidor:porta:banco:usuario:senha
    $pasta = Join-Path $env:APPDATA 'postgresql'
    $arquivo = Join-Path $pasta 'pgpass.conf'
    New-Item -ItemType Directory -Force -Path $pasta | Out-Null
    $prefixo = "localhost:$($PortaPg):*:postgres:"
    $escapada = $senha.Replace('\', '\\').Replace(':', '\:')
    $linhas = @()
    if (Test-Path $arquivo) { $linhas = @(Get-Content $arquivo | Where-Object { -not $_.StartsWith($prefixo) }) }
    $linhas += ($prefixo + $escapada)
    Set-Content -Path $arquivo -Value $linhas -Encoding ASCII
}

function Parar-Sistema {
    Get-CimInstance Win32_Process -Filter "Name = 'java.exe' OR Name = 'javaw.exe'" |
        Where-Object { $_.CommandLine -like '*mecanica-dbo.jar*' } |
        ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
    for ($i = 0; $i -lt 20 -and (Sistema-NoAr); $i++) { Start-Sleep -Milliseconds 500 }
}

function Agendar-Tarefas {
    $usuario = "$env:USERDOMAIN\$env:USERNAME"
    $principal = New-ScheduledTaskPrincipal -UserId $usuario -LogonType Interactive -RunLevel Limited

    $acao = New-ScheduledTaskAction -Execute 'powershell.exe' `
        -Argument "-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$Destino\iniciar-sistema.ps1`""
    $config = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -ExecutionTimeLimit ([TimeSpan]::Zero)
    Register-ScheduledTask -TaskName 'Mecanica DBO - Sistema' -Action $acao -Principal $principal -Settings $config `
        -Trigger (New-ScheduledTaskTrigger -AtLogOn -User $usuario) -Force | Out-Null

    $acao = New-ScheduledTaskAction -Execute 'powershell.exe' `
        -Argument "-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$Destino\scripts\backup-banco.ps1`""
    $config = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable `
        -ExecutionTimeLimit (New-TimeSpan -Hours 1)
    $gatilhos = @((New-ScheduledTaskTrigger -Daily -At '12:00'), (New-ScheduledTaskTrigger -Daily -At '18:00'))
    Register-ScheduledTask -TaskName 'Backup Mecanica DBO' -Action $acao -Principal $principal -Settings $config `
        -Trigger $gatilhos -Force | Out-Null
}

function Criar-Atalho {
    $atalho = Join-Path ([Environment]::GetFolderPath('Desktop')) 'Mecanica DBO.lnk'
    $shell = New-Object -ComObject WScript.Shell
    $lnk = $shell.CreateShortcut($atalho)
    $lnk.TargetPath = 'powershell.exe'
    $lnk.Arguments = "-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$Destino\abrir-sistema.ps1`""
    $lnk.WorkingDirectory = $Destino
    $icone = Join-Path $Destino 'app\site\favicon.ico'
    if (Test-Path $icone) { $lnk.IconLocation = $icone }
    $lnk.Description = 'Abrir o sistema da Mecanica DBO'
    $lnk.Save()
}

try {
    if ((Resolve-Path $Pacote).Path.TrimEnd('\') -eq $Destino.TrimEnd('\')) {
        throw "Rode o instalar.ps1 da pasta do pendrive (ou de uma copia), nao de $Destino."
    }

    Passo 'Conferindo Java e PostgreSQL'
    $java = Achar-Java
    $script:pgBin = Achar-PgBin
    Write-Host "Java: $java"
    Write-Host "PostgreSQL: $script:pgBin"

    $config = Ler-Config $arquivoConfig
    if ($config) {
        Passo 'Sistema ja instalado: mantendo as senhas guardadas (atualizacao)'
        $env:PGPASSWORD = $config.DB_PASSWORD
    } else {
        Passo 'Senhas (so desta vez)'
        $senhaPg = Pedir-Senha-Postgres
        Write-Host ''
        Write-Host 'Agora a senha de LOGIN do sistema (usuario: admin). Escolha uma nova e anote no papel.'
        $senhaApp = Pedir-Senha-Nova 'Senha do login (minimo 8 caracteres)' 8
        Write-Host ''
        $chave = Pedir-Cpf-Chave
        $config = [pscustomobject]@{ APP_PASSWORD = $senhaApp; CPF_CHAVE = $chave; DB_PASSWORD = $senhaPg }
        Gravar-Config $arquivoConfig $config
        Gravar-PgPass $senhaPg
        Write-Host 'Senhas guardadas (protegidas pelo Windows, so abrem neste usuario).' -ForegroundColor Green
    }

    Passo "Copiando o sistema para $Destino"
    Parar-Sistema
    $app = Join-Path $Destino 'app'
    if (Test-Path $app) { Remove-Item $app -Recurse -Force }   # tira as telas da versao anterior
    New-Item -ItemType Directory -Force -Path $Destino, (Join-Path $Destino 'scripts'), (Join-Path $Destino 'logs') | Out-Null
    Copy-Item (Join-Path $Pacote 'app') $Destino -Recurse -Force
    Copy-Item (Join-Path $Pacote 'scripts\*') (Join-Path $Destino 'scripts') -Force
    foreach ($nome in 'comum.ps1', 'iniciar-sistema.ps1', 'abrir-sistema.ps1', 'LEIA-ME-INSTALACAO.md') {
        Copy-Item (Join-Path $Pacote $nome) $Destino -Force
    }

    Passo 'Banco de dados'
    $existe = Pg 'psql' @('-h', 'localhost', '-p', "$PortaPg", '-U', 'postgres', '-w', '-d', 'postgres', '-tAc',
                          "select 1 from pg_database where datname = '$Banco'") -exigir
    if ($existe -eq '1') {
        Write-Host "O banco '$Banco' ja existe: os dados ficam como estao."
    } else {
        $copia = Get-ChildItem (Join-Path $Pacote 'banco') -Filter '*.backup' -ErrorAction SilentlyContinue |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1
        Pg 'createdb' @('-h', 'localhost', '-p', "$PortaPg", '-U', 'postgres', '-w', $Banco) -exigir | Out-Null
        if ($copia) {
            Write-Host "Restaurando $($copia.Name)..."
            Pg 'pg_restore' @('-h', 'localhost', '-p', "$PortaPg", '-U', 'postgres', '-w', '--no-owner', '-d', $Banco, $copia.FullName) -exigir | Out-Null
            Write-Host 'Banco criado com os dados do pacote.' -ForegroundColor Green
        } else {
            Write-Host 'Banco criado vazio (o sistema cria as tabelas ao ligar).' -ForegroundColor Green
        }
    }
    $env:PGPASSWORD = $null

    Passo 'Agendando: ligar com o Windows + backup as 12h e 18h'
    Agendar-Tarefas
    Criar-Atalho
    Write-Host 'Atalho "Mecanica DBO" criado na Area de Trabalho.'

    Passo 'Ligando o sistema (pode levar ate 1 minuto)'
    & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $Destino 'abrir-sistema.ps1')
    if ($LASTEXITCODE -ne 0) { throw "O sistema nao ligou. Veja $Destino\logs\sistema.log" }

    Passo 'Pronto!'
    Write-Host 'O sistema abriu no navegador. Entre com o usuario admin e a senha do login.' -ForegroundColor Green
    Write-Host 'Proximos passos (LEIA-ME-INSTALACAO.md): backup no Google Drive e apagar a pasta do pendrive.'
    exit 0
}
catch {
    $env:PGPASSWORD = $null
    Write-Host ''
    Write-Host "ERRO: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
