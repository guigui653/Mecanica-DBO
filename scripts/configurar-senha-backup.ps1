# ============================================================
#  Mecanica DBO - guarda a senha dos backups da nuvem (uma vez so)
#
#  Pergunta a senha duas vezes e grava em C:\MecanicaDBO\senha-backup.dat,
#  protegida pelo proprio Windows (DPAPI): o arquivo so abre neste
#  computador, com este usuario do Windows. Copiar o arquivo para outro
#  PC nao adianta.
#
#  O backup-nuvem.ps1 usa essa senha para trancar cada backup antes de
#  mandar para o Google Drive. SEM ESSA SENHA OS BACKUPS DA NUVEM NAO ABREM:
#  anote no papel, junto com a CPF_CHAVE.
#
#  Rode com o MESMO usuario do Windows que roda a tarefa agendada.
#
#  Uso:
#    powershell -ExecutionPolicy Bypass -File scripts\configurar-senha-backup.ps1
# ============================================================

param(
    [string]$ArquivoSenha = 'C:\MecanicaDBO\senha-backup.dat',
    [int]   $TamanhoMinimo = 12
)

$ErrorActionPreference = 'Stop'

function Para-Texto([Security.SecureString]$seguro) {
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($seguro)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
}

if (Test-Path $ArquivoSenha) {
    Write-Host ''
    Write-Host "Ja existe uma senha guardada em $ArquivoSenha." -ForegroundColor Yellow
    Write-Host 'Os backups ja enviados continuam abrindo SO com a senha antiga.'
    $resposta = Read-Host 'Trocar mesmo assim? Digite SIM para trocar'
    if ($resposta -ne 'SIM') { Write-Host 'Nada foi alterado.'; exit 0 }
}

Write-Host ''
Write-Host "Escolha a senha dos backups (minimo $TamanhoMinimo caracteres)."
Write-Host 'Dica: tres ou quatro palavras com numeros, ex.: Pneu-Azul-Oficina-2026'
Write-Host ''

while ($true) {
    $senha1 = Para-Texto (Read-Host 'Senha' -AsSecureString)
    $senha2 = Para-Texto (Read-Host 'Repita a senha' -AsSecureString)

    if ($senha1 -ne $senha2) {
        Write-Host 'As duas nao sao iguais. Tente de novo.' -ForegroundColor Red
        continue
    }
    if ($senha1.Length -lt $TamanhoMinimo) {
        Write-Host "Muito curta: tem $($senha1.Length) caracteres, precisa de pelo menos $TamanhoMinimo." -ForegroundColor Red
        continue
    }
    if ($senha1 -match '"') {
        Write-Host 'Nao use aspas (") na senha. Tente de novo.' -ForegroundColor Red
        continue
    }
    break
}

New-Item -ItemType Directory -Force -Path (Split-Path $ArquivoSenha) | Out-Null
ConvertTo-SecureString $senha1 -AsPlainText -Force |
    ConvertFrom-SecureString |
    Set-Content -Path $ArquivoSenha -Encoding ASCII

$senha1 = $null; $senha2 = $null

Write-Host ''
Write-Host "OK: senha guardada em $ArquivoSenha" -ForegroundColor Green
Write-Host ''
Write-Host 'AGORA ANOTE A SENHA NO PAPEL, junto com a CPF_CHAVE.' -ForegroundColor Yellow
Write-Host 'Sem ela, os backups da nuvem nao abrem.' -ForegroundColor Yellow
