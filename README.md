# Mecânica DBO

Sistema de gestão de ordens de serviço da **Mecânica DBO** (Recanto das Emas, Brasília/DF). Substitui as planilhas de controle da oficina: clientes, veículos, ordens de serviço, peças, mão de obra e recibo em PDF.

## Partes do sistema

| Pasta | O que é | Tecnologia |
|---|---|---|
| [`mecanica-dbo-v2/`](mecanica-dbo-v2/README.md) | Backend (API REST) | Spring Boot 3.3, Java 21, PostgreSQL, Flyway |
| [`mecanica-dbo-front/`](mecanica-dbo-front/) | Frontend (telas) | Angular 22 |
| [`mecanica-dbo-ocr/`](mecanica-dbo-ocr/) | Leitura de nota fiscal por foto (opcional) | Python, Flask, Tesseract |
| [`scripts/`](scripts/LEIA-ME-backup.md) | Backup e restauração do banco | PowerShell |
| `docs/rascunhos/` | Rascunhos e protótipos de design antigos, guardados só como referência | — |

## Como subir tudo no computador

### 1. Banco de dados

Instale o PostgreSQL (o desenvolvimento usa a versão 18) e crie o banco vazio:

```sql
CREATE DATABASE mecanica_dbo;
```

Não precisa rodar script de tabelas: o **Flyway** cria toda a estrutura na primeira vez que o backend sobe.

### 2. Backend

No IntelliJ, abra `mecanica-dbo-v2` e rode `MecanicaDboApplication` com a variável de ambiente **`APP_PASSWORD`** (a senha do login — sem ela o sistema não sobe). As outras variáveis estão no [README do backend](mecanica-dbo-v2/README.md#como-rodar).

A API fica em `http://localhost:8080` e a documentação em `http://localhost:8080/swagger-ui.html`.

### 3. Frontend

```bash
cd mecanica-dbo-front
npm install        # só na primeira vez
npm start
```

Abra `http://localhost:4200` e entre com o usuário `admin` (ou o definido em `APP_USERNAME`) e a senha de `APP_PASSWORD`.

### 4. OCR de notas fiscais (opcional)

Sem ele, o resto do sistema funciona normalmente; o botão de importar nota só avisa que o leitor está desligado.

```bash
cd mecanica-dbo-ocr
pip install -r requirements.txt
python ocr_service.py
```

Precisa do [Tesseract](https://github.com/UB-Mannheim/tesseract/wiki) instalado (com o idioma português). Se ele não estiver em `C:\Program Files\Tesseract-OCR`, defina `TESSERACT_CMD` com o caminho do `tesseract.exe`.

## Backup

O banco tem os dados reais da oficina. O script `scripts\backup-banco.ps1` gera uma cópia diária, e o [LEIA-ME de backup](scripts/LEIA-ME-backup.md) explica como agendar, restaurar e guardar uma cópia fora do computador.

**Backups nunca vão para o GitHub** (`*.backup` está no `.gitignore`): têm nomes, CPFs e telefones de clientes.

## Testes

```bash
cd mecanica-dbo-front
npx ng test --watch=false
```

## Regras que não podem quebrar

- **Totais da OS são calculados pelo banco** (trigger), nunca no Java nem no Angular.
- **Peça pode ter valor negativo** (linha de crédito); serviço não.
- **OS entregue não pode ser alterada** — para corrigir, volte o status para Concluída.
- **Estrutura do banco só muda por migração nova** (`V3__...sql`), nunca editando uma já aplicada.
- **O formato de erro da API** (`timestamp`, `status`, `mensagem`, `campos`) é lido pelo frontend: não mude.
