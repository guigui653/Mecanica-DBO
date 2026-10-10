import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, catchError, of } from 'rxjs';
import { OrdemServicoService } from '../../core/services/ordem-servico';
import { ClienteService } from '../../core/services/cliente';
import { OrdemServico } from '../../shared/models/ordem-servico.model';
import { Cliente } from '../../shared/models/cliente.model';
import { ModalNovoCliente } from '../clientes/modal-novo-cliente/modal-novo-cliente';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, CurrencyPipe, DatePipe, ModalNovoCliente],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {
  private osService      = inject(OrdemServicoService);
  private clienteService = inject(ClienteService);

  readonly today = new Date();

  /* ── estado bruto ───────────────────────────────────────── */
  carregando = signal(true);
  erro       = signal(false);
  todasOrdens = signal<OrdemServico[]>([]);
  clientes    = signal<Cliente[]>([]);
  modalNovoClienteAberto = signal(false);

  /* ── métricas computadas ─────────────────────────────────── */

  /** Carros fisicamente no pátio: ABERTA ou EM_ANDAMENTO */
  noPatio = computed(() =>
    this.todasOrdens().filter(o =>
      o.status === 'ABERTA' || o.status === 'EM_ANDAMENTO'
    )
  );

  totalPatio = computed(() => this.noPatio().length);

  /** No pátio do mais antigo ao mais recente (encalhado no topo) */
  patioOrdenado = computed(() =>
    [...this.noPatio()].sort((a, b) =>
      new Date(a.dataEntrada ?? '').getTime() -
      new Date(b.dataEntrada ?? '').getTime()
    )
  );

  /** Concluídas — precisam de ligação para o cliente */
  prontas = computed(() =>
    this.todasOrdens().filter(o => o.status === 'CONCLUIDA')
  );

  /** Faturamento: totalGeral das ENTREGUE no mês atual */
  faturamentoMes = computed(() => {
    const agora = new Date();
    return this.todasOrdens()
      .filter(o => {
        if (o.status !== 'ENTREGUE') return false;
        const ref = o.dataSaidaReal ?? o.dataEntrada;
        if (!ref) return false;
        const d = new Date(ref + 'T00:00:00');
        return d.getFullYear() === agora.getFullYear() &&
               d.getMonth()    === agora.getMonth();
      })
      .reduce((acc, o) => acc + (o.totalGeral ?? 0), 0);
  });

  /** Clientes cadastrados neste mês */
  clientesDoMes = computed(() => {
    const agora = new Date();
    return this.clientes().filter(c => {
      if (!c.criadoEm) return false;
      const d = new Date(c.criadoEm);
      return d.getFullYear() === agora.getFullYear() &&
             d.getMonth()    === agora.getMonth();
    });
  });

  clientesNovos = computed(() => this.clientesDoMes().length);

  /* ── ciclo de vida ───────────────────────────────────────── */

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(false);

    forkJoin({
      ordens:   this.osService.buscar().pipe(catchError(() => of(null))),
      clientes: this.clienteService.listar().pipe(catchError(() => of(null)))
    }).subscribe(({ ordens, clientes }) => {
      if (ordens === null) {
        this.erro.set(true);
        this.carregando.set(false);
        return;
      }
      this.todasOrdens.set(ordens);
      this.clientes.set(clientes ?? []);
      this.carregando.set(false);
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
    const map: Record<string, string> = {
      ABERTA:       'badge badge-aberta',
      EM_ANDAMENTO: 'badge badge-em-andamento',
      CONCLUIDA:    'badge badge-concluida',
      ENTREGUE:     'badge badge-entregue',
    };
    return map[os.status] ?? 'badge';
  }

  labelStatus(os: OrdemServico): string {
    const map: Record<string, string> = {
      ABERTA:       'Aberta',
      EM_ANDAMENTO: 'Em andamento',
      CONCLUIDA:    'Pronto p/ entrega',
      ENTREGUE:     'Entregue',
    };
    return map[os.status] ?? os.status;
  }

  mesDoCliente(c: Cliente): string {
    if (!c.criadoEm) return '';
    return new Date(c.criadoEm).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' });
  }
}
