# Mecânica DBO — Backend

API REST do sistema de gestão da **Mecânica DBO**: Spring Boot 3.3, Java 21 e PostgreSQL.

Para subir o sistema inteiro (banco, backend, frontend e OCR), veja o [README da raiz](../README.md).

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
| `APP_USERNAME` | não | `admin` | Usuário do login |
| `DB_URL` | não | `jdbc:postgresql://localhost:5432/mecanica_dbo` | Apontar para outro banco (ex.: uma cópia para teste) |
| `DB_USERNAME` / `DB_PASSWORD` | não | `postgres` / `postgres` | Acesso ao PostgreSQL |
| `APP_CORS_ORIGINS` | não | `http://localhost:4200,...` | Endereço do frontend, se não for o padrão |
| `OCR_URL` | não | `http://127.0.0.1:5000` | Endereço do serviço Python de OCR |
| `FLYWAY_ENABLED` | não | `true` | Desligar o Flyway (só em casos especiais) |
| `SPRING_PROFILES_ACTIVE` | não | — | `prod` no PC da oficina: sem Swagger, sem SQL no console, log INFO |

A API sobe em `http://localhost:8080`.

## Banco de dados e Flyway

A estrutura do banco está em `src/main/resources/db/migration`:

| Arquivo | O que faz |
|---|---|
| `V1__schema_inicial.sql` | Tabelas, enums, função e triggers de totais, constraints e índices (gerado do banco real em 29/10/2026) |
| `V2__normalizar_placas.sql` | Placas só com letras e números (`JEH-3030` → `JEH3030`) + `CHECK` que impede hífen daqui em diante |

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
├── exception/    GlobalExceptionHandler, OcrExceptionHandler e exceções de negócio
├── model/        Entidades JPA
├── repository/   Spring Data (consultas com JOIN FETCH)
└── service/      Regras de negócio
    └── pdf/      ReciboMontador (formata a OS para o recibo)
```

Decisões que valem para o projeto inteiro:

- **Entrada sempre por DTO de request, sem `id`.** Com a entidade no corpo, um POST com `id` faria merge (UPDATE de um registro existente).
- **Escrita em `XyzService`, leitura em `XyzConsultaService`.** As consultas montam os DTOs dentro da transação, com `JOIN FETCH`, sem N+1 nem `LazyInitializationException`.
- **OS ENTREGUE é imutável** (`garantirEditavel`): editar a OS ou mexer em itens devolve 409. Para corrigir, volte o status para CONCLUIDA.
- **Cadastro de cliente + veículo + OS numa transação só** (`AtendimentoService`): se uma parte falha, nada é gravado.
- **Placa:** toda normalização e formatação passa por `service/Placas`.

## Endpoints

Todos exigem login, exceto `/api/auth/*` e o Swagger. O Swagger (`/swagger-ui.html`) tem a lista completa com os corpos.

| Recurso | Endpoints principais |
|---|---|
| Login | `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me` |
| Clientes | `GET /api/clientes?q=`, `GET /{id}`, `GET /{id}/veiculos`, `POST`, `PUT /{id}`, `DELETE /{id}` |
| Veículos | `GET /api/veiculos?q=`, `GET /{id}`, `GET /placa/{placa}`, `POST /cliente/{clienteId}`, `PUT /{id}`, `DELETE /{id}` (inativa) |
| Atendimento | `POST /api/atendimentos` — cliente (novo ou existente) + veículo (novo ou existente) + OS (opcional), tudo ou nada |
| Ordens de serviço | `GET /api/ordens?q=`, `GET /abertas`, `GET /periodo?inicio=&fim=`, `GET /{id}`, `GET /veiculo/{veiculoId}`, `POST /veiculo/{veiculoId}`, `PUT /{id}`, `PATCH /{id}/status?status=`, `DELETE /{id}` (só ENTREGUE) |
| Itens da OS | `POST /api/ordens/{id}/pecas`, `DELETE /{osId}/pecas/{itemId}`, `POST /{id}/servicos`, `DELETE /{osId}/servicos/{itemId}` |
| Recibo | `GET /api/ordens/{id}/pdf` |
| OCR | `POST /api/ocr/nota` (multipart, campo `file`), `GET /api/ocr/status` |

### Formato de erro

Todos os erros saem assim — o frontend lê `mensagem` e `campos`:

```json
{ "timestamp": "...", "status": 409, "mensagem": "Placa já cadastrada: JEH-3030", "campos": { } }
```

`RecursoNaoEncontradoException` → 404, `RegraDeNegocioException` → 409, validação → 400 com `campos`. Erros de constraint do banco são traduzidos pelo nome da constraint em `GlobalExceptionHandler.traduzirIntegridade` — constraint nova pede um `if` lá.

## Recibo em PDF

`PdfService` busca a OS, o `ReciboMontador` formata dinheiro, datas, placa e campos vazios, o Thymeleaf aplica em `templates/pdf/recibo.html` e o Flying Saucer gera o PDF.

Para mudar o visual, edite `recibo.html`. Ele precisa ser **XML válido** e usar **CSS 2.1** (sem flexbox, grid, `border-radius` ou `&nbsp;` — use `&#160;`).

Dados da oficina no cabeçalho: `app.oficina.nome`, `app.oficina.endereco`, `app.oficina.telefone` no `application.properties`.

O script Python antigo do recibo está congelado em `legado/python-recibo/`, só para referência.
