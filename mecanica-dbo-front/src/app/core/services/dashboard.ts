import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Cliente } from '../../shared/models/cliente.model';
import { OrdemServico, StatusOs } from '../../shared/models/ordem-servico.model';

/** Um mês do gráfico de faturamento. */
export interface FaturamentoMes {
  ano: number;
  /** 1 a 12 */
  mes: number;
  total: number;
}

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
  /** Faturamento dos últimos 6 meses, do mais antigo ao atual. */
  faturamentoMensal: FaturamentoMes[];
  /** Quantas OS existem em cada situação. */
  osPorStatus: Record<StatusOs, number>;
  /** As 5 OS mais recentes. */
  ultimasOs: OrdemServico[];
  /** Veículos ativos. */
  totalVeiculos: number;
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private http = inject(HttpClient);

  /** GET /dashboard/resumo */
  resumo(): Observable<DashboardResumo> {
    return this.http.get<DashboardResumo>('/dashboard/resumo');
  }
}
