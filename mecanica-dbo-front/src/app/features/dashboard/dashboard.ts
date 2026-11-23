import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DashboardResumo, DashboardService } from '../../core/services/dashboard';
import { OrdemServico, ROTULOS_STATUS, StatusOs, TAG_STATUS, classeBadge } from '../../shared/models/ordem-servico.model';

const NOMES_MES = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];
const ORDEM_STATUS: StatusOs[] = ['ABERTA', 'EM_ANDAMENTO', 'CONCLUIDA', 'ENTREGUE'];
const COR_STATUS: Record<StatusOs, string> = {
  ABERTA: 'var(--status-aberta)',
  EM_ANDAMENTO: 'var(--status-em-andamento)',
  CONCLUIDA: 'var(--status-concluida)',
  ENTREGUE: 'var(--status-entregue)'
};
import { Cliente } from '../../shared/models/cliente.model';
import { ModalNovoCliente } from '../clientes/modal-novo-cliente/modal-novo-cliente';

/**
 * Painel do dia. Os números vêm prontos do backend (GET /dashboard/resumo),
 * contados pelo banco — o navegador não baixa mais todas as OS e clientes.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, CurrencyPipe, DatePipe, ModalNovoCliente],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {
  private service = inject(DashboardService);

  readonly today = new Date();

  carregando = signal(true);
  erro       = signal(false);
  resumo     = signal<DashboardResumo | null>(null);
  modalNovoClienteAberto = signal(false);

  /* ── números e listas do painel ──────────────────────────── */

  /** Carros fisicamente no pátio: ABERTA ou EM_ANDAMENTO. */
  totalPatio     = computed(() => this.resumo()?.noPatio ?? 0);
  /** OS abertas, ainda aguardando atendimento. */
  osAbertas      = computed(() => this.resumo()?.osAbertas ?? 0);
  /** No pátio, do mais antigo ao mais recente (encalhado no topo). */
  patioOrdenado  = computed<OrdemServico[]>(() => this.resumo()?.patio ?? []);
  /** Prontas — precisam de ligação para o cliente. */
  prontas        = computed<OrdemServico[]>(() => this.resumo()?.prontas ?? []);
  /** Faturamento: total das OS entregues no mês atual. */
  faturamentoMes = computed(() => this.resumo()?.faturamentoMes ?? 0);
  clientesDoMes  = computed<Cliente[]>(() => this.resumo()?.clientesDoMes ?? []);
  clientesNovos  = computed(() => this.clientesDoMes().length);

  /** Barras do gráfico: altura em % do maior mês (mínimo visível quando há valor). */
  barrasFaturamento = computed(() => {
    const meses = this.resumo()?.faturamentoMensal ?? [];
    const maior = Math.max(1, ...meses.map(m => Number(m.total) || 0));
    return meses.map(m => {
      const total = Number(m.total) || 0;
      return {
        rotulo: NOMES_MES[m.mes - 1] ?? String(m.mes),
        total,
        altura: total > 0 ? Math.max(4, (total / maior) * 100) : 0,
        atual: m.mes === this.today.getMonth() + 1 && m.ano === this.today.getFullYear()
      };
    });
  });

  /** Linhas do "Ordens por status": quantidade e largura da barra. */
  distribuicaoStatus = computed(() => {
    const contagem = this.resumo()?.osPorStatus;
    const total = ORDEM_STATUS.reduce((s, st) => s + (contagem?.[st] ?? 0), 0);
    return ORDEM_STATUS.map(st => ({
      status: st,
      rotulo: ROTULOS_STATUS[st],
      qtd: contagem?.[st] ?? 0,
      largura: total ? ((contagem?.[st] ?? 0) / total) * 100 : 0,
      cor: COR_STATUS[st]
    }));
  });

  ultimasOs     = computed<OrdemServico[]>(() => this.resumo()?.ultimasOs ?? []);
  totalVeiculos = computed(() => this.resumo()?.totalVeiculos ?? 0);
  totalClientes = computed(() => this.resumo()?.totalClientes ?? 0);

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(false);
    this.service.resumo().subscribe({
      next: r => {
        this.resumo.set(r);
        this.carregando.set(false);
      },
      error: () => {
        this.erro.set(true);
        this.carregando.set(false);
      }
    });
  }

  abrirModalNovoCliente(): void {
    this.modalNovoClienteAberto.set(true);
  }

  aoFecharModalNovoCliente(recarregar: boolean): void {
    this.modalNovoClienteAberto.set(false);
    if (recarregar) {
      this.carregar();
    }
  }

  /* ── helpers ─────────────────────────────────────────────── */

  diasParado(os: OrdemServico): number {
    if (!os.dataEntrada) return 0;
    const entrada = new Date(os.dataEntrada + 'T00:00:00');
    const hoje    = new Date();
    hoje.setHours(0, 0, 0, 0);
    entrada.setHours(0, 0, 0, 0);
    return Math.max(0, Math.round((hoje.getTime() - entrada.getTime()) / 86_400_000));
  }

  labelDias(dias: number): string {
    if (dias === 0) return 'entrou hoje';
    if (dias === 1) return '1 dia';
    return `${dias} dias`;
  }

  badgeClass(os: OrdemServico): string {
    return classeBadge(os.status);
  }

  tagStatus(os: OrdemServico): string {
    return TAG_STATUS[os.status] ?? '';
  }

  /** Valor curto para cima da barra: R$ 4.120 / R$ 12,5 mil. */
  valorCurto(v: number): string {
    if (v >= 10_000) return 'R$ ' + (v / 1000).toLocaleString('pt-BR', { maximumFractionDigits: 1 }) + ' mil';
    return v.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 });
  }

  labelStatus(os: OrdemServico): string {
    return ROTULOS_STATUS[os.status] ?? os.status;
  }

  mesDoCliente(c: Cliente): string {
    if (!c.criadoEm) return '';
    return new Date(c.criadoEm).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' });
  }
}
