# Como aplicar o design no projeto Angular

## Onde vai cada arquivo

| Arquivo | Destino no projeto |
|---|---|
| `styles.css` | `src/styles.css` — **substitui** o existente |
| `app.html` | `src/app/app.html` — substitui |
| `app.css` | `src/app/app.css` — substitui |
| `app.ts` | `src/app/app.ts` — substitui |

## Depois de copiar

O `app.ts` usa `HttpClient`, então confirme que o `app.config.ts` já tem
`provideHttpClient(withInterceptors([apiInterceptor]))` — se você seguiu o
README, já está lá.

Rode `ng serve` e abra `http://localhost:4200`.

## Classes disponíveis

Use nos seus componentes em vez de escrever CSS do zero.

### Botões
```html
<button class="btn btn-primary">Salvar</button>
<button class="btn btn-secondary">Cancelar</button>
<button class="btn btn-ghost">Editar</button>
<button class="btn btn-danger">Inativar</button>
<button class="btn btn-primary btn-block">Ocupa a largura toda</button>
```

### Campos
```html
<div class="field">
  <label>Placa</label>
  <input class="input" [(ngModel)]="placa">
  <span class="field-error">Placa inválida</span>
</div>

<div class="field">
  <label>Combustível</label>
  <select class="select">...</select>
</div>

<div class="field">
  <label>Diagnóstico</label>
  <textarea class="textarea"></textarea>
</div>
```

### Tabela
```html
<table class="table">
  <thead>
    <tr>
      <th>Cliente</th>
      <th>Placa</th>
      <th class="col-right">Total</th>
      <th class="col-actions"></th>
    </tr>
  </thead>
  <tbody>
    <tr class="clickable" (click)="abrir(os)">
      <td>{{ os.veiculo.cliente.nome }}</td>
      <td><span class="placa">{{ os.veiculo.placa }}</span></td>
      <td class="col-right">{{ os.totalGeral | currency:'BRL' }}</td>
      <td class="col-actions">
        <button class="btn btn-ghost">Abrir</button>
      </td>
    </tr>
  </tbody>
</table>
```

### Badge de status
As classes seguem o enum do backend em minúsculo com hífen:

```html
<span class="badge" [class]="'badge badge-' + os.status.toLowerCase().replace('_','-')">
  {{ os.status }}
</span>
```

| Status | Classe |
|---|---|
| `ABERTA` | `badge-aberta` |
| `EM_ANDAMENTO` | `badge-em-andamento` |
| `CONCLUIDA` | `badge-concluida` |
| `ENTREGUE` | `badge-entregue` |

### Cabeçalho de página
```html
<div class="page">
  <div class="page-header">
    <div>
      <h1 class="page-title">Ordens de Serviço</h1>
      <p class="page-subtitle">12 em aberto</p>
    </div>
    <button class="btn btn-primary">Nova OS</button>
  </div>

  <div class="toolbar">
    <input class="input" placeholder="Buscar por placa ou cliente">
    <div class="filter-group">
      <button class="filter-chip active">Todas</button>
      <button class="filter-chip">Abertas</button>
    </div>
  </div>
</div>
```

### Cards de métrica (painel)
```html
<div class="grid-4">
  <div class="metric">
    <div class="metric-label">OS abertas</div>
    <div class="metric-value">12</div>
    <div class="metric-note">3 entram hoje</div>
  </div>
</div>
```

### Diálogo
```html
<div class="dialog-backdrop" (click)="fechar()">
  <div class="dialog" (click)="$event.stopPropagation()">
    <h2 class="dialog-title">Novo cliente</h2>
    <div class="dialog-body">
      <!-- campos -->
    </div>
    <div class="dialog-actions">
      <button class="btn btn-secondary" (click)="fechar()">Cancelar</button>
      <button class="btn btn-primary" (click)="salvar()">Salvar</button>
    </div>
  </div>
</div>
```

## Sobre o Angular Material

O design system é CSS puro e não depende do Material. Se você já instalou o
Material e quer manter os dialogs e snackbars dele, os dois convivem — mas os
componentes do Material vão manter a aparência padrão deles, não a da oficina.
Para visual consistente, prefira as classes acima.

## Cores

| Variável | Valor | Uso |
|---|---|---|
| `--color-dark` | `#1a1917` | sidebar, login, toast |
| `--color-accent` | `#b68235` | dourado, ações principais |
| `--color-bg` | `#faf9f7` | fundo das páginas |
| `--color-surface` | `#ffffff` | cards e tabelas |

As fontes (Fraunces para títulos, Inter para corpo) vêm do Google Fonts pelo
`@import` no topo do `styles.css` — precisa de internet na primeira carga. Para
uso offline na oficina, baixe os arquivos `.woff2` e troque o import por
`@font-face` local.
