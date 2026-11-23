# ============================================================
#  Mecanica DBO - abre o sistema no navegador (atalho da Area de Trabalho)
#
#  Se o sistema estiver desligado, liga e espera ficar pronto.
# ============================================================

$ErrorActionPreference = 'Stop'
$Pasta = $PSScriptRoot
. (Join-Path $Pasta 'comum.ps1')

function Avisar([string]$mensagem) {
    Add-Type -AssemblyName System.Windows.Forms
    [System.Windows.Forms.MessageBox]::Show($mensagem, 'Mecanica DBO') | Out-Null
}

if (-not (Sistema-NoAr)) {
    & powershell -NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File (Join-Path $Pasta 'iniciar-sistema.ps1')
    $limite = (Get-Date).AddSeconds(90)
    while (-not (Sistema-NoAr)) {
        if ((Get-Date) -gt $limite) {
            Avisar ("O sistema nao ligou.`n`nVeja o final do arquivo:`n$Pasta\logs\sistema.log`n" +
                    "e tambem $Pasta\logs\inicializacao.log")
            exit 1
        }
        Start-Sleep -Seconds 2
    }
}

Start-Process $EnderecoSistema
