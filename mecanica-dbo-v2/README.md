# Mecânica DBO — Backend

API REST do sistema de gestão da **Mecânica DBO**: Spring Boot 3.3, Java 21 e PostgreSQL.

Para subir o sistema inteiro (banco, backend e frontend), veja o [README da raiz](../README.md).

---

## Stack

| Tecnologia | Uso |
|---|---|
| Java 21 + Spring Boot 3.3.3 | API REST |
| Spring Data JPA / Hibernate 6.5 | Acesso ao banco (`ddl-auto=validate`: o Hibernate só confere, não cria tabelas) |
| PostgreSQL | Banco `mecanica_dbo`, com enums nativos, triggers e coluna gerada |
| Flyway 10 | Estrutura do banco versionada em `src/main/resources/db/migration` |
| Spring Security | Login por sessão (cookie) + CSRF por cookie |
| Thymeleaf + Flying Saucer | Recibo da OS em PDF, só com Java |
| Springdoc | Swagger em `/swagger-ui.html` (desligado no perfil `prod`) |
| Lombok | Getters/setters/construtores |

## Como rodar

Pré-requisitos: Java 21 e PostgreSQL com o banco `mecanica_dbo` criado (vazio ou já existente — o Flyway cuida da estrutura).

No IntelliJ, rode `MecanicaDboApplication` com estas **variáveis de ambiente** (Run → Edit Configurations → Modify options → Environment variables):

| Variável | Obrigatória | Padrão | Para quê |
|---|---|---|---|
| `APP_PASSWORD` | **sim** | — | Senha do login. Sem ela o sistema não sobe |
| `CPF_CHAVE` | **sim** | — | Chave da criptografia do CPF (32 bytes em Base64; como gerar, abaixo). Sem ela o sistema não sobe |
| `CPF_CHAVE_ANTIGA` | não | — | Só na **troca de chave** (abaixo). Apague depois |
| `APP_USERNAME` | não | `admin` | Usuário do login |
| `DB_URL` | não | `jdbc:postgresql://localhost:5432/mecanica_dbo` | Apontar para outro banco (ex.: uma cópia para teste) |
| `DB_USERNAME` / `DB_PASSWORD` | não | `postgres` / `postgres` | Acesso ao PostgreSQL |
| `APP_CORS_ORIGINS` | não | `http://localhost:4200,...` | Endereço do frontend, se não for o padrão |
| `FLYWAY_ENABLED` | não | `true` | Desligar o Flyway (só em casos especiais) |
| `SPRING_PROFILES_ACTIVE` | não | — | `prod` no PC da oficina: sem Swagger, sem SQL no console, log INFO |

A API sobe em `http://localhost:8080`.

### A chave do CPF (`CPF_CHAVE`)

O CPF fica **criptografado no banco** (AES-256-GCM). Para gerar a chave, uma vez, no PowerShell:

```powershell
$b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

- **Anote a chave em papel e guarde fora do PC**, junto com a senha dos backups. **Perder a chave = perder todos os CPFs**: nem restaurando um backup dá para lê-los.
- **A chave acompanha o banco.** Ao levar o banco (ou restaurar um backup) para outro PC, o backend de lá precisa da **mesma** `CPF_CHAVE`.
- A tela e o recibo mostram o CPF mascarado (`***.456.789-**`). A busca de clientes acha o CPF só pelo número completo.
- O sistema **recusa chave de exemplo** (tipo `xxxx...=` ou `AAAA...=`): a chave tem que ser gerada pelo comando acima.
- Se o terminal já é PowerShell, rode o comando **direto** (sem `powershell -Command "..."` em volta). Para a chave ir direto para a área de transferência, sem aparecer na tela, termine com `| Set-Clipboard`.

### Trocar a chave do CPF

Quando a chave vazou ou foi usada uma chave fraca:

1. Gere a chave nova (comando acima) e **anote no papel**.
2. Nas variáveis de ambiente, renomeie a `CPF_CHAVE` atual para `CPF_CHAVE_ANTIGA` e crie a `CPF_CHAVE` com a nova:
   `APP_PASSWORD=...;CPF_CHAVE_ANTIGA=<chave antiga>;CPF_CHAVE=<chave nova>`
3. Suba o backend. Logo depois do Flyway, o log mostra `Troca da chave do CPF concluída: N CPF(s) trancado(s) com a chave nova`. É tudo numa transação: se algum CPF não abrir com nenhuma das duas chaves, nada muda e o sistema não sobe.
4. **Apague a `CPF_CHAVE_ANTIGA`** e faça um backup novo.

Os backups feitos **antes** da troca continuam trancados com a chave antiga.

## Banco de dados e Flyway

A estrutura do banco está em `src/main/resources/db/migration`:

| Arquivo | O que faz |
|---|---|
| `V1__schema_inicial.sql` | Tabelas, enums, função e triggers de totais, constraints e índices (gerado do banco real em 29/10/2026) |
| `V2__normalizar_placas.sql` | Placas só com letras e números (`JEH-3030` → `JEH3030`) + `CHECK` que impede hífen daqui em diante |
| `V3__criptografar_cpf` (Java, em `com.mecanicadbo.migracao`) | CPF criptografado: cria `cpf_cifrado` e `cpf_hash`, converte os CPFs existentes e apaga a coluna `cpf` em claro. Para sem mudar nada se dois clientes tiverem o mesmo CPF. É em Java porque precisa da `CPF_CHAVE` |

- **Banco vazio:** o Flyway roda todas as versões e cria tudo.
- **Banco que já tinha tabelas antes do Flyway:** faz *baseline* na versão 1 e roda só as seguintes.
- **Mudança de estrutura nova:** crie `V3__descricao.sql`. **Nunca edite uma migração já aplicada** — o Flyway recusa subir se o arquivo mudar.

### Regras que vivem no banco

- **Totais da OS** (`total_pecas`, `total_servicos`, `total_geral`) são calculados pela trigger `recalcular_totais_os`. O Java nunca soma. Peças com `pago_pelo_cliente = true` ficam fora do total.
- **`item_peca.valor_total`** é coluna gerada (`quantidade × valor_unitario`).
- **Peça pode ter valor negativo** (linha de crédito). Serviço não (`CHECK valor >= 0`).
- **Placa** só com letras e números, maiúsculas. O hífen do padrão antigo é só exibição.
- **Status da OS** é o enum nativo `status_os` (mapeado com `@JdbcTypeCode(SqlTypes.NAMED_ENUM)`).

## Arquitetura

```
com.mecanicadbo
├── config/       SecurityConfig (login, CSRF, CORS), OpenApiConfig
├── controller/   Endpoints REST
├── dto/          *Request (entrada, sem id) e *DTO (saída)
├── exception/    GlobalExceptionHandler e exceções de negócio
├── model/        Entidades JPA
├── repository/   Spring Data (consultas com JOIN FETCH)
└── service/      Regras de negócio
    └── pdf/      ReciboMontador (formata a OS para o recibo)
```

Decisões que valem para o projeto inteiro:

- **Entrada sempre por DTO de request, sem `id`.** Com a entidade no corpo, um POST com `id` faria merge (UPDATE de um registro existente).
- **Escrita em `XyzService`, leitura em `XyzConsultaService`.** As consultas montam os DTOs dentro da transação, com `JOIN FETCH`, sem N+1 nem `LazyInitializationException`.
- **OS ENTREGUE é imutável** (`garantirEditavel`): editar a OS ou mexer em itens devolve 409. Para corrigir, volte o status para CONCLUIDA.
- **Histórico nunca é apagado junto:** cliente com OS não pode ser apagado (só inativado), e apagar veículo não leva as OS (sem cascade).
- **Cadastro de cliente + veículo + OS numa transação só** (`AtendimentoService`): se uma parte falha, nada é gravado.
- **Placa:** toda normalização e formatação passa por `service/Placas`.

## Endpoints

Todos exigem login, exceto `/api/auth/*` e o Swagger. O Swagger (`/swagger-ui.html`) tem a lista completa com os corpos.

| Recurso | Endpoints principais |
|---|---|
| Login | `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me` |
| Clientes | `GET /api/clientes?q=`, `GET /{id}`, `GET /{id}/veiculos`, `POST`, `PUT /{id}`, `PATCH /{id}/inativar`, `DELETE /{id}` (só cliente **sem nenhuma OS** — cadastro duplicado ou de teste; com histórico, 409 sugerindo inativar) |
| Veículos | `GET /api/veiculos?q=`, `GET /{id}`, `GET /placa/{placa}`, `POST /cliente/{clienteId}`, `PUT /{id}`, `DELETE /{id}` (inativa) |
| Atendimento | `POST /api/atendimentos` — cliente (novo ou existente) + veículo (novo ou existente) + OS (opcional), tudo ou nada |
| Ordens de serviço | `GET /api/ordens?q=`, `GET /abertas`, `GET /periodo?inicio=&fim=`, `GET /{id}`, `GET /veiculo/{veiculoId}`, `POST /veiculo/{veiculoId}`, `PUT /{id}`, `PATCH /{id}/status?status=`, `DELETE /{id}` (só ENTREGUE) |
| Itens da OS | `POST /api/ordens/{id}/pecas`, `DELETE /{osId}/pecas/{itemId}`, `POST /{id}/servicos`, `DELETE /{osId}/servicos/{itemId}` |
| Recibo | `GET /api/ordens/{id}/pdf` |
| Painel | `GET /api/dashboard/resumo` — contadores, pátio, prontas, faturamento e clientes novos do mês, já contados pelo banco |

### Formato de erro

Todos os erros saem assim — o frontend lê `mensagem` e `campos`:

```json
{ "timestamp": "...", "status": 409, "mensagem": "Placa já cadastrada: JEH-3030", "campos": { } }
```

`RecursoNaoEncontradoException` → 404, `RegraDeNegocioException` → 409, validação → 400 com `campos`. Erros de constraint do banco são traduzidos pelo nome da constraint em `GlobalExceptionHandler.traduzirIntegridade` — constraint nova pede um `if` lá.

## Testes

```bash
mvn test
```

| Classe | O que garante |
|---|---|
| `PlacasTest` | Normalização e formatação de placa |
| `ReciboMontadorTest` | Dinheiro, datas, crédito negativo, peça paga pelo cliente e campos vazios no recibo |
| `OrdemServicoServiceTest` | Transições de status e a trava de OS entregue |
| `AtendimentoServiceTest` | Combinações aceitas no `/atendimentos` e a ordem de gravação |
| `ClienteServiceTest` | Apagar só cliente sem OS; inativar mantém o cadastro |
| `BancoIntegracaoTest` | Contra um PostgreSQL de verdade: Flyway, trigger dos totais, rollback do atendimento, enum da nota fiscal e painel |

O `BancoIntegracaoTest` só roda quando a variável `TESTE_DB_URL` aponta para um banco **descartável** (ele grava dados). Ele se recusa a rodar contra o `mecanica_dbo`:

```powershell
createdb -U postgres mecanica_dbo_teste
$env:TESTE_DB_URL = "jdbc:postgresql://localhost:5432/mecanica_dbo_teste"
mvn test
```

## Recibo em PDF

`PdfService` busca a OS, o `ReciboMontador` formata dinheiro, datas, placa e campos vazios, o Thymeleaf aplica em `templates/pdf/recibo.html` e o Flying Saucer gera o PDF.

Para mudar o visual, edite `recibo.html`. Ele precisa ser **XML válido** e usar **CSS 2.1** (sem flexbox, grid, `border-radius` ou `&nbsp;` — use `&#160;`).

Dados da oficina no cabeçalho: `app.oficina.nome`, `app.oficina.endereco`, `app.oficina.telefone` no `application.properties`.

O script Python antigo do recibo está congelado em `legado/python-recibo/`, só para referência.
