# ============================================================
#  Mecanica DBO - troca a senha do login do sistema (usuario admin)
#
#  Rode no computador da oficina, com o usuario do Windows que usa o sistema:
#    powershell -ExecutionPolicy Bypass -File C:\MecanicaDBO\trocar-senha-login.ps1
#
#  Pede a senha nova duas vezes, guarda (protegida pelo Windows) e religa
#  o sistema. As outras senhas, a CPF_CHAVE e os dados nao mudam.
# ============================================================

$ErrorActionPreference = 'Stop'
$Pasta = $PSScriptRoot
. (Join-Path $Pasta 'comum.ps1')
$arquivoConfig = Join-Path $Pasta 'config\sistema.dat'

try {
    $config = Ler-Config $arquivoConfig
    if (-not $config) { throw "Configuracao nao encontrada em $arquivoConfig. O sistema foi instalado com este usuario do Windows?" }

    Passo 'Senha nova do login (usuario: admin)'
    Write-Host 'Dica: tres palavras com numero, ex.: Oleo-Freio-Pneu-77. Anote no papel.'
    Write-Host 'Nada aparece enquanto voce digita; e normal.'
    $config.APP_PASSWORD = Pedir-Senha-Nova 'Senha nova (minimo 10 caracteres)' 10
    Gravar-Config $arquivoConfig $config
    Write-Host 'Senha guardada.' -ForegroundColor Green

    Passo 'Religando o sistema com a senha nova'
    Parar-Sistema
    & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $Pasta 'abrir-sistema.ps1')
    if ($LASTEXITCODE -ne 0) { throw "O sistema nao ligou. Veja $Pasta\logs\sistema.log" }

    Passo 'Pronto'
    Write-Host 'Entre no sistema com o usuario admin e a senha nova. Anote no papel e risque a antiga.' -ForegroundColor Green
    exit 0
}
catch {
    Write-Host ''
    Write-Host "ERRO: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
