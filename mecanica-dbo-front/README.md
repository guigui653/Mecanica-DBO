# Mecânica DBO — Frontend

Interface web do sistema de gestão da **Mecânica DBO**, construída em **Angular 22** com design system próprio em Vanilla CSS. Conecta-se à API REST do backend Spring Boot em `http://localhost:8080/api`.

---

## Sumário

- [Stack e versões](#stack-e-versões)
- [Como rodar](#como-rodar)
- [Estrutura de pastas](#estrutura-de-pastas)
- [Design System](#design-system)
- [Roteamento](#roteamento)
- [Shell da aplicação](#shell-da-aplicação)
- [Telas](#telas)
- [Serviços Core](#serviços-core)
- [Modelos Shared](#modelos-shared)
- [Interceptor HTTP](#interceptor-http)
- [Internacionalização](#internacionalização)
- [Build e testes](#build-e-testes)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Decisões de arquitetura](#decisões-de-arquitetura)

---

## Stack e versões

| Tecnologia | Versão |
|---|---|
| Angular | 22.1.x |
| TypeScript | 6.0.x |
| RxJS | 7.8.x |
| Angular CDK / Material | 22.1.x (CDK apenas) |
| Node / npm | 22.x / 11.x |
| Tipografia | Inter + Fraunces (Google Fonts) |
| CSS | Vanilla CSS — design system global em `src/styles.css` |
| Testes | Vitest 4 + jsdom |

> **Sem TailwindCSS, sem Bootstrap.** Todo estilo vem do design system em `styles.css` ou dos arquivos `.css` de cada componente.

---

## Como rodar

```bash
# 1. Instalar dependências (apenas na primeira vez)
npm install

# 2. Subir o servidor de desenvolvimento
npm start
# Acesse: http://localhost:4200

# 3. Build de produção (opcional)
npm run build
# Saída em: dist/mecanica-dbo-front/
```

> O backend precisa estar rodando em `http://localhost:8080` para que as chamadas de API funcionem.
> O interceptor prefixia todas as URLs relativas com a `apiUrl` do environment automaticamente.

---

## Estrutura de pastas

```
src/
├── app/
│   ├── app.ts               # Componente raiz — sidebar + router-outlet
│   ├── app.html             # Shell: sidebar fixa + main com router-outlet
│   ├── app.css              # Estilos do shell e da sidebar
│   ├── app.routes.ts        # Tabela de rotas da aplicação
│   ├── app.config.ts        # ApplicationConfig (providers globais)
│   │
│   ├── core/
│   │   ├── interceptors/
│   │   │   └── api.interceptor.ts   # Prefixa URLs relativas com apiUrl
│   │   └── services/
│   │       ├── ordem-servico.ts     # OrdemServicoService
│   │       ├── cliente.ts           # ClienteService
│   │       └── veiculo.ts           # VeiculoService
│   │
│   ├── features/
│   │   ├── dashboard/
│   │   │   ├── dashboard.ts
│   │   │   ├── dashboard.html
│   │   │   └── dashboard.css
│   │   ├── ordens/
│   │   │   ├── lista-os/
│   │   │   │   ├── lista-os.ts
│   │   │   │   ├── lista-os.html
│   │   │   │   └── lista-os.css
│   │   │   └── detalhe-os/
│   │   │       ├── detalhe-os.ts
│   │   │       ├── detalhe-os.html
│   │   │       └── detalhe-os.css
│   │   ├── clientes/
│   │   │   └── lista-clientes/
│   │   │       ├── lista-clientes.ts
│   │   │       ├── lista-clientes.html
│   │   │       └── lista-clientes.css
│   │   └── veiculos/
│   │       ├── busca-veiculo/
│   │       │   ├── busca-veiculo.ts
│   │       │   ├── busca-veiculo.html
│   │       │   └── busca-veiculo.css
│   │       └── form-veiculo/
│   │           └── form-veiculo.ts
│   │
│   └── shared/
│       ├── components/
│       │   └── sidebar/           # Componente legado (substituído pelo app.html)
│       └── models/
│           ├── ordem-servico.model.ts
│           ├── cliente.model.ts
│           └── veiculo.model.ts
│
├── environments/
│   ├── environment.ts             # dev — apiUrl: http://localhost:8080/api
│   └── environment.prod.ts        # prod
├── styles.css                     # Design system global
├── main.ts                        # Bootstrap + registro do locale pt-BR
└── index.html                     # Ponto de entrada HTML
```

---

## Design System

Definido em `src/styles.css`. Todas as telas usam essas classes — nenhum componente inventa estilo do zero.

### Tokens CSS (`:root`)

| Variável | Valor / Uso |
|---|---|
| `--color-bg` | `#faf9f7` — fundo geral |
| `--color-surface` | `#ffffff` — fundo de cards e inputs |
| `--color-text` | `#1a1917` — texto principal |
| `--color-text-soft` | texto secundário (60% opaco) |
| `--color-text-faint` | texto terciário (45% opaco) |
| `--color-accent` | `#b68235` — dourado da oficina |
| `--color-accent-700` | `#8f6526` — dourado escuro |
| `--color-divider` | bordas e separadores |
| `--color-dark` | `#1a1917` — sidebar |
| `--font-heading` | Fraunces (serifada elegante) |
| `--font-body` | Inter (sans-serif limpa) |
| `--radius` | `4px` |

### Cores de status da OS

| Status | Cor |
|---|---|
| `ABERTA` | `#b68235` (dourado) |
| `EM_ANDAMENTO` | `#2b6cb0` (azul) |
| `CONCLUIDA` | `#2f855a` (verde) |
| `ENTREGUE` | `#4a5568` (cinza) |

### Classes globais

| Classe | Descrição |
|---|---|
| `.card` | Container branco com borda e padding |
| `.card-header` | Flex row com título e ações |
| `.table` | Tabela com cabeçalho uppercase |
| `.badge` | Pílula de status com ponto colorido |
| `.badge-aberta/em-andamento/concluida/entregue` | Variantes de cor |
| `.placa` | Placa visual (borda, letter-spacing) |
| `.btn`, `.btn-primary`, `.btn-secondary`, `.btn-ghost`, `.btn-danger` | Botões |
| `.field`, `.input`, `.textarea`, `.select` | Formulários |
| `.toast`, `.toast-wrap`, `.toast-error` | Notificações flutuantes |
| `.metric` | Card de KPI do painel |
| `.view-header`, `.view-content` | Estrutura de página |
| `.page`, `.page-header`, `.page-title` | Estrutura alternativa |
| `.grid-2/3/4`, `.row`, `.row-between`, `.col` | Helpers de layout |
| `.tag`, `.tag-accent`, `.tag-neutral`, `.tag-outline` | Tags de situação |
| `.chip`, `.chip.active` | Filtros em pílula |
| `.seg`, `.seg-opt` | Controle segmentado |
| `.dialog-backdrop`, `.dialog`, `.dialog-body`, `.dialog-actions` | Modais |

---

## Roteamento

Arquivo: `src/app/app.routes.ts`

| Rota | Componente | Lazy? |
|---|---|---|
| `/` | Redireciona para `/dashboard` | — |
| `/dashboard` | `Dashboard` | Sim |
| `/clientes` | `ListaClientes` | Sim |
| `/veiculos` | `BuscaVeiculo` | Sim |
| `/ordens` | `ListaOs` | Sim |
| `/ordens/nova` | `NovaOs` | Sim |
| `/ordens/:id` | `DetalheOs` | Sim |

> A rota estática `/ordens/nova` deve vir antes de `/ordens/:id`. A rota de detalhe exige o segmento de ID e não captura `/ordens` sozinha.

Todos os componentes usam `loadComponent` — lazy loading automático do Angular 17+.

---

## Shell da aplicação

Arquivo: `src/app/app.ts` + `src/app/app.html`

A aplicação é um shell com **sidebar fixa à esquerda** e `<router-outlet>` no conteúdo à direita.

### O que o shell faz

- Exibe a **sidebar de navegação** com links para as 4 seções
- Mostra **contadores ao vivo** no menu:
  - "Ordens de Serviço" — número de OS abertas (via `GET /ordens/abertas`)
  - "Clientes" — total de clientes (via `GET /clientes`)
- Exibe **indicador de status da API** no rodapé:
  - Ponto verde + hora da última sincronização quando online
  - "API offline" quando a chamada falha
- Os contadores são carregados no `ngOnInit` via `HttpClient` direto no componente raiz, usando `/ordens/abertas` como health check implícito

---

## Telas

### Painel (Dashboard)

**Rota:** `/dashboard` | **Arquivo:** `features/dashboard/dashboard.ts`

Carrega dados reais via `forkJoin` (ordens + clientes em paralelo) e exibe métricas computadas com `signal` + `computed`.

#### Métricas

| Métrica | Fonte |
|---|---|
| Carros no pátio | OS com status `ABERTA` ou `EM_ANDAMENTO` |
| Prontos para entrega | OS com status `CONCLUIDA` |
| Faturamento do mês | Soma de `totalGeral` das OS `ENTREGUE` no mês corrente |
| Clientes novos | Clientes com `criadoEm` no mês corrente |

#### Lista "Pátio"

Ordenada do mais antigo ao mais recente (encalhado no topo). Linha clicável navega para `/ordens/:id`.

#### Comportamento offline

Se `GET /ordens` falhar, exibe mensagem de erro com botão "Tentar novamente".

---

### Lista de OS

**Rota:** `/ordens` | **Arquivo:** `features/ordens/lista-os/lista-os.ts`

Carrega somente as ordens retornadas pela API e mostra placa, veículo e proprietário reais. Sem registros, apresenta estado vazio. Falhas da API apresentam mensagem e botão de tentar novamente. O campo mecânico fica sem valor porque a API atual não fornece essa informação.

#### Filtros

| Filtro | Status correspondente |
|---|---|
| Todas | Todos |
| Em serviço | `EM_ANDAMENTO` |
| Aguardando | `ABERTA` |
| Prontas | `CONCLUIDA` |
| Entregues | `ENTREGUE` |

Campo de busca por texto livre (número, placa, carro, cliente, mecânico).

#### Clique em linha

- OS com `id` real → navega para `/ordens/:id`
- Não são acrescentadas ordens de demonstração.

---

### Nova OS

**Rota:** `/ordens/nova` | **Arquivo:** `features/ordens/nova-os/nova-os.ts`

Busca o veículo cadastrado pela placa, recebe quilometragem, previsão e reclamações, e cria a OS pelo endpoint existente. Ao criar, navega para o detalhe da OS retornada. Os botões de nova ordem nas demais telas usam essa rota.

---

### Detalhe da OS

**Rota:** `/ordens/:id` | **Arquivo:** `features/ordens/detalhe-os/detalhe-os.ts`

Tela principal de trabalho do mecânico. Usa exclusivamente `signal` e `computed` do Angular — sem `@Input`, sem NgRx.

#### Seções

**1. Cabeçalho**
- Número da OS, badge de status, dias do carro na oficina
- Botão "Baixar recibo" (PDF via `GET /ordens/{id}/pdf`)
- Botão de avanço de status (um passo por vez)

**2. Faixa do veículo**
- Placa em destaque, modelo, cor, combustível, proprietário com telefone

**3. Queixa e diagnóstico**
- Campos: quilometragem, previsão de entrega, reclamações do cliente, laudo técnico
- Botão "Salvar alterações" aparece **apenas quando há mudança pendente** (detectado via `snapshotForm()`)
- Salvo via `PUT /ordens/{id}`
- Inclusões e remoções de itens preservam os campos ainda não salvos.
- Status e recibo exigem salvar as alterações pendentes antes de continuar.
- Há confirmação ao sair sem salvar e proteção ao fechar ou recarregar a página.
- Quantidades, valores e quilometragem são validados antes do envio.

**4. Peças**
- Tabela com todas as peças da OS
- Peças com `pagoPeloCliente = true` aparecem com valor riscado e tag "Pago pelo cliente"
- Linha de inclusão no final (descrição, quantidade, valor, checkbox "Pago pelo cliente")
- Adição via `POST /ordens/{osId}/pecas`
- Remoção via `DELETE /ordens/{osId}/pecas/{itemId}` com confirmação

**5. Serviços / Mão de obra**
- Mesma estrutura da tabela de peças, sem coluna de quantidade
- Adição via `POST /ordens/{osId}/servicos`
- Remoção via `DELETE /ordens/{osId}/servicos/{itemId}`

**6. Totais (lateral sticky)**
- Total de peças, total de serviços, total geral
- Nota sobre o valor excluído de peças pagas pelo cliente
- O backend recalcula tudo via trigger do PostgreSQL — o frontend não soma nada

#### Fluxo de status (botão único)

```
ABERTA         → [Iniciar serviço]      → EM_ANDAMENTO
EM_ANDAMENTO   → [Concluir serviço]     → CONCLUIDA
CONCLUIDA      → [Entregar ao cliente]  → ENTREGUE
```

Ao entregar, exige confirmação com `confirm()`. Via `PATCH /ordens/{id}/status?status=`.

#### Modo somente leitura (OS ENTREGUE)

- Todos os campos ficam `disabled`
- Botões de remover e linhas de inclusão desaparecem
- Aparece aviso dourado explicando que a OS é histórico imutável
- O backend também recusa edição via `PUT` com `409 Conflict`

#### Toast

Todas as ações (salvar, adicionar, remover, erro) disparam toast no canto inferior direito.
Desaparece em 3 s (sucesso) ou 6 s (erro).

---

### Lista de Clientes

**Rota:** `/clientes` | **Arquivo:** `features/clientes/lista-clientes/lista-clientes.ts`

- Busca por nome, CPF ou telefone via `GET /clientes?q=`
- Cadastro de novo cliente via modal
- Edição inline via modal
- Última visita e filtros derivados das OS reais; contagem de veículos carregada ao selecionar o cliente
- Sem identificação fictícia de frotas e sem datas ou contagens inventadas

---

### Busca de Veículo

**Rota:** `/veiculos` | **Arquivo:** `features/veiculos/busca-veiculo/busca-veiculo.ts`

- Campo de busca por placa → `GET /veiculos/placa/{placa}`
- Exibe o proprietário aninhado na resposta de veículo e carrega o histórico real via `GET /ordens/veiculo/{veiculoId}`
- Distingue placa não cadastrada de API indisponível; não utiliza dados fictícios
- Atalhos para placas consultadas de verdade na sessão
- Cadastro abre o formulário de veículo existente; nova OS pode receber a placa selecionada
- Próxima revisão fica não informada, pois a API ainda não fornece esse campo

---

## Serviços Core

Todos usam `inject(HttpClient)` e são `providedIn: 'root'`.

### OrdemServicoService — `core/services/ordem-servico.ts`

| Método | Verbo | Endpoint |
|---|---|---|
| `listar(status?, q?)` | GET | `/ordens` + filtro local por status |
| `buscar(q?)` | GET | `/ordens?q=` |
| `listarAbertas()` | GET | `/ordens/abertas` |
| `listarPorPeriodo(inicio, fim)` | GET | `/ordens/periodo` |
| `buscarPorId(id)` | GET | `/ordens/{id}` |
| `listarPorVeiculo(veiculoId)` | GET | `/ordens/veiculo/{veiculoId}` |
| `criar(veiculoId, os)` | POST | `/ordens/veiculo/{veiculoId}` |
| `atualizar(id, dados)` | PUT | `/ordens/{id}` |
| `alterarStatus(id, status)` | PATCH | `/ordens/{id}/status?status=` |
| `adicionarPeca(osId, item)` | POST | `/ordens/{osId}/pecas` |
| `removerPeca(osId, itemId)` | DELETE | `/ordens/{osId}/pecas/{itemId}` |
| `adicionarServico(osId, item)` | POST | `/ordens/{osId}/servicos` |
| `removerServico(osId, itemId)` | DELETE | `/ordens/{osId}/servicos/{itemId}` |
| `gerarPdf(osId)` | GET | `/ordens/{osId}/pdf` (Blob) |

> Métodos de itens devolvem a OS inteira já com totais recalculados pelo trigger do banco — sem GET extra.

### ClienteService — `core/services/cliente.ts`

| Método | Verbo | Endpoint |
|---|---|---|
| `listar(q?)` | GET | `/clientes?q=` |
| `buscarPorId(id)` | GET | `/clientes/{id}` |
| `listarVeiculos(id)` | GET | `/clientes/{id}/veiculos` |
| `criar(cliente)` | POST | `/clientes` |
| `atualizar(id, cliente)` | PUT | `/clientes/{id}` |
| `inativar(id)` | DELETE | `/clientes/{id}` |

### VeiculoService — `core/services/veiculo.ts`

| Método | Verbo | Endpoint |
|---|---|---|
| `buscarPorPlaca(placa)` | GET | `/veiculos/placa/{placa}` |
| `buscarPorId(id)` | GET | `/veiculos/{id}` |
| `listarPorCliente(clienteId)` | GET | `/clientes/{clienteId}/veiculos` |
| `criar(clienteId, veiculo)` | POST | `/veiculos/cliente/{clienteId}` |
| `atualizar(id, veiculo)` | PUT | `/veiculos/{id}` |
| `inativar(id)` | DELETE | `/veiculos/{id}` |

---

## Modelos Shared

### `ordem-servico.model.ts`

```typescript
type StatusOs = 'ABERTA' | 'EM_ANDAMENTO' | 'CONCLUIDA' | 'ENTREGUE'

interface ClienteResumo   { id, nome, cpf?, telefone1 }
interface VeiculoResumo   { id, placa, marca, modelo, cor, combustivel, chassi?, anoFabricacao?, cliente? }
interface ItemPeca        { id?, descricao, quantidade, valorUnitario, valorTotal?, pagoPeloCliente? }
interface ItemServico     { id?, descricao, valor }
interface OrdemServico    { id?, status, kmEntrada?, dataEntrada?, dataSaidaPrevista?,
                            dataSaidaReal?, reclamacoes?, diagnostico?,
                            totalPecas?, totalServicos?, totalGeral?,
                            veiculo?, itensPeca?, itensServico? }
interface AtualizarOsRequest { kmEntrada?, dataSaidaPrevista?, reclamacoes?, diagnostico? }

const ROTULOS_STATUS: Record<StatusOs, string>
const PROXIMO_STATUS: Partial<Record<StatusOs, { status: StatusOs; rotulo: string }>>
```

### `cliente.model.ts`

```typescript
interface Cliente { id?, nome, cpf?, telefone1, telefone2?, email?, ativo?, criadoEm? }
```

### `veiculo.model.ts`

```typescript
type Combustivel = 'GASOLINA' | 'ETANOL' | 'FLEX' | 'DIESEL' | 'ELETRICO' | 'GNV'
interface Veiculo { id?, placa, marca, modelo, cor, combustivel, chassi?, anoFabricacao?, ativo?, criadoEm? }
```

---

## Interceptor HTTP

Arquivo: `src/app/core/interceptors/api.interceptor.ts`

```typescript
export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.url.startsWith('http')) return next(req);
  return next(req.clone({ url: `${environment.apiUrl}${req.url}` }));
};
```

Permite que os services usem caminhos relativos (`/ordens`, `/clientes`) sem conhecer o host da API. Em produção, basta trocar `apiUrl` no environment.

---

## Internacionalização

`src/main.ts`:
```typescript
import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
registerLocaleData(localePt, 'pt-BR');
```

`src/app/app.config.ts`:
```typescript
{ provide: LOCALE_ID, useValue: 'pt-BR' }
```

Habilita pipes nativos com formatação brasileira: valores `R$ 1.240,00`, datas `28/09/2026`.

---

## Build e testes

```bash
npm start        # Desenvolvimento com hot reload → http://localhost:4200
npm run build    # Build de produção → dist/mecanica-dbo-front/
npm run watch    # Build em modo watch
npm test         # Testes unitários (Vitest + jsdom)
```

### Budget de CSS

Angular configurado com budget de `4 kB` por arquivo CSS de componente. O `dashboard.css` excede em ~342 bytes — aviso, não erro. Ajustável em `angular.json`.

---

## Variáveis de ambiente

`src/environments/environment.ts`:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  ocrUrl: 'http://localhost:5000'   // microserviço Python de OCR (nota fiscal)
};
```

Para produção, edite `environment.prod.ts` com as URLs reais.

---

## Decisões de arquitetura

### Standalone Components (sem NgModules)
Todos os componentes usam `standalone: true`. Imports são declarados por componente.

### Signals para estado local, RxJS para HTTP
`DetalheOs` usa `signal` e `computed` para todo estado da tela. RxJS é usado apenas onde o `HttpClient` retorna observables — sem `Subject` ou `BehaviorSubject` manual.

### Nenhum estado global
Sem NgRx, Akita ou similar. Cada tela gerencia seu próprio estado. Dados compartilhados (contadores da sidebar) são carregados independentemente onde precisam.

### OrdemServicoService é um superset do pacote detalhe-os
O `ordem-servico.ts` tem o método `listar(status?, q?)` extra, usado pelo `lista-os.ts`. O arquivo do pacote `detalhe-os-mecanica-dbo` é um subconjunto — **não substituir**.

### Dados reais nas telas de consulta
`lista-os`, `busca-veiculo` e `lista-clientes` usam informações da API. Dados ausentes permanecem não informados; falhas de conexão são mostradas separadamente de listas vazias e cadastros não encontrados.

### PDF gerado no backend
O botão "Baixar recibo" chama `GET /ordens/{id}/pdf` e recebe um `Blob`. O backend monta o PDF só com Java (Thymeleaf + Flying Saucer) — o frontend cria um link temporário e aciona o download.
