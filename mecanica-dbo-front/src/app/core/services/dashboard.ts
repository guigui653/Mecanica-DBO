import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Cliente } from '../../shared/models/cliente.model';
import { OrdemServico } from '../../shared/models/ordem-servico.model';

/** Espelha o DashboardResumoDTO do backend: já vem contado pelo banco. */
export interface DashboardResumo {
  /** Carros na oficina (Aberta + Em andamento). */
  noPatio: number;
  /** OS Aberta, aguardando atendimento. */
  osAbertas: number;
  /** Soma das OS entregues no mês corrente. */
  faturamentoMes: number;
  /** Clientes ativos. */
  totalClientes: number;
  /** OS no pátio, da mais antiga para a mais nova. */
  patio: OrdemServico[];
  /** OS prontas para entrega (ligar para o cliente). */
  prontas: OrdemServico[];
  /** Clientes cadastrados no mês corrente, mais recentes primeiro. */
  clientesDoMes: Cliente[];
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private http = inject(HttpClient);

  /** GET /dashboard/resumo */
  resumo(): Observable<DashboardResumo> {
    return this.http.get<DashboardResumo>('/dashboard/resumo');
  }
}
