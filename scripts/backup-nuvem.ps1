# ============================================================
#  Mecanica DBO - manda o backup mais recente para o Google Drive
#
#  1. Pega o .backup mais novo de C:\MecanicaDBO\backups.
#  2. Tranca com a senha dos backups (7-Zip, AES-256, nomes escondidos).
#  3. Confere se o arquivo trancado abre com a senha.
#  4. Copia para a pasta do "Google Drive para computador", que sobe
#     sozinho para a nuvem.
#  5. Mantem os 60 mais recentes na nuvem (30 dias, 2 por dia).
#
#  Normalmente roda sozinho, no fim do backup-banco.ps1. So funciona
#  depois do configurar-senha-backup.ps1. Veja scripts\LEIA-ME-backup.md.
#
#  Uso (na mao):
#    powershell -ExecutionPolicy Bypass -File scripts\backup-nuvem.ps1
#    powershell -ExecutionPolicy Bypass -File scripts\backup-nuvem.ps1 -PastaNuvem "G:\Meu Drive\Backups Mecanica DBO"
#
#  (Texto sem acentos de proposito, como no backup-banco.ps1.)
# ============================================================

param(
    [string]$PastaBackups = 'C:\MecanicaDBO\backups',
    [string]$PastaNuvem   = '',
    [string]$ArquivoSenha = 'C:\MecanicaDBO\senha-backup.dat',
    [string]$Banco        = 'mecanica_dbo',
    [int]   $Manter       = 60
)

$ErrorActionPreference = 'Stop'
$NomeSubpasta = 'Backups Mecanica DBO'

function Escrever-Log([string]$mensagem) {
    $linha = '{0:yyyy-MM-dd HH:mm:ss}  NUVEM: {1}' -f (Get-Date), $mensagem
    Write-Host $linha
    Add-Content -Path (Join-Path $PastaBackups 'backup.log') -Value $linha -Encoding UTF8
}

function Achar-7Zip {
    foreach ($exe in @("$env:ProgramFiles\7-Zip\7z.exe", "${env:ProgramFiles(x86)}\7-Zip\7z.exe")) {
        if ($exe -and (Test-Path $exe)) { return $exe }
    }
    throw '7-Zip nao encontrado. Instale com: winget install 7zip.7zip'
}

function Achar-PastaNuvem {
    if ($PastaNuvem) {
        $raiz = Split-Path $PastaNuvem
        if ($raiz -and -not (Test-Path $raiz)) {
            throw "A pasta $raiz nao existe. O Google Drive para computador esta aberto e conectado?"
        }
        return $PastaNuvem
    }
    # O Google Drive para computador cria uma unidade (normalmente G:)
    # com a pasta "Meu Drive" (ou "My Drive", se o Windows estiver em ingles).
    foreach ($unidade in (Get-PSDrive -PSProvider FileSystem)) {
        foreach ($nome in @('Meu Drive', 'My Drive')) {
            $meuDrive = Join-Path $unidade.Root $nome
            if (Test-Path $meuDrive) { return (Join-Path $meuDrive $NomeSubpasta) }
        }
    }
    throw 'Pasta "Meu Drive" nao encontrada. Abra o Google Drive para computador e confira se esta conectado (ou informe -PastaNuvem).'
}

function Ler-Senha {
    if (-not (Test-Path $ArquivoSenha)) {
        throw "Senha dos backups nao configurada. Rode scripts\configurar-senha-backup.ps1 (com este usuario do Windows)."
    }
    try {
        $seguro = Get-Content $ArquivoSenha -Raw | ForEach-Object { $_.Trim() } | ConvertTo-SecureString
    } catch {
        throw "Nao consegui abrir $ArquivoSenha. Ele foi criado por outro usuario do Windows? Rode o configurar-senha-backup.ps1 de novo com o usuario da tarefa agendada."
    }
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($seguro)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
}

function Rodar-7Zip([string]$exe, [string[]]$argumentos, [string]$etapa) {
    $ErrorActionPreference = 'Continue'
    $saida = & $exe @argumentos 2>&1 | Out-String
    $codigo = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    if ($codigo -ne 0) { throw "7-Zip falhou ao $etapa (codigo $codigo): $($saida.Trim())" }
}

$temporario = $null
try {
    $original = Get-ChildItem -Path $PastaBackups -Filter "$($Banco)_*.backup" |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $original) { throw "Nenhum $($Banco)_*.backup em $PastaBackups. Rode o backup-banco.ps1 primeiro." }

    $sete    = Achar-7Zip
    $destino = Achar-PastaNuvem
    $senha   = Ler-Senha

    $nome       = [IO.Path]::ChangeExtension($original.Name, '.7z')
    $temporario = Join-Path $env:TEMP $nome
    $final      = Join-Path $destino $nome

    if (Test-Path $final) {
        Escrever-Log "$nome ja esta na nuvem. Nada a enviar."
        exit 0
    }

    # Tranca numa pasta temporaria primeiro: o Drive so ve o arquivo pronto.
    if (Test-Path $temporario) { Remove-Item $temporario -Force }
    Rodar-7Zip $sete @('a', '-t7z', '-mhe=on', "-p$senha", '-y', $temporario, $original.FullName) 'trancar o backup'
    Rodar-7Zip $sete @('t', "-p$senha", $temporario) 'conferir o arquivo trancado'
    $senha = $null

    New-Item -ItemType Directory -Force -Path $destino | Out-Null
    Move-Item -Path $temporario -Destination $final -Force
    $temporario = $null

    $tamanhoKb = [math]::Round((Get-Item $final).Length / 1KB, 1)
    Escrever-Log "OK: $nome ($tamanhoKb KB) copiado para $destino"

    # Limpeza: so os arquivos deste script, e so depois de um envio bem-sucedido.
    Get-ChildItem -Path $destino -Filter "$($Banco)_*.7z" |
        Sort-Object Name -Descending |
        Select-Object -Skip $Manter |
        ForEach-Object {
            Remove-Item $_.FullName -Force
            Escrever-Log "Removido da nuvem: $($_.Name)"
        }

    exit 0
}
catch {
    Escrever-Log "ERRO: $($_.Exception.Message)"
    exit 1
}
finally {
    if ($temporario -and (Test-Path $temporario)) { Remove-Item $temporario -Force -ErrorAction SilentlyContinue }
}
