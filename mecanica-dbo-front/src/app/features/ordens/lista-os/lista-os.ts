import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { OrdemServicoService } from '../../../core/services/ordem-servico';
import { mensagemDoErro } from '../../../core/utils/mensagem-erro';
import { ROTULOS_STATUS, StatusOs, TAG_STATUS } from '../../../shared/models/ordem-servico.model';
import { formatarPlaca } from '../../../shared/pipes/placa.pipe';

export interface OsViewItem {
  id?: number;
  num: string;
  placa: string;
  carro: string;
  cliente: string;
  servico: string;
  valor: string;
  valorNum: number;
  status: StatusOs;
  statusLabel: string;
  tagClass: string;
}

const formatarValor = (n: number) =>
  n.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

@Component({
  selector: 'app-lista-os',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './lista-os.html',
  styleUrl: './lista-os.css'
})
export class ListaOs implements OnInit {
  private service = inject(OrdemServicoService);
  private router  = inject(Router);

  readonly filtros = ['Todas', 'Em serviço', 'Aguardando', 'Prontas', 'Entregues'];

  /** Cada filtro corresponde a um status da OS ("Todas" não filtra). */
  private readonly statusDoFiltro: Record<string, StatusOs> = {
    'Em serviço': 'EM_ANDAMENTO',
    'Aguardando': 'ABERTA',
    'Prontas':    'CONCLUIDA',
    'Entregues':  'ENTREGUE'
  };

  todasOrdens = signal<OsViewItem[]>([]);
  filtroAtivo = signal('Todas');
  termoBusca  = signal('');
  carregando  = signal(false);
  erro        = signal('');
  excluindoId = signal<number | null>(null);

  ordensFiltradas = computed(() => {
    const q = this.termoBusca().trim().toLowerCase();
    const status = this.statusDoFiltro[this.filtroAtivo()];
    return this.todasOrdens().filter(o => {
      // A placa entra com e sem hífen, para "jeh3030" e "jeh-3030" acharem o mesmo carro.
      const matchQ = !q || [o.num, o.placa, o.placa.replace('-', ''), o.carro, o.cliente, o.servico]
        .join(' ').toLowerCase().includes(q);
      return matchQ && (!status || o.status === status);
    });
  });

  resumoTexto = computed(() => {
    const lista = this.ordensFiltradas();
    const total = lista.reduce((acc, o) => acc + o.valorNum, 0);
    return `${lista.length} ordens listadas · total R$ ${formatarValor(total)}`;
  });

  ngOnInit() {
    this.carregar();
  }

  carregar() {
    this.carregando.set(true);
    this.erro.set('');

    this.service.listar().subscribe({
      next: ordens => {
        this.todasOrdens.set((ordens || []).map(o => ({
          id: o.id,
          num: 'OS ' + (o.id ?? '—'),
          placa: formatarPlaca(o.veiculo?.placa) || '—',
          carro: [o.veiculo?.marca, o.veiculo?.modelo].filter(Boolean).join(' ') || '—',
          cliente: o.veiculo?.cliente?.nome ?? '—',
          servico: o.diagnostico || o.reclamacoes || 'Não informado',
          valor: formatarValor(o.totalGeral ?? 0),
          valorNum: o.totalGeral ?? 0,
          status: o.status,
          statusLabel: ROTULOS_STATUS[o.status] ?? o.status,
          tagClass: TAG_STATUS[o.status] ?? 'tag-neutral'
        })));
        this.carregando.set(false);
      },
      error: () => {
        this.erro.set('Não foi possível carregar as ordens. Tente novamente.');
        this.todasOrdens.set([]);
        this.carregando.set(false);
      }
    });
  }

  selecionarFiltro(f: string) {
    this.filtroAtivo.set(f);
  }

  abrirDetalhe(o: OsViewItem) {
    if (o.id) {
      this.router.navigate(['/ordens', o.id]);
    }
  }

  novaOs() {
    this.router.navigate(['/ordens/nova']);
  }

  excluirOs(event: Event, o: OsViewItem) {
    event.stopPropagation();
    if (!o.id) return;
    const id = o.id;

    const confirma = window.confirm(
      `Excluir ${o.num} (${o.placa} — ${o.cliente})?\n\nEsta ação não pode ser desfeita.`
    );
    if (!confirma) return;

    this.excluindoId.set(id);
    this.service.excluir(id).subscribe({
      next: () => {
        this.todasOrdens.update(lista => lista.filter(x => x.id !== id));
        this.excluindoId.set(null);
      },
      error: e => {
        this.excluindoId.set(null);
        window.alert(mensagemDoErro(e, 'Não foi possível excluir a OS.'));
      }
    });
  }
}
