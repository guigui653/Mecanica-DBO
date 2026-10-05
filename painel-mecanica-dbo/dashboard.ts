import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { catchError, forkJoin, of } from 'rxjs';

interface ClienteApi {
  id: number;
  nome: string;
  telefone1: string;
  criadoEm?: string;
}

interface VeiculoApi {
  id: number;
  placa: string;
  marca: string;
  modelo: string;
  cliente?: { id: number; nome: string };
}

interface OrdemApi {
  id: number;
  status: 'ABERTA' | 'EM_ANDAMENTO' | 'CONCLUIDA' | 'ENTREGUE';
  dataEntrada?: string;
  dataSaidaPrevista?: string;
  reclamacoes?: string;
  totalGeral?: number;
  veiculo?: VeiculoApi;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {

  private http = inject(HttpClient);
  private router = inject(Router);

  carregando = signal(true);
  erro       = signal<string | null>(null);

  ordens   = signal<OrdemApi[]>([]);
  clientes = signal<ClienteApi[]>([]);

  /** OS que ainda não foram entregues — o carro está fisicamente na oficina. */
  noPatio = computed(() =>
    this.ordens().filter(o => o.status === 'ABERTA' || o.status === 'EM_ANDAMENTO')
  );

  abertas = computed(() =>
    this.ordens().filter(o => o.status === 'ABERTA')
  );

  emAndamento = computed(() =>
    this.ordens().filter(o => o.status === 'EM_ANDAMENTO')
  );

  /** Prontas para entrega — o cliente já pode ser avisado. */
  concluidas = computed(() =>
    this.ordens().filter(o => o.status === 'CONCLUIDA')
  );

  /** Soma das OS entregues no mês corrente. */
  faturamentoMes = computed(() => {
    const hoje = new Date();
    return this.ordens()
      .filter(o => {
        if (o.status !== 'ENTREGUE' || !o.dataEntrada) return false;
        const d = new Date(o.dataEntrada + 'T00:00:00');
        return d.getMonth() === hoje.getMonth()
            && d.getFullYear() === hoje.getFullYear();
      })
      .reduce((soma, o) => soma + (o.totalGeral ?? 0), 0);
  });

  clientesNovosMes = computed(() => {
    const hoje = new Date();
    return this.clientes().filter(c => {
      if (!c.criadoEm) return false;
      const d = new Date(c.criadoEm);
      return d.getMonth() === hoje.getMonth()
          && d.getFullYear() === hoje.getFullYear();
    }).length;
  });

  /** Ordens do pátio ordenadas por entrada mais antiga — as que estão parando há mais tempo. */
  patioOrdenado = computed(() =>
    [...this.noPatio()].sort((a, b) =>
      (a.dataEntrada ?? '').localeCompare(b.dataEntrada ?? '')
    )
  );

  hoje = new Date();

  ngOnInit(): void {
    this.carregar();
  }

  private carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);

    forkJoin({
      ordens:   this.http.get<OrdemApi[]>('/ordens').pipe(catchError(() => of(null))),
      clientes: this.http.get<ClienteApi[]>('/clientes').pipe(catchError(() => of(null)))
    }).subscribe(({ ordens, clientes }) => {
      if (ordens === null) {
        this.erro.set('Não foi possível conectar à API. Verifique se o backend está rodando na porta 8080.');
      } else {
        this.ordens.set(ordens);
      }
      if (clientes) this.clientes.set(clientes);
      this.carregando.set(false);
    });
  }

  recarregar(): void {
    this.carregar();
  }

  abrirOs(id: number): void {
    this.router.navigate(['/ordens', id]);
  }

  /** Dias que o veículo está parado na oficina. */
  diasParado(dataEntrada?: string): number {
    if (!dataEntrada) return 0;
    const entrada = new Date(dataEntrada + 'T00:00:00');
    const diff = Date.now() - entrada.getTime();
    return Math.max(0, Math.floor(diff / 86400000));
  }

  classeBadge(status: string): string {
    return 'badge badge-' + status.toLowerCase().replace('_', '-');
  }

  rotuloStatus(status: string): string {
    const mapa: Record<string, string> = {
      ABERTA: 'Aberta',
      EM_ANDAMENTO: 'Em andamento',
      CONCLUIDA: 'Concluída',
      ENTREGUE: 'Entregue'
    };
    return mapa[status] ?? status;
  }
}
