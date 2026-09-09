# Mecânica DBO — Frontend Angular

Aplicação web em Angular 22 com Angular Material, responsável pela interface de gestão da oficina Mecânica DBO.
Consome a API REST do backend Spring Boot 3.3.3.

---

## Índice

- [Tecnologias](#tecnologias)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Configuração inicial](#configuração-inicial)
- [Rotas](#rotas)
- [Módulos e funcionalidades](#módulos-e-funcionalidades)
- [Serviços (core)](#serviços-core)
- [Modelos de dados](#modelos-de-dados)
- [Integração com o backend](#integração-com-o-backend)
- [Testes](#testes)
- [Como executar](#como-executar)

---

## Tecnologias

- Angular 22 (standalone components)
- Angular Material — tabelas, formulários, dialogs, snackbars
- RxJS — programação reativa / HTTP
- TypeScript
- CSS — estilos dos componentes

---

## Estrutura do projeto

```
src/
├── app/
│   ├── core/
│   │   ├── interceptors/
│   │   │   └── api.interceptor.ts      # prefixo da API + tratamento de erro
│   │   └── services/
│   │       ├── cliente.service.ts
│   │       ├── veiculo.service.ts
│   │       └── ordem-servico.service.ts
│   │
│   ├── features/
│   │   ├── dashboard/
│   │   │   ├── dashboard.ts            # OS abertas + atalhos
│   │   │   ├── dashboard.html
│   │   │   └── dashboard.css
│   │   │
│   │   ├── clientes/
│   │   │   ├── lista-clientes/
│   │   │   │   ├── lista-clientes.ts
│   │   │   │   ├── lista-clientes.html
│   │   │   │   └── lista-clientes.css
│   │   │   └── form-cliente/
│   │   │       └── form-cliente.ts     # dialog de cadastro/edição
│   │   │
│   │   ├── veiculos/
│   │   │   ├── busca-veiculo/
│   │   │   │   ├── busca-veiculo.ts    # busca por placa
│   │   │   │   ├── busca-veiculo.html
│   │   │   │   └── busca-veiculo.css
│   │   │   └── form-veiculo/
│   │   │       └── form-veiculo.ts     # dialog de cadastro/edição
│   │   │
│   │   └── ordens/
│   │       ├── lista-os/
│   │       │   ├── lista-os.ts
│   │       │   ├── lista-os.html
│   │       │   └── lista-os.css
│   │       └── detalhe-os/
│   │           ├── detalhe-os.ts       # itens, status, PDF
│   │           ├── detalhe-os.html
│   │           └── detalhe-os.css
│   │
│   ├── shared/
│   │   └── models/
│   │       ├── cliente.model.ts
│   │       ├── veiculo.model.ts
│   │       └── ordem-servico.model.ts
│   │
│   ├── app.ts
│   ├── app.html
│   ├── app.config.ts
│   └── app.routes.ts
│
├── environments/
│   ├── environment.ts
│   └── environment.prod.ts
├── main.ts
└── styles.css
```

---

## Configuração inicial

### `src/environments/environment.ts`

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  ocrUrl: 'http://localhost:5000'
};
```

### `src/app/app.config.ts`

```typescript
import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { routes } from './app.routes';
import { apiInterceptor } from './core/interceptors/api.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([apiInterceptor])),
    provideAnimationsAsync()
  ]
};
```

### `src/main.ts`

```typescript
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

bootstrapApplication(App, appConfig)
  .catch(err => console.error(err));
```

### Interceptor HTTP — `api.interceptor.ts`

Prefixa a URL base da API. Requisições que já começam com `http` passam intactas
(útil para chamar o serviço de OCR na porta 5000).

```typescript
import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.url.startsWith('http')) {
    return next(req);
  }
  return next(req.clone({
    url: `${environment.apiUrl}${req.url}`
  }));
};
```

---

## Rotas

Todas usam lazy loading com `loadComponent`.

| Path | Componente | Descrição |
|------|-----------|-----------|
| `/` | — | redireciona para `/dashboard` |
| `/dashboard` | `Dashboard` | OS abertas e atalhos |
| `/clientes` | `ListaClientes` | listagem e busca de clientes |
| `/veiculos` | `BuscaVeiculo` | busca de veículo por placa |
| `/ordens` | `ListaOs` | listagem e busca de OS |
| `/ordens/:id` | `DetalheOs` | detalhe da OS, itens e PDF |

---

## Módulos e funcionalidades

### Dashboard

- Lista OS com status ABERTA (`GET /ordens/abertas`)
- Atalhos para nova OS, clientes e busca por placa
- O backend não expõe endpoint de health check. Para monitorar disponibilidade, adicione o Spring Boot Actuator no `pom.xml` e exponha `/actuator/health`.

### Clientes

| Arquivo | Descrição |
|---------|-----------|
| `lista-clientes.ts` | listagem com busca por nome, CPF ou telefone |
| `lista-clientes.html` | tabela: nome, CPF, telefone, e-mail, ações |
| `form-cliente.ts` | dialog de cadastro e edição |

- Listar e buscar por termo livre (`?q=`)
- Cadastrar e editar via dialog
- Inativar com confirmação (exclusão lógica)
- Ver veículos do cliente (`GET /clientes/{id}/veiculos`)

> O backend rejeita CPF duplicado com `409 Conflict` e mensagem no corpo — trate esse status no formulário e exiba a mensagem retornada.

### Veículos

| Arquivo | Descrição |
|---------|-----------|
| `busca-veiculo.ts` | busca por placa (campo principal) |
| `busca-veiculo.html` | dados do veículo + histórico de OS |
| `form-veiculo.ts` | dialog de cadastro e edição |

- Buscar por placa (`GET /veiculos/placa/{placa}`)
- Cadastrar vinculado a um cliente (`POST /veiculos/cliente/{clienteId}`)
- Editar e inativar

> **Combustíveis aceitos pelo banco:** `GASOLINA`, `ETANOL`, `FLEX`, `DIESEL`, `ELETRICO`, `GNV` — use exatamente esses valores no select.

> Não existe endpoint de listagem geral de veículos. A navegação parte da placa ou do cliente.
> Se precisar de uma listagem completa, adicione `GET /api/veiculos` no `VeiculoController`.

### Ordens de serviço

| Arquivo | Descrição |
|---------|-----------|
| `lista-os.ts` | listagem com busca por cliente ou placa |
| `lista-os.html` | tabela: nº OS, cliente, veículo, status, entrada, total |
| `detalhe-os.ts` | detalhe completo, itens, status e PDF |

- Buscar por cliente ou placa (`?q=`)
- Filtrar OS abertas (`GET /ordens/abertas`)
- Filtrar por período (`GET /ordens/periodo?inicio=&fim=`)
- Badges coloridos por status
- Adicionar e remover peças e serviços
- Alterar status
- Gerar PDF do recibo

**Status válidos (enum `StatusOs` no backend):**

```
ABERTA → EM_ANDAMENTO → CONCLUIDA → ENTREGUE
```

> O backend impõe uma regra: `ENTREGUE` só é aceito se a OS estiver `CONCLUIDA`.
> Qualquer outra transição retorna `409 Conflict`.
> Ao marcar como entregue, o backend preenche `dataSaidaReal` automaticamente.

> Os totais (`totalPecas`, `totalServicos`, `totalGeral`) são recalculados por trigger
> no PostgreSQL a cada peça ou serviço inserido. O front **nunca** os envia — apenas
> relê a OS após adicionar ou remover um item.

---

## Serviços (core)

### `ClienteService`

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `listar(q?)` | `GET /clientes?q=` | lista ou filtra por nome, CPF ou telefone |
| `buscarPorId(id)` | `GET /clientes/{id}` | busca por ID |
| `listarVeiculos(id)` | `GET /clientes/{id}/veiculos` | veículos do cliente |
| `criar(c)` | `POST /clientes` | cadastra — 201 Created |
| `atualizar(id, c)` | `PUT /clientes/{id}` | atualiza |
| `inativar(id)` | `DELETE /clientes/{id}` | inativa — 204 No Content |

### `VeiculoService`

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `buscarPorId(id)` | `GET /veiculos/{id}` | busca por ID |
| `buscarPorPlaca(placa)` | `GET /veiculos/placa/{placa}` | busca por placa |
| `criar(clienteId, v)` | `POST /veiculos/cliente/{clienteId}` | cadastra vinculado ao cliente |
| `atualizar(id, v)` | `PUT /veiculos/{id}` | atualiza |
| `inativar(id)` | `DELETE /veiculos/{id}` | inativa — 204 No Content |

### `OrdemServicoService`

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `buscar(q?)` | `GET /ordens?q=` | busca por cliente ou placa |
| `listarAbertas()` | `GET /ordens/abertas` | OS com status ABERTA |
| `listarPorPeriodo(ini, fim)` | `GET /ordens/periodo?inicio=&fim=` | filtra por data (ISO yyyy-MM-dd) |
| `buscarPorId(id)` | `GET /ordens/{id}` | detalhe da OS |
| `criar(veiculoId, os)` | `POST /ordens/veiculo/{veiculoId}` | cria OS — 201 Created |
| `alterarStatus(id, status)` | `PATCH /ordens/{id}/status?status=` | altera status (query param) |
| `adicionarPeca(osId, item)` | `POST /ordens/{osId}/pecas` | adiciona peça — 201 |
| `removerPeca(osId, itemId)` | `DELETE /ordens/{osId}/pecas/{itemId}` | remove peça — 204 |
| `adicionarServico(osId, item)` | `POST /ordens/{osId}/servicos` | adiciona serviço — 201 |
| `removerServico(osId, itemId)` | `DELETE /ordens/{osId}/servicos/{itemId}` | remove serviço — 204 |
| `gerarPdf(osId)` | `GET /ordens/{osId}/pdf` | retorna Blob (application/pdf) |

> O status em `alterarStatus` vai como **query param**, não no corpo:
> ```typescript
> alterarStatus(id: number, status: StatusOs): Observable<OrdemServico> {
>   return this.http.patch<OrdemServico>(`/ordens/${id}/status`, null, { params: { status } });
> }
> ```

> Para o PDF, use `responseType: 'blob'`:
> ```typescript
> gerarPdf(osId: number): Observable<Blob> {
>   return this.http.get(`/ordens/${osId}/pdf`, { responseType: 'blob' });
> }
> ```

### `OcrService` (opcional)

Chama o microserviço Python diretamente na porta 5000.
O interceptor ignora URLs absolutas, então passe a URL completa.

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `health()` | `GET {ocrUrl}/health` | status do serviço |
| `processar(file)` | `POST {ocrUrl}/ocr/processar` | envia imagem (multipart, campo `file`) |

Retorna `{ sucesso, metadados, itens, total_itens, texto_bruto }`.
Os itens devem ser revisados pelo usuário antes de virarem peças da OS.

---

## Modelos de dados

### `cliente.model.ts`
```typescript
export interface Cliente {
  id?: number;
  nome: string;
  cpf?: string;
  telefone1: string;
  telefone2?: string;
  email?: string;
  ativo?: boolean;
  criadoEm?: string;
}
```

### `veiculo.model.ts`
```typescript
export type Combustivel =
  | 'GASOLINA'
  | 'ETANOL'
  | 'FLEX'
  | 'DIESEL'
  | 'ELETRICO'
  | 'GNV';

export interface Veiculo {
  id?: number;
  placa: string;
  marca: string;
  modelo: string;
  cor: string;
  combustivel: Combustivel;
  chassi?: string;
  anoFabricacao?: number;
  ativo?: boolean;
  criadoEm?: string;
}
```

> O backend serializa `Veiculo` sem o objeto `cliente` (marcado com `@JsonIgnore`
> para evitar referência circular). Para saber de quem é o veículo, navegue a partir
> de `GET /clientes/{id}/veiculos`.

### `ordem-servico.model.ts`
```typescript
export type StatusOs = 'ABERTA' | 'EM_ANDAMENTO' | 'CONCLUIDA' | 'ENTREGUE';

export interface ItemPeca {
  id?: number;
  descricao: string;
  quantidade: number;
  valorUnitario: number;
  valorTotal?: number;       // calculado pelo banco — somente leitura
  pagoPeloCliente?: boolean;
  criadoEm?: string;
}

export interface ItemServico {
  id?: number;
  descricao: string;
  valor: number;
  criadoEm?: string;
}

export interface OrdemServico {
  id?: number;
  status: StatusOs;
  kmEntrada?: number;
  dataEntrada?: string;        // ISO yyyy-MM-dd
  dataSaidaPrevista?: string;
  dataSaidaReal?: string;
  reclamacoes?: string;        // queixas do cliente
  diagnostico?: string;        // observações do mecânico
  totalPecas?: number;         // recalculado por trigger
  totalServicos?: number;
  totalGeral?: number;
  criadoEm?: string;
}
```

> `OrdemServico` também é serializada sem `veiculo`, `itensPeca`, `itensServico` e
> `notasFiscais` (todos `@JsonIgnore`). Para montar a tela de detalhe com o veículo e
> os itens, ou você faz chamadas separadas, ou adiciona um DTO de resposta no backend —
> **a segunda opção é a recomendada**.

---

## Integração com o backend

- API base: `http://localhost:8080/api`
- OCR: `http://localhost:5000`
- O interceptor aplica o prefixo apenas em URLs relativas

### CORS

O backend ainda não tem CORS configurado. Sem isso o navegador bloqueia toda requisição
vinda do `localhost:4200`. Crie no backend:

```java
package com.mecanicadbo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:4200")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
                .allowedHeaders("*");
    }
}
```

### Formato de erro

O `GlobalExceptionHandler` do backend retorna sempre o mesmo formato:

```json
{
  "timestamp": "2026-08-27T15:19:40.044851400",
  "status": 409,
  "mensagem": "CPF já cadastrado para: João Mecânico",
  "campos": {
    "nome": "Nome é obrigatório"
  }
}
```

| Status | Quando ocorre |
|--------|--------------|
| `400` | validação falhou — o campo `campos` traz erro por atributo |
| `404` | recurso não encontrado |
| `409` | regra de negócio violada (CPF ou placa duplicada, transição de status inválida) |

> Trate esses três casos no interceptor ou em cada serviço e exiba mensagem ao usuário via snackbar.

---

## Testes

```typescript
// serviços
providers: [
  provideHttpClient(),
  provideHttpClientTesting()
]

// componentes standalone
imports: [NomeDoComponente],
providers: [
  provideHttpClient(),
  provideHttpClientTesting(),
  provideNoopAnimations()
]
```

```bash
npm test
```

---

## Como executar

```bash
npm install
ng serve
```

Acesse `http://localhost:4200`.

**Pré-requisitos rodando em paralelo:**

| Serviço | Porta | Comando |
|---------|-------|---------|
| PostgreSQL | 5432 | serviço do Windows |
| Spring Boot | 8080 | rodar `MecanicaDboApplication` no IntelliJ |
| OCR (opcional) | 5000 | `python ocr_service.py` |
