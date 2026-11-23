# ============================================================
#  Mecanica DBO - funcoes usadas pelos scripts de instalacao.
#  Nao rode este arquivo sozinho: os outros carregam ele.
# ============================================================

$PortaSistema = 8080
$EnderecoSistema = "http://127.0.0.1:$PortaSistema"

function Sistema-NoAr {
    $cliente = New-Object Net.Sockets.TcpClient
    try {
        $tentativa = $cliente.BeginConnect('127.0.0.1', $PortaSistema, $null, $null)
        if (-not $tentativa.AsyncWaitHandle.WaitOne(500)) { return $false }
        $cliente.EndConnect($tentativa)
        return $true
    } catch {
        return $false
    } finally {
        $cliente.Close()
    }
}

function Achar-Java {
    # 'java -version' escreve no stderr; no PowerShell 5.1, com 'Stop', isso viraria erro.
    $ErrorActionPreference = 'Continue'
    $candidatos = @()
    $comando = Get-Command java -ErrorAction SilentlyContinue
    if ($comando) { $candidatos += $comando.Source }
    $candidatos += (Get-ChildItem "$env:ProgramFiles\Eclipse Adoptium\*\bin\java.exe" -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending | ForEach-Object { $_.FullName })
    foreach ($java in $candidatos) {
        $versao = (& $java -version 2>&1 | Out-String)
        if ($versao -match 'version "(\d+)') {
            if ([int]$Matches[1] -ge 21) { return $java }
        }
    }
    throw 'Java 21 nao encontrado. Instale com: winget install EclipseAdoptium.Temurin.21.JRE (e abra o PowerShell de novo).'
}

function Achar-PgBin {
    $achado = Get-ChildItem 'C:\Program Files\PostgreSQL\*\bin\psql.exe' -ErrorAction SilentlyContinue |
        Sort-Object { [int]($_.Directory.Parent.Name -replace '\D', '') } -Descending |
        Select-Object -First 1
    if (-not $achado) { throw 'PostgreSQL nao encontrado em C:\Program Files\PostgreSQL. Instale o PostgreSQL 18 primeiro.' }
    return $achado.Directory.FullName
}

function Texto-De([Security.SecureString]$seguro) {
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($seguro)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
}

# Segredos do sistema (senha do login, CPF_CHAVE, senha do PostgreSQL),
# guardados com a protecao do Windows (DPAPI): so abrem neste PC, com este usuario.
function Ler-Config([string]$arquivo) {
    if (-not (Test-Path $arquivo)) { return $null }
    $seguro = (Get-Content $arquivo -Raw).Trim() | ConvertTo-SecureString
    return (Texto-De $seguro) | ConvertFrom-Json
}

function Gravar-Config([string]$arquivo, $config) {
    New-Item -ItemType Directory -Force -Path (Split-Path $arquivo) | Out-Null
    ($config | ConvertTo-Json -Compress) |
        ConvertTo-SecureString -AsPlainText -Force |
        ConvertFrom-SecureString |
        Set-Content -Path $arquivo -Encoding ASCII
}
