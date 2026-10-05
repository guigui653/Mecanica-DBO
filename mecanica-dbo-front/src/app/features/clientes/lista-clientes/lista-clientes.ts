import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { catchError, forkJoin, of } from 'rxjs';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ClienteService } from '../../../core/services/cliente';
import { OrdemServicoService } from '../../../core/services/ordem-servico';
import { Cliente } from '../../../shared/models/cliente.model';
import { FormCliente } from '../form-cliente/form-cliente';

export interface ClienteExt extends Cliente {
  totalVeiculos?: number;
  ultimaVisita?: string;
  subtitulo?: string;
  tags?: string[];
}

@Component({
  selector: 'app-lista-clientes',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule],
  templateUrl: './lista-clientes.html',
  styleUrl: './lista-clientes.css'
})
export class ListaClientes implements OnInit {
  private service = inject(ClienteService);
  private dialog  = inject(MatDialog);
  private router  = inject(Router);
  private osService = inject(OrdemServicoService);
  private cdr = inject(ChangeDetectorRef);

  clientes: ClienteExt[] = [];
  clientesFiltrados: ClienteExt[] = [];
  clienteSelecionado: ClienteExt | null = null;

  termoBusca = '';
  carregando = false;
  erro = '';
  aviso = '';

  filtros = ['Todos', 'Com OS aberta', 'Inativos há 1 ano'];
  filtroAtivo = 'Todos';

  ngOnInit() {
    this.carregar();
  }

  carregar() {
    this.carregando = true;
    this.erro = '';
    this.aviso = '';
    forkJoin({
      clientes: this.service.listar(),
      ordens: this.osService.buscar().pipe(catchError(() => of(null)))
    }).subscribe({
      next: ({ clientes, ordens }) => {
        if (ordens === null) {
          this.aviso = 'Os clientes foram carregados, mas as situações e últimas visitas não estão disponíveis.';
          this.filtroAtivo = 'Todos';
          this.filtros = ['Todos'];
        } else this.filtros = ['Todos', 'Com OS aberta', 'Inativos há 1 ano'];
        const limite = new Date();
        limite.setFullYear(limite.getFullYear() - 1);
        this.clientes = clientes.map(c => {
          const historico = (ordens ?? []).filter(o => o.veiculo?.cliente?.id === c.id);
          const ultima = historico.map(o => o.dataEntrada ?? '').filter(Boolean).sort().at(-1);
          const tags: string[] = [];
          if (historico.some(o => o.status === 'ABERTA' || o.status === 'EM_ANDAMENTO')) tags.push('Com OS aberta');
          if (ultima && tags.length === 0 && new Date(ultima + 'T00:00:00') < limite) tags.push('Inativos há 1 ano');
          return { ...c, ultimaVisita: ultima ? new Date(ultima + 'T00:00:00').toLocaleDateString('pt-BR') : undefined, tags };
        });
        if (this.clienteSelecionado) this.clienteSelecionado = this.clientes.find(c => c.id === this.clienteSelecionado?.id) ?? null;
        this.aplicarFiltros();
        this.carregando = false;
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.erro = 'Não foi possível carregar os clientes. Tente novamente.';
        this.carregando = false;
        this.cdr.markForCheck();
      }
    });
  }

  aplicarBusca() {
    this.aplicarFiltros();
  }

  selecionarFiltro(f: string) {
    this.filtroAtivo = f;
    this.aplicarFiltros();
  }

  private aplicarFiltros() {
    const q = this.termoBusca.trim().toLowerCase();
    this.clientesFiltrados = this.clientes.filter(c => {
      const matchQuery = !q || [c.nome, c.cpf, c.telefone1, c.email].join(' ').toLowerCase().includes(q);
      const matchFilter = this.filtroAtivo === 'Todos' || (c.tags || []).includes(this.filtroAtivo);
      return matchQuery && matchFilter;
    });
  }

  selecionarCliente(c: ClienteExt) {
    this.clienteSelecionado = c;
    if (c.id) this.service.listarVeiculos(c.id).subscribe({
      next: veiculos => { c.totalVeiculos = veiculos.length; this.cdr.markForCheck(); },
      error: () => { c.totalVeiculos = undefined; this.cdr.markForCheck(); }
    });
  }

  voltarLista() {
    this.clienteSelecionado = null;
  }

  obterSubtitulo(c: ClienteExt): string {
    if (c.subtitulo) return c.subtitulo;
    if (c.tags?.includes('Com OS aberta')) return 'OS em aberto';
    return c.criadoEm ? `Cliente desde ${new Date(c.criadoEm).getFullYear()}` : 'Cliente cadastrado';
  }

  abrirNovaOs() {
    this.router.navigate(['/ordens/nova']);
  }

  abrirForm(cliente?: ClienteExt) {
    const ref = this.dialog.open(FormCliente, { data: cliente ?? null });
    ref.afterClosed().subscribe(salvou => {
      if (salvou) this.carregar();
    });
  }

  enviarWhatsApp(cliente: ClienteExt) {
    const fone = (cliente.telefone1 || '').replace(/\D/g, '');
    if (fone) {
      window.open(`https://wa.me/55${fone}`, '_blank');
    }
  }
}
