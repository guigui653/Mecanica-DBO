import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/login/login').then(m => m.Login)
  },
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./features/dashboard/dashboard').then(m => m.Dashboard)
  },
  {
    path: 'clientes',
    canActivate: [authGuard],
    loadComponent: () => import('./features/clientes/lista-clientes/lista-clientes').then(m => m.ListaClientes)
  },
  {
    path: 'veiculos',
    canActivate: [authGuard],
    loadComponent: () => import('./features/veiculos/busca-veiculo/busca-veiculo').then(m => m.BuscaVeiculo)
  },
  {
    path: 'ordens',
    canActivate: [authGuard],
    loadComponent: () => import('./features/ordens/lista-os/lista-os').then(m => m.ListaOs)
  },
  {
    path: 'ordens/nova',
    canActivate: [authGuard],
    loadComponent: () => import('./features/ordens/nova-os/nova-os').then(m => m.NovaOs)
  },
  {
    path: 'ordens/:id',
    canActivate: [authGuard],
    canDeactivate: [(component: { podeSair(): boolean }) => component.podeSair()],
    loadComponent: () => import('./features/ordens/detalhe-os/detalhe-os').then(m => m.DetalheOs)
  },
];
