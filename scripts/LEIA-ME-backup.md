# Backup do banco da Mecânica DBO

O banco guarda todo o histórico da oficina: clientes, carros, ordens de serviço e valores. Se o computador quebrar e não houver backup, **tudo se perde**. Estes scripts fazem uma cópia do banco todo dia, automaticamente.

| Arquivo | Para que serve |
|---|---|
| `backup-banco.ps1` | Gera um arquivo `.backup` com o banco inteiro e apaga os que têm mais de 30 dias |
| `restaurar-banco.ps1` | Abre um `.backup` num banco **separado**, para conferir ou recuperar dados |
| `configurar-senha-backup.ps1` | Guarda a senha dos backups da nuvem (uma vez só) |
| `backup-nuvem.ps1` | Tranca o backup mais recente com a senha e copia para o Google Drive. Roda sozinho no fim do `backup-banco.ps1` |

Os backups ficam em `C:\MecanicaDBO\backups`. O resultado de cada execução (deu certo ou deu erro) fica anotado em `C:\MecanicaDBO\backups\backup.log`.

---

## 1. Configurar a senha (uma vez só)

Os scripts **não guardam senha**. Ela fica num arquivo do próprio PostgreSQL:

1. Abra a pasta `%APPDATA%\postgresql` (cole isso na barra de endereço do Explorador de Arquivos). Se a pasta não existir, crie.
2. Crie dentro dela o arquivo `pgpass.conf` com uma linha:

   ```
   localhost:5432:*:postgres:SENHA_DO_POSTGRES
   ```

   O formato é `servidor:porta:banco:usuário:senha`. O `*` vale para qualquer banco.

Esse arquivo pertence ao usuário do Windows que o criou. Por isso, a tarefa agendada (passo 3) precisa rodar **com esse mesmo usuário**.

## 2. Testar o backup na mão

Abra o PowerShell na pasta do projeto e rode:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\backup-banco.ps1
```

Deve aparecer `OK: backup concluido`, e um arquivo `mecanica_dbo_AAAA-MM-DD_HHmm.backup` deve surgir em `C:\MecanicaDBO\backups`.

O `-ExecutionPolicy Bypass` é necessário porque o Windows, por padrão, não deixa rodar scripts `.ps1`.

## 3. Agendar o backup automático (12h e 18h)

1. Aperte a tecla Windows, digite **Agendador de Tarefas** e abra.
2. No painel da direita, clique em **Criar Tarefa...** (não use "Criar Tarefa Básica").
3. Aba **Geral**:
   - Nome: `Backup Mecanica DBO`
   - Marque **Executar estando o usuário conectado ou não**. O Windows vai pedir a senha do seu usuário ao salvar.
   - Confira que o usuário mostrado é o mesmo que tem o `pgpass.conf`.
4. Aba **Disparadores** → **Novo...**:
   - **Diariamente**, às **12:00** → OK.
   - **Novo...** de novo: **Diariamente**, às **18:00** → OK.
5. Aba **Ações** → **Novo...**:
   - Programa/script: `powershell.exe`
   - Adicione argumentos:
     ```
     -NoProfile -ExecutionPolicy Bypass -File "C:\CAMINHO\DO\PROJETO\scripts\backup-banco.ps1"
     ```
     Troque `C:\CAMINHO\DO\PROJETO` pela pasta real do projeto no computador.
6. Aba **Condições**: desmarque **Iniciar a tarefa somente se o computador estiver ligado na energia CA**. Isso só importa em notebook.
7. Aba **Configurações**: marque **Executar a tarefa assim que possível após uma inicialização agendada ter sido perdida**. Se o PC estiver desligado ao meio-dia, o backup roda quando ele for ligado.
8. Clique em OK.

**Para testar:** clique com o botão direito na tarefa → **Executar**. Depois, abra o `backup.log` e veja se a última linha diz `OK`.

## 4. Cópia no Google Drive (no computador da oficina)

O backup no mesmo computador protege contra erro de digitação ou dado apagado, mas **não** contra HD queimado, roubo ou vírus. Por isso, cada backup também vai para o Google Drive, **trancado com senha**.

Faça uma vez só, no computador da oficina, com o **mesmo usuário do Windows** da tarefa agendada:

1. **Instale o Google Drive e o 7-Zip** (PowerShell):

   ```powershell
   winget install Google.GoogleDrive
   winget install 7zip.7zip
   ```

2. **Abra o Google Drive para computador** (menu Iniciar → "Google Drive") e entre com a conta Google da oficina. Ele cria uma unidade, normalmente `G:`, com a pasta `Meu Drive`.
3. **Guarde a senha dos backups:**

   ```powershell
   powershell -ExecutionPolicy Bypass -File scripts\configurar-senha-backup.ps1
   ```

   **Anote a senha no papel**, junto com a `CPF_CHAVE`. Sem ela, os backups da nuvem não abrem.
4. **Teste:** rode o backup (passo 2). Além do `OK: backup concluido`, deve aparecer `NUVEM: OK: ... copiado para G:\Meu Drive\Backups Mecanica DBO`. Confira no site drive.google.com se a pasta **Backups Mecanica DBO** apareceu.

Daí em diante, o backup das 12h e das 18h também sobe para o Drive sozinho. Ficam os 60 mais recentes (30 dias). Um computador sem a senha configurada (por exemplo, o de desenvolvimento) faz só o backup local.

## 5. Restaurar um backup

> **Os CPFs dos clientes estão criptografados no banco.** Um backup restaurado só mostra os CPFs se o sistema rodar com a **mesma `CPF_CHAVE`** de quando ele foi feito. Guarde essa chave junto com as outras senhas, fora do PC. (Backups anteriores a 16/11/2026 ainda têm o CPF legível.)

### Backup que veio do Google Drive (`.7z`)

1. Baixe o `.7z` do drive.google.com (pasta **Backups Mecanica DBO**).
2. Clique com o botão direito → **7-Zip → Extrair aqui** e digite a **senha dos backups**. Sai o arquivo `.backup`.
3. Siga abaixo, como um backup local.

### Para conferir ou consultar dados antigos (seguro)

```powershell
powershell -ExecutionPolicy Bypass -File scripts\restaurar-banco.ps1 `
    -Arquivo C:\MecanicaDBO\backups\mecanica_dbo_2026-10-29_1815.backup `
    -BancoDestino mecanica_dbo_copia
```

O script cria um banco **separado** com os dados do backup. Abra-o no DataGrip para consultar. O banco real não é tocado.

- O script **recusa** o nome `mecanica_dbo`, de propósito.
- Se o banco destino já existir, acrescente `-Substituir` para apagá-lo e recriá-lo.

### Para recuperar o banco real (só em emergência)

Faça com calma e sem pressa. Nada aqui apaga o banco atual; ele só muda de nome.

1. Restaure o backup num banco separado, por exemplo `mecanica_dbo_restaurado`, usando o comando acima.
2. Abra o `mecanica_dbo_restaurado` no DataGrip e confira se os dados estão lá.
3. **Pare o sistema** (o backend Spring).
4. No DataGrip, conectado ao banco `postgres` (não ao `mecanica_dbo`), rode:

   ```sql
   ALTER DATABASE mecanica_dbo RENAME TO mecanica_dbo_antigo;
   ALTER DATABASE mecanica_dbo_restaurado RENAME TO mecanica_dbo;
   ```

5. Suba o sistema de novo.

O banco anterior fica guardado como `mecanica_dbo_antigo`. Só apague quando tiver certeza de que está tudo certo.

## Se o backup der erro

Abra o `backup.log`. Os erros mais comuns são:

| Mensagem | O que fazer |
|---|---|
| `password authentication failed` ou `no password supplied` | O `pgpass.conf` está faltando, com senha errada, ou a tarefa está rodando com outro usuário do Windows |
| `pg_dump.exe nao encontrado` | O PostgreSQL foi instalado em outra pasta. Rode com `-PgBin "C:\caminho\para\PostgreSQL\18\bin"` |
| `NUVEM: ERRO: Pasta "Meu Drive" nao encontrada` | O Google Drive para computador está fechado ou desconectado. Abra e entre na conta |
| `NUVEM: ERRO: Senha dos backups nao configurada` ou `Nao consegui abrir ...senha-backup.dat` | Rode o `configurar-senha-backup.ps1` com o mesmo usuário do Windows da tarefa agendada |
| `NUVEM: ERRO: 7-Zip nao encontrado` | `winget install 7zip.7zip` |
| `could not connect to server` | O serviço do PostgreSQL está parado. Abra **Serviços** do Windows e inicie `postgresql-x64-18` |
