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
#   3. Cria o banco a partir da copia que veio no pacote (so se ainda nao existir),
#      com um usuario proprio no PostgreSQL ('mecanica'), que so mexe nesse banco.
#   4. Agenda: ligar o sistema quando o Windows entrar + backup as 12h e 18h.
#   5. Cria o atalho "Mecanica DBO" na Area de Trabalho e abre o sistema.
#
#  Pode rodar de novo para ATUALIZAR: mantem as senhas e o banco, e faz
#  um backup antes de trocar o sistema.
#  Rode com o usuario do Windows que vai usar o sistema (nao precisa ser administrador).
# ============================================================

param(
    [string]$Destino = 'C:\MecanicaDBO',
    [string]$Banco   = 'mecanica_dbo',
    [int]   $PortaPg = 5432,
    [string]$UsuarioBanco = 'mecanica'
)

$ErrorActionPreference = 'Stop'
$Pacote = $PSScriptRoot
. (Join-Path $Pacote 'comum.ps1')
$arquivoConfig = Join-Path $Destino 'config\sistema.dat'

# Roda um programa do PostgreSQL; devolve a saida. Erro so se $exigir.
function Pg([string]$programa, [string[]]$argumentos, [switch]$exigir) {
    $ErrorActionPreference = 'Continue'
    $saida = & (Join-Path $script:pgBin "$programa.exe") @argumentos 2>&1 | Out-String
    $script:ultimoCodigo = $LASTEXITCODE
    if ($exigir -and $LASTEXITCODE -ne 0) { throw "$programa falhou (codigo $LASTEXITCODE): $($saida.Trim())" }
    return $saida.Trim()
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

function Senha-Aleatoria {
    # 32 caracteres hexadecimais. Ninguem precisa digitar: fica so na config protegida.
    $bytes = New-Object byte[] 16
    [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    return -join ($bytes | ForEach-Object { $_.ToString('x2') })
}

function Psql-Postgres([string]$sql) {
    return Pg 'psql' @('-h', 'localhost', '-p', "$PortaPg", '-U', 'postgres', '-w', '-d', 'postgres', '-tAc', $sql) -exigir
}

function Garantir-Usuario-Banco([string]$senha) {
    # A senha e hexadecimal: nao tem aspas, entao pode ir direto no SQL.
    $existe = Psql-Postgres "select 1 from pg_roles where rolname = '$UsuarioBanco'"
    if ($existe -eq '1') {
        Psql-Postgres "ALTER ROLE $UsuarioBanco LOGIN PASSWORD '$senha'" | Out-Null
    } else {
        Psql-Postgres "CREATE ROLE $UsuarioBanco LOGIN PASSWORD '$senha'" | Out-Null
    }
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
        # O acesso como 'postgres' vem do pgpass.conf gravado na instalacao.

        Passo 'Backup antes de atualizar'
        # Se a versao nova der problema, os dados de um minuto antes estao guardados.
        $backupAntes = Join-Path $Pacote 'scripts\backup-banco.ps1'   # o do pacote (versao nova)
        & powershell -NoProfile -ExecutionPolicy Bypass -File $backupAntes
        if ($LASTEXITCODE -eq 2) {
            Write-Host 'Backup local OK; a copia da nuvem falhou (veja o backup.log). Seguindo com a atualizacao.' -ForegroundColor Yellow
        } elseif ($LASTEXITCODE -ne 0) {
            throw 'O backup antes da atualizacao falhou, entao nada foi trocado. Veja C:\MecanicaDBO\backups\backup.log.'
        }
    } else {
        Passo 'Senhas (so desta vez)'
        $senhaPg = Pedir-Senha-Postgres
        Write-Host ''
        Write-Host 'Agora a senha de LOGIN do sistema (usuario: admin). Escolha uma nova e anote no papel.'
        Write-Host 'Dica: tres palavras com numero, ex.: Oleo-Freio-Pneu-77'
        $senhaApp = Pedir-Senha-Nova 'Senha do login (minimo 10 caracteres)' 10
        Write-Host ''
        $chave = Pedir-Cpf-Chave
        Gravar-PgPass $senhaPg   # usado pelo backup automatico
    }

    Passo "Copiando o sistema para $Destino"
    Parar-Sistema
    $app = Join-Path $Destino 'app'
    if (Test-Path $app) { Remove-Item $app -Recurse -Force }   # tira as telas da versao anterior
    New-Item -ItemType Directory -Force -Path $Destino, (Join-Path $Destino 'scripts'), (Join-Path $Destino 'logs') | Out-Null
    Copy-Item (Join-Path $Pacote 'app') $Destino -Recurse -Force
    Copy-Item (Join-Path $Pacote 'scripts\*') (Join-Path $Destino 'scripts') -Force
    foreach ($nome in 'comum.ps1', 'iniciar-sistema.ps1', 'abrir-sistema.ps1', 'trocar-senha-login.ps1', 'LEIA-ME-INSTALACAO.md') {
        Copy-Item (Join-Path $Pacote $nome) $Destino -Force
    }

    Passo 'Banco de dados'
    $senhaBanco = $null
    if (-not $config) {
        $senhaBanco = Senha-Aleatoria
        Garantir-Usuario-Banco $senhaBanco
    }
    $existe = Psql-Postgres "select 1 from pg_database where datname = '$Banco'"
    if ($existe -eq '1') {
        Write-Host "O banco '$Banco' ja existe: os dados ficam como estao."
    } else {
        $copia = Get-ChildItem (Join-Path $Pacote 'banco') -Filter '*.backup' -ErrorAction SilentlyContinue |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1
        Pg 'createdb' @('-h', 'localhost', '-p', "$PortaPg", '-U', 'postgres', '-w', '-O', $UsuarioBanco, $Banco) -exigir | Out-Null
        if ($copia) {
            Write-Host "Restaurando $($copia.Name)..."
            # --role: as tabelas ficam do usuario 'mecanica', que o sistema usa.
            Pg 'pg_restore' @('-h', 'localhost', '-p', "$PortaPg", '-U', 'postgres', '-w', '--no-owner',
                              "--role=$UsuarioBanco", '-d', $Banco, $copia.FullName) -exigir | Out-Null
            Write-Host 'Banco criado com os dados do pacote.' -ForegroundColor Green
        } else {
            Write-Host 'Banco criado vazio (o sistema cria as tabelas ao ligar).' -ForegroundColor Green
        }
    }

    if (-not $config) {
        # Banco do usuario 'mecanica' (o normal): o sistema entra com ele.
        # Banco antigo de outro dono: continua entrando como 'postgres'.
        $dono = Psql-Postgres "select pg_get_userbyid(datdba) from pg_database where datname = '$Banco'"
        if ($dono -eq $UsuarioBanco) {
            $usuarioApp = $UsuarioBanco; $senhaDb = $senhaBanco
        } else {
            $usuarioApp = 'postgres'; $senhaDb = $senhaPg
            Write-Host "Aviso: o banco ja existia e pertence a '$dono'; o sistema vai entrar como 'postgres'." -ForegroundColor Yellow
        }
        $config = [pscustomobject]@{
            APP_PASSWORD = $senhaApp; CPF_CHAVE = $chave; DB_USERNAME = $usuarioApp; DB_PASSWORD = $senhaDb
        }
        Gravar-Config $arquivoConfig $config
        Write-Host 'Senhas guardadas (protegidas pelo Windows, so abrem neste usuario).' -ForegroundColor Green
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
