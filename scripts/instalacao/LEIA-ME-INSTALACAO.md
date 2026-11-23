# Instalar a Mecânica DBO no computador da oficina

São duas partes: **montar o pacote no seu PC** e **instalar no computador do dono**. Depois de instalado, o sistema liga sozinho quando o Windows entra e faz backup às 12h e às 18h.

## O que ter à mão (no papel)

| O quê | Para quê |
|---|---|
| **`CPF_CHAVE`** (a mesma do seu PC) | O instalador pede. Sem ela, os CPFs não abrem |
| Senha do PostgreSQL do computador do dono | Você escolhe na parte B, passo 2 |
| Senha nova do login do sistema | Você escolhe na parte B, passo 4 |
| Senha dos backups da nuvem | Você escolhe na parte B, passo 6 |
| Chave de recuperação do BitLocker | Aparece na parte B, passo 8 |

**Guarde o papel longe do notebook.** Se levarem os dois juntos, as senhas não protegem nada.

---

## Parte A: no seu PC

1. No terminal do IntelliJ, baixe a versão mais nova:

   ```powershell
   git pull
   ```

2. Gere o pacote. Na primeira vez leva alguns minutos: ele compila o sistema, roda os testes, gera as telas e faz um backup novo do banco.

   ```powershell
   powershell -ExecutionPolicy Bypass -File scripts\gerar-pacote.ps1
   ```

   No fim tem que aparecer `Pronto` e o caminho da pasta `pacote\MecanicaDBO`.

3. Copie a pasta **`pacote\MecanicaDBO` inteira** para o pendrive.

---

## Parte B: no computador do dono

Use o usuário do Windows que o dono usa no dia a dia. Não precisa ser administrador, a não ser para instalar o Java e o PostgreSQL.

### 1. Instalar o Java

Abra o PowerShell (menu Iniciar → digite **PowerShell**) e rode:

```powershell
winget install EclipseAdoptium.Temurin.21.JRE
```

### 2. Instalar o PostgreSQL 18

1. Baixe o instalador em **postgresql.org/download/windows** → "Download the installer" → versão **18**, Windows x86-64.
2. Abra e vá clicando em **Next**. Quando pedir **senha (password)**, crie uma e **anote no papel**. A porta deixe **5432**.
3. No final, **desmarque** "Stack Builder" e clique em **Finish**.

### 3. Copiar o pacote

Copie a pasta **`MecanicaDBO`** do pendrive para a **Área de Trabalho**.

### 4. Rodar o instalador

1. Abra a pasta `MecanicaDBO` (a da Área de Trabalho).
2. Clique com o botão direito num espaço vazio da pasta → **Abrir no Terminal**.
3. Rode:

   ```powershell
   powershell -ExecutionPolicy Bypass -File .\instalar.ps1
   ```

4. Ele vai perguntar, uma de cada vez. As senhas não aparecem enquanto você digita, e isso é normal:
   - **Senha do PostgreSQL:** a do passo 2.
   - **Senha do login:** crie uma nova (mínimo 10 caracteres), repita e **anote no papel**. O usuário é `admin`.
   - **CPF_CHAVE:** a do papel, letra por letra.

5. No fim, o navegador abre sozinho no sistema. Entre com **admin** e a senha nova.

**Confira:** os clientes aparecem, o CPF está borrado e a busca pelo CPF completo encontra o cliente.

### 5. Atalho

Na Área de Trabalho apareceu o atalho **Mecanica DBO**. É por ele que se abre o sistema no dia a dia.

### 6. Backup no Google Drive

1. No PowerShell:

   ```powershell
   winget install Google.GoogleDrive
   ```

   ```powershell
   winget install 7zip.7zip
   ```

2. Abra o **Google Drive** (menu Iniciar) e entre com a **conta Google do dono**.
3. Crie a senha dos backups e **anote no papel**:

   ```powershell
   powershell -ExecutionPolicy Bypass -File C:\MecanicaDBO\scripts\configurar-senha-backup.ps1
   ```

4. Teste:

   ```powershell
   powershell -ExecutionPolicy Bypass -File C:\MecanicaDBO\scripts\backup-banco.ps1
   ```

   Tem que aparecer `OK: backup concluido` e `NUVEM: OK: ...`. Confira em **drive.google.com** se a pasta **Backups Mecanica DBO** apareceu.

### 7. Apagar as cópias

A pasta do pacote tem uma cópia do banco com os dados dos clientes. Depois que tudo funcionar:

- apague a pasta **`MecanicaDBO` da Área de Trabalho** (a de `C:\MecanicaDBO` fica, porque é o sistema);
- apague a pasta do **pendrive**;
- no seu PC, apague a pasta **`pacote`** do projeto.

### 8. Proteger o notebook contra roubo

Sem isso, quem levar o notebook consegue copiar o banco (nomes, telefones, carros, valores). Com isso, o disco fica ilegível sem a senha do Windows.

1. **Senha ou PIN no Windows:** Configurações → Contas → Opções de entrada → **PIN (Windows Hello)**.
2. **Bloqueio automático:** Configurações → Contas → Opções de entrada → "Se você estiver ausente, quando o Windows deverá exigir que você entre novamente?" → **Quando o computador sair do modo de suspensão**. E em Configurações → Sistema → Energia → tela desliga em **5 minutos**.
3. **Criptografia do disco:** Configurações → Privacidade e segurança → **Criptografia do dispositivo** → **Ativar**. (No Windows Pro aparece como **BitLocker**: Painel de Controle → Criptografia de Unidade de Disco BitLocker → Ativar.)
4. **Anote a chave de recuperação:** em Configurações → Privacidade e segurança → Criptografia do dispositivo → **Chaves de recuperação do BitLocker**, ou no BitLocker → **Fazer backup da chave de recuperação** → **Imprimir**. Guarde junto com as outras senhas. Sem ela, se o Windows quebrar, o disco não abre mais.

Se a opção "Criptografia do dispositivo" não aparecer, o notebook não suporta a criptografia grátis do Windows Home. Me avise que vemos outra saída.

---

## Atualizar o sistema depois

1. No seu PC: `git pull` e depois gere o pacote **sem o banco** (os dados do computador do dono não mudam):

   ```powershell
   powershell -ExecutionPolicy Bypass -File scripts\gerar-pacote.ps1 -SemBanco
   ```

2. No computador do dono: copie a pasta nova e rode o `instalar.ps1` de novo (parte B, passo 4). Ele **não pergunta as senhas** e **não mexe no banco**: primeiro **faz um backup** (`C:\MecanicaDBO\backups`, e no Drive), depois troca o sistema e liga de novo.

**Guarde o pacote anterior** (por exemplo, renomeie para `MecanicaDBO-2026-11-17`). Se a versão nova der problema, rode o `instalar.ps1` do pacote anterior para voltar. Se a versão nova tiver mudado o banco, o sistema antigo pode não ligar: aí é preciso restaurar o backup feito antes da atualização (veja `C:\MecanicaDBO\scripts\LEIA-ME-backup.md`). Me chame antes de fazer isso.

## Trocar a senha do login

No computador do dono, no PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File C:\MecanicaDBO\trocar-senha-login.ps1
```

Ele pede a senha nova duas vezes e religa o sistema. A `CPF_CHAVE`, as outras senhas e os dados não mudam.

> Este script chega ao computador do dono **na próxima atualização** (o `instalar.ps1` copia ele para `C:\MecanicaDBO`).

## Se algo der errado

| Problema | O que fazer |
|---|---|
| `Java 21 nao encontrado` | Feche o PowerShell, abra de novo e rode o instalador outra vez. Se continuar, refaça o passo 1 |
| `PostgreSQL nao encontrado` | Refaça o passo 2 |
| `Nao consegui entrar no PostgreSQL com essa senha` | É a senha criada ao instalar o PostgreSQL (passo 2) |
| `Muito curta: minimo 10 caracteres` | A senha do login precisa de pelo menos 10 letras/números |
| O atalho mostra "O sistema nao ligou" | Abra `C:\MecanicaDBO\logs\sistema.log` e veja as últimas linhas |
| `A CPF_CHAVE não abre os CPFs deste banco` (no `sistema.log`) | A chave digitada não é a do papel. Apague a pasta `C:\MecanicaDBO\config` e rode o `instalar.ps1` de novo |
| Backup da nuvem com erro | Veja `C:\MecanicaDBO\scripts\LEIA-ME-backup.md`, parte "Se o backup der erro" |
