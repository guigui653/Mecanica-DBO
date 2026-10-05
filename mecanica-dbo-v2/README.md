# Mecânica DBO — Backend

API REST do sistema de gestão da **Mecânica DBO**, construída em **Spring Boot 3.3** com Java 21 e banco de dados PostgreSQL.

---

## Sumário

- [Stack e versões](#stack-e-versões)
- [Como rodar](#como-rodar)
- [Estrutura de pacotes](#estrutura-de-pacotes)
- [Banco de dados](#banco-de-dados)
- [Entidades (Model)](#entidades-model)
- [DTOs](#dtos)
- [Endpoints](#endpoints)
- [Serviços](#serviços)
- [Tratamento de erros](#tratamento-de-erros)
- [CORS](#cors)
- [Swagger](#swagger)
- [PDF e microserviço OCR](#pdf-e-microserviço-ocr)
- [Decisões de arquitetura](#decisões-de-arquitetura)

---

## Stack e versões

| Tecnologia | Versão |
|---|---|
| Java | 21 |
| Spring Boot | 3.3.3 |
| Spring Data JPA | 3.3.x |
| Hibernate | 6.x |
| PostgreSQL Driver | runtime |
| Lombok | latest (excluído do JAR final) |
| Springdoc OpenAPI (Swagger) | 2.5.0 |
| Jackson JSR310 | (datas ISO) |
| Maven | wrapper incluso |

### pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.3</version>
        <relativePath/>
    </parent>

    <groupId>com.mecanicadbo</groupId>
    <artifactId>mecanica_dbo</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>mecanica_dbo</name>
    <description>API de Gestao - Mecanica DBO</description>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>

        <!-- Spring Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Data JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- PostgreSQL Driver -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Swagger UI -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.5.0</version>
        </dependency>

        <!-- DevTools -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>

        <!-- Jackson JSR310 (Datas) -->
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
        </dependency>

        <!-- Testes -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
```

---

## Como rodar

### Pré-requisitos

- **Java 21** instalado e no `PATH`
- **PostgreSQL** rodando em `localhost:5432`
- Banco `mecanica_dbo` criado com o schema (tabelas + triggers)

### Passos

```bash
# 1. Entrar na pasta do backend
cd mecanica-dbo-v2

# 2. Compilar e iniciar
./mvnw spring-boot:run

# Ou, se mvn estiver no PATH:
mvn spring-boot:run
```

A API sobe em `http://localhost:8080`.
Swagger UI em `http://localhost:8080/swagger-ui.html`.

### application.properties (desenvolvimento)

```properties
server.port=8080

# Banco de dados
spring.datasource.url=jdbc:postgresql://localhost:5432/mecanica_dbo
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.default_batch_fetch_size=10

# JSON
spring.jackson.default-property-inclusion=non_null
spring.jackson.serialization.write-dates-as-timestamps=false

# Swagger
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.operationsSorter=method

# Log
logging.level.com.mecanicadbo=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

> Para produção, substitua usuário e senha por variáveis de ambiente.

---

## Estrutura de pacotes

```
com.mecanicadbo/
├── MecanicaDboApplication.java   # Ponto de entrada Spring Boot
│
├── config/
│   ├── OpenApiConfig.java         # Metadados do Swagger (título, versão)
│   └── WebConfig.java             # CORS — libera http://localhost:4200
│
├── controller/
│   ├── ClienteController.java     # /api/clientes
│   ├── VeiculoController.java     # /api/veiculos
│   ├── OrdemServicoController.java # /api/ordens
│   └── PdfController.java         # /api/ordens/{id}/pdf
│
├── dto/
│   ├── AtualizarOsRequest.java    # PUT /ordens/{id} — campos editáveis
│   ├── OrdemServicoDTO.java       # Resposta de OS (completa ou resumida)
│   ├── VeiculoResumoDTO.java      # Veículo + cliente aninhado
│   ├── ClienteResumoDTO.java      # Cliente resumido (dentro do veículo)
│   ├── ItemPecaDTO.java           # Peça com valorTotal
│   └── ItemServicoDTO.java        # Serviço
│
├── exception/
│   ├── RecursoNaoEncontradoException.java  # lança 404
│   ├── RegraDeNegocioException.java        # lança 409
│   └── GlobalExceptionHandler.java         # @RestControllerAdvice
│
├── model/
│   ├── Cliente.java
│   ├── Veiculo.java
│   ├── OrdemServico.java
│   ├── ItemPeca.java
│   ├── ItemServico.java
│   ├── NotaFiscal.java
│   └── StatusOs.java              # enum: ABERTA, EM_ANDAMENTO, CONCLUIDA, ENTREGUE
│
├── repository/
│   ├── ClienteRepository.java
│   ├── VeiculoRepository.java
│   ├── OrdemServicoRepository.java
│   ├── ItemPecaRepository.java
│   └── ItemServicoRepository.java
│
└── service/
    ├── ClienteService.java
    ├── VeiculoService.java
    ├── VeiculoConsultaService.java
    ├── OrdemServicoService.java
    ├── OrdemServicoConsultaService.java
    └── PdfService.java
```

---

## Banco de dados

### Configuração JPA

```properties
spring.jpa.hibernate.ddl-auto=validate
```

O Hibernate está em modo `validate` — ele **não cria nem altera tabelas**. O schema deve ser criado previamente com scripts SQL.

### Tipos enumerados do PostgreSQL

O banco usa tipos `ENUM` nativos (`status_os`, `status_ocr`). Mapeamento com:

```java
@Enumerated(EnumType.STRING)
@JdbcTypeCode(SqlTypes.NAMED_ENUM)
@Column(nullable = false, columnDefinition = "status_os")
private StatusOs status = StatusOs.ABERTA;
```

### Trigger de totais

Os campos `total_pecas`, `total_servicos` e `total_geral` em `ordem_servico` são **calculados por trigger PostgreSQL** após cada INSERT/DELETE em `item_peca` e `item_servico`.

Regra:
- `total_pecas` = soma de `valor_unitario * quantidade` das peças onde `pago_pelo_cliente = false`
- `total_servicos` = soma de `valor` de todos os serviços
- `total_geral` = `total_pecas + total_servicos`

O backend nunca calcula esses valores — apenas lê o que o trigger atualizou.

### Tabelas principais

| Tabela | Descrição |
|---|---|
| `cliente` | Dados do proprietário |
| `veiculo` | Veículo vinculado a um cliente |
| `ordem_servico` | OS com status, datas, km, diagnóstico e totais |
| `item_peca` | Peças de uma OS com flag `pago_pelo_cliente` |
| `item_servico` | Mão de obra de uma OS |
| `nota_fiscal` | NF vinculada a uma OS (feature OCR) |

---

## Entidades (Model)

### `Cliente`

| Campo | Tipo | Restrições |
|---|---|---|
| `id` | `Long` | PK, auto |
| `nome` | `String` | NOT NULL, max 120, @NotBlank |
| `cpf` | `String` | unique, max 14 |
| `telefone1` | `String` | NOT NULL, max 20, @NotBlank |
| `telefone2` | `String` | max 20 |
| `email` | `String` | max 100 |
| `ativo` | `Boolean` | NOT NULL, default true |
| `criadoEm` | `LocalDateTime` | NOT NULL, @PrePersist |

### `Veiculo`

| Campo | Tipo | Restrições |
|---|---|---|
| `id` | `Long` | PK, auto |
| `cliente` | `Cliente` | FK NOT NULL, @JsonIgnore |
| `placa` | `String` | NOT NULL, unique, max 10 |
| `marca` | `String` | NOT NULL, max 60 |
| `modelo` | `String` | NOT NULL, max 80 |
| `chassi` | `String` | max 20 |
| `cor` | `String` | NOT NULL, max 40 |
| `combustivel` | `String` | NOT NULL, max 20 |
| `anoFabricacao` | `Short` | |
| `ativo` | `Boolean` | NOT NULL, default true |
| `criadoEm` | `LocalDateTime` | @PrePersist |

### `OrdemServico`

| Campo | Tipo | Restrições |
|---|---|---|
| `id` | `Long` | PK, auto |
| `veiculo` | `Veiculo` | FK NOT NULL, @JsonIgnore |
| `status` | `StatusOs` | NOT NULL, NAMED_ENUM PostgreSQL |
| `kmEntrada` | `Integer` | |
| `dataEntrada` | `LocalDate` | NOT NULL, @PrePersist |
| `dataSaidaPrevista` | `LocalDate` | |
| `dataSaidaReal` | `LocalDate` | Preenchido ao marcar ENTREGUE |
| `reclamacoes` | `String` | TEXT |
| `diagnostico` | `String` | TEXT |
| `totalPecas` | `BigDecimal` | Calculado por trigger |
| `totalServicos` | `BigDecimal` | Calculado por trigger |
| `totalGeral` | `BigDecimal` | Calculado por trigger |
| `criadoEm` | `LocalDateTime` | @PrePersist |

### `ItemPeca`

| Campo | Tipo | Restrições |
|---|---|---|
| `id` | `Long` | PK, auto |
| `ordemServico` | `OrdemServico` | FK NOT NULL, @JsonIgnore |
| `notaFiscal` | `NotaFiscal` | FK nullable, @JsonIgnore |
| `descricao` | `String` | NOT NULL, max 200 |
| `quantidade` | `BigDecimal` | NOT NULL, default 1 |
| `valorUnitario` | `BigDecimal` | NOT NULL, @PositiveOrZero |
| `valorTotal` | `BigDecimal` | insertable=false, updatable=false (calculado pelo banco) |
| `pagoPeloCliente` | `Boolean` | NOT NULL, default false |
| `criadoEm` | `LocalDateTime` | @PrePersist |

### `ItemServico`

| Campo | Tipo | Restrições |
|---|---|---|
| `id` | `Long` | PK, auto |
| `ordemServico` | `OrdemServico` | FK NOT NULL, @JsonIgnore |
| `descricao` | `String` | NOT NULL, max 200 |
| `valor` | `BigDecimal` | NOT NULL, @PositiveOrZero |
| `criadoEm` | `LocalDateTime` | @PrePersist |

### `NotaFiscal`

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | `Long` | PK, auto |
| `ordemServico` | `OrdemServico` | FK NOT NULL |
| `fornecedor` | `String` | max 120 |
| `cnpjFornecedor` | `String` | max 18 |
| `numeroNf` | `String` | max 30 |
| `dataEmissao` | `LocalDate` | |
| `valorTotalNf` | `BigDecimal` | |
| `imagemPath` | `String` | Caminho da imagem escaneada |
| `ocrStatus` | `StatusOcr` | PENDENTE / PROCESSADO / ERRO |
| `importadoEm` | `LocalDateTime` | |

### `StatusOs` (enum)

```java
public enum StatusOs {
    ABERTA, EM_ANDAMENTO, CONCLUIDA, ENTREGUE
}
```

---

## DTOs

O backend usa DTOs para evitar serialização circular (as entidades têm `@JsonIgnore` nos relacionamentos).

### `OrdemServicoDTO` (record)

Dois factory methods estáticos:
- `OrdemServicoDTO.completo(os, pecas, servicos)` — inclui veículo, cliente e itens. Usado em `GET /ordens/{id}`.
- `OrdemServicoDTO.resumo(os)` — inclui veículo e cliente, sem itens. Usado nas listagens.

### `AtualizarOsRequest` (record)

```java
public record AtualizarOsRequest(
    Integer kmEntrada,
    LocalDate dataSaidaPrevista,
    String reclamacoes,
    String diagnostico
) {}
```

Status, totais e veículo **não entram aqui** — status tem endpoint próprio com regra de transição, totais são calculados por trigger, e o veículo é definido na criação.

### `VeiculoResumoDTO`

Inclui os dados do veículo + objeto `ClienteResumoDTO` aninhado (nome, CPF, telefone).

---

## Endpoints

### Clientes — `/api/clientes`

| Método | Endpoint | Descrição | Status |
|---|---|---|---|
| GET | `/api/clientes` | Listar todos ou filtrar com `?q=` (nome, CPF, telefone) | 200 |
| GET | `/api/clientes/{id}` | Buscar cliente por ID | 200 / 404 |
| GET | `/api/clientes/{id}/veiculos` | Listar veículos do cliente | 200 / 404 |
| POST | `/api/clientes` | Cadastrar novo cliente | 201 / 400 |
| PUT | `/api/clientes/{id}` | Atualizar dados do cliente | 200 / 404 |
| DELETE | `/api/clientes/{id}` | Inativar cliente (`ativo = false`) | 204 / 404 |

### Veículos — `/api/veiculos`

| Método | Endpoint | Descrição | Status |
|---|---|---|---|
| GET | `/api/veiculos/{id}` | Buscar veículo por ID | 200 / 404 |
| GET | `/api/veiculos/placa/{placa}` | Buscar por placa | 200 / 404 |
| POST | `/api/veiculos/cliente/{clienteId}` | Cadastrar veículo vinculado a um cliente | 201 / 404 |
| PUT | `/api/veiculos/{id}` | Atualizar dados do veículo | 200 / 404 |
| DELETE | `/api/veiculos/{id}` | Inativar veículo | 204 / 404 |

### Ordens de Serviço — `/api/ordens`

| Método | Endpoint | Descrição | Status |
|---|---|---|---|
| GET | `/api/ordens` | Buscar OS com `?q=` (cliente ou placa) | 200 |
| GET | `/api/ordens/abertas` | Listar OS com status ABERTA | 200 |
| GET | `/api/ordens/periodo` | Listar OS por `?inicio=&fim=` (ISO date) | 200 |
| GET | `/api/ordens/{id}` | Detalhe completo (veículo + cliente + itens) | 200 / 404 |
| GET | `/api/ordens/veiculo/{veiculoId}` | Histórico de OS de um veículo | 200 |
| POST | `/api/ordens/veiculo/{veiculoId}` | Criar nova OS para o veículo | 201 / 404 |
| PUT | `/api/ordens/{id}` | Atualizar km, previsão, reclamações e diagnóstico | 200 / 404 / 409 |
| PATCH | `/api/ordens/{id}/status?status=` | Avançar status da OS | 200 / 404 / 409 |
| POST | `/api/ordens/{id}/pecas` | Adicionar peça à OS | 201 / 404 |
| DELETE | `/api/ordens/{osId}/pecas/{itemId}` | Remover peça da OS | 200 / 404 |
| POST | `/api/ordens/{id}/servicos` | Adicionar serviço à OS | 201 / 404 |
| DELETE | `/api/ordens/{osId}/servicos/{itemId}` | Remover serviço da OS | 200 / 404 |
| GET | `/api/ordens/{id}/pdf` | Gerar PDF do recibo | 200 / 500 |

> `PUT` e `PATCH /status` retornam `409 Conflict` se a OS estiver `ENTREGUE`. O `PATCH` também retorna `409` ao tentar marcar `ENTREGUE` sem estar `CONCLUIDA`.

---

## Serviços

### `OrdemServicoService`

| Método | Descrição |
|---|---|
| `criar(veiculoId, os)` | Vincula ao veículo, define status ABERTA, persiste |
| `atualizar(id, dados)` | Atualiza km, previsão, reclamações e diagnóstico. Lança 409 se ENTREGUE |
| `atualizarStatus(id, novoStatus)` | Avança o status. ENTREGUE só aceito se atual for CONCLUIDA. Preenche `dataSaidaReal` |
| `adicionarPeca(osId, item)` | Vincula à OS e persiste. Trigger recalcula totais |
| `removerPeca(osId, itemId)` | Valida que o item pertence à OS antes de deletar |
| `adicionarServico(osId, item)` | Idem para serviços |
| `removerServico(osId, itemId)` | Idem |

### `OrdemServicoConsultaService`

Consultas que retornam DTOs (separadas para evitar `LazyInitializationException`):

| Método | Descrição |
|---|---|
| `buscarCompleta(id)` | Força carga do veículo e cliente na transação, retorna `OrdemServicoDTO.completo` |
| `buscar(termo)` | Busca por cliente ou placa, retorna lista de resumos |
| `listarAbertas()` | Filtra por status ABERTA |
| `listarPorPeriodo(inicio, fim)` | Filtra por intervalo de datas |
| `listarPorVeiculo(veiculoId)` | Histórico ordenado por data DESC |

### `ClienteService`

| Método | Descrição |
|---|---|
| `buscar(q)` | Lista todos ou filtra por nome, CPF ou telefone |
| `buscarPorId(id)` | 404 se não encontrado |
| `criar(cliente)` | Persiste e retorna |
| `atualizar(id, dados)` | Copia campos e persiste |
| `inativar(id)` | Seta `ativo = false` (exclusão lógica) |

### `VeiculoService` / `VeiculoConsultaService`

Mesma separação escrita/leitura:
- **`VeiculoService`** — `criar`, `atualizar`, `inativar`
- **`VeiculoConsultaService`** — `buscarPorId`, `buscarPorPlaca`, `listarPorCliente` (retornam `VeiculoResumoDTO`)

### `PdfService`

Delega a geração do PDF para um script Python (`gerar_recibo.py` com ReportLab):
1. Carrega a OS em `@Transactional(readOnly = true)`
2. Serializa dados para JSON (montando o mapa manualmente, sem `@JsonIgnore`)
3. Escreve JSON em arquivo temporário
4. Invoca o processo Python passando o JSON
5. Lê o PDF gerado e retorna como `byte[]`

---

## Tratamento de erros

Classe: `GlobalExceptionHandler` (`@RestControllerAdvice`)

Formato padrão de resposta de erro:

```json
{
  "timestamp": "2026-09-28T19:00:00",
  "status": 404,
  "mensagem": "OS não encontrada: id=999"
}
```

Para erros de validação, o campo `campos` é adicionado:

```json
{
  "timestamp": "...",
  "status": 400,
  "mensagem": "Dados inválidos",
  "campos": {
    "nome": "Nome é obrigatório",
    "telefone1": "Telefone é obrigatório"
  }
}
```

| Exceção | HTTP |
|---|---|
| `RecursoNaoEncontradoException` | 404 Not Found |
| `RegraDeNegocioException` | 409 Conflict |
| `MethodArgumentNotValidException` | 400 Bad Request |

---

## CORS

Classe: `WebConfig`

```java
registry.addMapping("/api/**")
    .allowedOrigins("http://localhost:4200", "http://127.0.0.1:4200")
    .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
    .allowedHeaders("*")
    .exposedHeaders("Content-Disposition")   // necessário para download do PDF
    .maxAge(3600);
```

Para produção, adicionar a URL do frontend em produção.

---

## Swagger

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/api-docs`

Todos os endpoints têm `@Operation(summary = "...")` e os controllers têm `@Tag(name = "...")`.

---

## PDF e microserviço OCR

### Geração de recibo

O endpoint `GET /api/ordens/{id}/pdf` aciona o `PdfService` que:
1. Busca a OS no banco
2. Monta o JSON manualmente (evitando `@JsonIgnore`)
3. Chama o processo Python `gerar_recibo.py`
4. Retorna `Content-Type: application/pdf` + `Content-Disposition: attachment`

**Requisito:** Python 3.12 + ReportLab instalados.

```bash
pip install reportlab
```

O caminho do Python está hardcoded em `PdfService.java`. Se precisar mudar de máquina, ajuste a constante `PYTHON` ou use `python3` se estiver no `PATH`.

### Microserviço OCR

Pasta `mecanica-dbo-ocr/ocr_service.py`. Processa imagens de notas fiscais e extrai dados para a tabela `nota_fiscal`. Roda independentemente em `http://localhost:5000`.

---

## Decisões de arquitetura

### Separação Service / ConsultaService

Para OS e Veículo há dois services:
- **`XyzService`** — escrita. Retorna entidades.
- **`XyzConsultaService`** — leitura. Retorna DTOs dentro de `@Transactional`, evitando `LazyInitializationException`.

### Totais por trigger, não por código Java

O backend nunca soma peças e serviços. O PostgreSQL faz isso em tempo real via trigger, garantindo consistência mesmo que a API seja chamada diretamente.

### OS ENTREGUE é imutável

`PUT /ordens/{id}` lança `RegraDeNegocioException` (409) se `status == ENTREGUE`. A OS entregue é o histórico permanente do veículo.

### Status avança um passo por vez

A regra que o backend impõe é: `ENTREGUE` só é aceito quando o status atual for `CONCLUIDA`. O frontend reforça o avanço linear com botão único.

### Jackson sem nulos e datas como string

```properties
spring.jackson.default-property-inclusion=non_null
spring.jackson.serialization.write-dates-as-timestamps=false
```

Campos nulos não aparecem no JSON. Datas em ISO (`2026-09-28`).

### @JsonIgnore nos relacionamentos

Todos os `@ManyToOne` e `@OneToMany` têm `@JsonIgnore` para evitar serialização circular. Os dados relacionados chegam ao cliente via DTOs construídos dentro da transação.
