import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard').then(m => m.Dashboard)
  },
  {
    path: 'clientes',
    loadComponent: () => import('./features/clientes/lista-clientes/lista-clientes').then(m => m.ListaClientes)
  },
  {
    path: 'veiculos',
    loadComponent: () => import('./features/veiculos/busca-veiculo/busca-veiculo').then(m => m.BuscaVeiculo)
  },
  {
    path: 'ordens',
    loadComponent: () => import('./features/ordens/lista-os/lista-os').then(m => m.ListaOs)
  },
  {
    path: 'ordens/:id',
    loadComponent: () => import('./features/ordens/detalhe-os/detalhe-os').then(m => m.DetalheOs)
  },
];
