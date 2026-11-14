import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { OrdemServicoService } from '../../../core/services/ordem-servico';
import { OrdemServico, StatusOs } from '../../../shared/models/ordem-servico.model';
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
  status: string;
  statusLabel: string;
  tagClass: string;
}

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
  private cdr = inject(ChangeDetectorRef);

  todasOrdens: OsViewItem[] = [];
  ordensFiltradas: OsViewItem[] = [];

  filtros = ['Todas', 'Em serviço', 'Aguardando', 'Prontas', 'Entregues'];

  /** Cada filtro corresponde a um status da OS ("Todas" não filtra). */
  private readonly statusDoFiltro: Record<string, StatusOs> = {
    'Em serviço': 'EM_ANDAMENTO',
    'Aguardando': 'ABERTA',
    'Prontas':    'CONCLUIDA',
    'Entregues':  'ENTREGUE'
  };
  filtroAtivo = 'Todas';
  termoBusca = '';
  carregando = false;
  erro = '';
  excluindoId: number | null = null;

  ngOnInit() {
    this.carregar();
  }

  carregar() {
    this.carregando = true;
    this.erro = '';

    this.service.listar().subscribe({
      next: (bancoOrdens) => {
        const reais: OsViewItem[] = (bancoOrdens || []).map(o => {
          const num = 'OS ' + (o.id ?? '—');
          const valor = (o.totalGeral ?? 0).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
          return {
            id: o.id,
            num,
            placa: formatarPlaca(o.veiculo?.placa) || '—',
            carro: [o.veiculo?.marca, o.veiculo?.modelo].filter(Boolean).join(' ') || '—',
            cliente: o.veiculo?.cliente?.nome ?? '—',
            servico: o.diagnostico || o.reclamacoes || 'Não informado',
            valor,
            valorNum: o.totalGeral ?? 0,
            status: o.status,
            statusLabel: this.obterLabelStatus(o.status),
            tagClass: this.obterTagClass(o.status)
          };
        });

        // A lista representa somente ordens retornadas pela API.
        this.todasOrdens = reais;
        this.aplicarFiltros();
        this.carregando = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.erro = 'Não foi possível carregar as ordens. Tente novamente.';
        this.todasOrdens = [];
        this.aplicarFiltros();
        this.carregando = false;
        this.cdr.markForCheck();
      }
    });
  }

  selecionarFiltro(f: string) {
    this.filtroAtivo = f;
    this.aplicarFiltros();
  }

  aplicarFiltros() {
    const q = this.termoBusca.trim().toLowerCase();
    this.ordensFiltradas = this.todasOrdens.filter(o => {
      // A placa entra com e sem hífen, para "jeh3030" e "jeh-3030" acharem o mesmo carro.
      const matchQ = !q || [o.num, o.placa, o.placa.replace('-', ''), o.carro, o.cliente, o.servico].join(' ').toLowerCase().includes(q);

      const status = this.statusDoFiltro[this.filtroAtivo];
      const matchF = !status || o.status === status;

      return matchQ && matchF;
    });
  }

  get resumoTexto(): string {
    const total = this.ordensFiltradas.reduce((acc, o) => acc + o.valorNum, 0);
    const totalFmt = total.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    return `${this.ordensFiltradas.length} ordens listadas · total R$ ${totalFmt}`;
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

    const confirma = window.confirm(
      `Excluir ${o.num} (${o.placa} — ${o.cliente})?\n\nEsta ação não pode ser desfeita.`
    );
    if (!confirma) return;

    this.excluindoId = o.id;
    this.service.excluir(o.id).subscribe({
      next: () => {
        this.todasOrdens = this.todasOrdens.filter(x => x.id !== o.id);
        this.aplicarFiltros();
        this.excluindoId = null;
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.excluindoId = null;
        const msg = e?.error?.mensagem ?? 'Não foi possível excluir a OS.';
        window.alert(msg);
        this.cdr.markForCheck();
      }
    });
  }

  private obterLabelStatus(status: StatusOs): string {
    switch (status) {
      case 'ABERTA': return 'Aberta';
      case 'EM_ANDAMENTO': return 'Em serviço';
      case 'CONCLUIDA': return 'Pronto p/ entrega';
      case 'ENTREGUE': return 'Entregue';
      default: return status;
    }
  }

  private obterTagClass(status: StatusOs): string {
    switch (status) {
      case 'ABERTA': return 'tag-outline';
      case 'EM_ANDAMENTO': return 'tag-accent';
      case 'CONCLUIDA': return 'tag-accent-2';
      case 'ENTREGUE': return 'tag-neutral';
      default: return 'tag-neutral';
    }
  }
}
