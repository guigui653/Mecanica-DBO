# Painel do dia — como aplicar

## Arquivos

| Arquivo | Destino |
|---|---|
| `dashboard.ts` | `src/app/features/dashboard/dashboard.ts` |
| `dashboard.html` | `src/app/features/dashboard/dashboard.html` |
| `dashboard.css` | `src/app/features/dashboard/dashboard.css` |

Se o seu componente hoje se chama diferente (ex: `dashboard.component.ts`),
renomeie os arquivos ou ajuste `templateUrl` e `styleUrl` dentro do `.ts`.

## Passo obrigatório — registrar o locale pt-BR

O painel formata moeda e data em português. Sem isso o Angular lança
`NG0701: Missing locale data`.

Em `src/main.ts`, antes do `bootstrapApplication`:

```typescript
import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';

registerLocaleData(localePt, 'pt-BR');
```

E em `src/app/app.config.ts`, adicione o provider:

```typescript
import { LOCALE_ID } from '@angular/core';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([apiInterceptor])),
    provideAnimationsAsync(),
    { provide: LOCALE_ID, useValue: 'pt-BR' }   // ← adicionar
  ]
};
```

## Rota

Confirme que `app.routes.ts` aponta para o componente com o nome certo:

```typescript
{
  path: 'dashboard',
  loadComponent: () => import('./features/dashboard/dashboard').then(m => m.Dashboard)
}
```

## O que o painel mostra

Quatro métricas no topo e dois blocos abaixo. Todos os números vêm da API real
— não há valor fixo em lugar nenhum.

| Bloco | Fonte |
|---|---|
| No pátio agora | OS com status `ABERTA` ou `EM_ANDAMENTO` |
| Prontas para entrega | OS com status `CONCLUIDA` |
| Faturamento do mês | soma de `totalGeral` das OS `ENTREGUE` do mês corrente |
| Clientes | total de `GET /clientes`, com contagem de novos no mês |
| Veículos no pátio | tabela ordenada pela entrada mais antiga |
| Atalhos | navegação |

A coluna "Parado há" calcula os dias desde `dataEntrada` — serve para o
mecânico ver rapidamente qual carro está encalhado.

## Blocos do protótipo que ficaram de fora

O protótipo do Claude Design tinha oito blocos. Três dependem de entidades que
não existem no banco:

| Bloco | O que falta |
|---|---|
| Orçamentos a aprovar | entidade `Orcamento` + endpoints |
| Agenda de hoje | entidade `Agendamento` + endpoints |
| Estoque baixo | entidade `Peca` de catálogo com saldo |

Preenchê-los com número fixo deixaria a tela bonita e o dado errado. Quando
essas entidades existirem no backend, os blocos entram.

## Requisições

O painel faz duas chamadas em paralelo (`forkJoin`) no `ngOnInit`:
`GET /ordens` e `GET /clientes`. Todo o resto é calculado no cliente com
`computed()`, sem ida extra ao servidor.

Se a API estiver fora do ar, aparece um aviso vermelho com botão de tentar de
novo, em vez de a tela ficar em branco.
