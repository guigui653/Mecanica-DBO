# Backup do banco da Mecânica DBO

O banco guarda todo o histórico da oficina: clientes, carros, ordens de serviço e valores. Se o computador quebrar e não houver backup, **tudo se perde**. Estes scripts fazem uma cópia do banco todo dia, automaticamente.

| Arquivo | Para que serve |
|---|---|
| `backup-banco.ps1` | Gera um arquivo `.backup` com o banco inteiro e apaga os que têm mais de 30 dias |
| `restaurar-banco.ps1` | Abre um `.backup` num banco **separado**, para conferir ou recuperar dados |

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

## 4. Guardar uma cópia fora do computador (toda semana)

O backup no mesmo computador protege contra erro de digitação ou dado apagado, mas **não** contra HD queimado, roubo ou vírus. Uma vez por semana, copie a pasta `C:\MecanicaDBO\backups` (ou só o arquivo mais recente) para um **pendrive** ou para uma **nuvem** (Google Drive, OneDrive).

## 5. Restaurar um backup

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
| `could not connect to server` | O serviço do PostgreSQL está parado. Abra **Serviços** do Windows e inicie `postgresql-x64-18` |
