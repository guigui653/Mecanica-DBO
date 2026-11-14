import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { catchError, forkJoin, of } from 'rxjs';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ClienteService } from '../../../core/services/cliente';
import { OrdemServicoService } from '../../../core/services/ordem-servico';
import { mensagemDoErro } from '../../../core/utils/mensagem-erro';
import { Cliente } from '../../../shared/models/cliente.model';
import { FormCliente } from '../form-cliente/form-cliente';
import { VeiculosDoCliente } from '../veiculos-do-cliente/veiculos-do-cliente';

export interface ClienteExt extends Cliente {
  totalVeiculos?: number;
  ultimaVisita?: string;
  tags?: string[];
}

const FILTROS_COMPLETOS = ['Todos', 'Com OS aberta', 'Inativos há 1 ano'];

@Component({
  selector: 'app-lista-clientes',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, VeiculosDoCliente],
  templateUrl: './lista-clientes.html',
  styleUrl: './lista-clientes.css'
})
export class ListaClientes implements OnInit {
  private service = inject(ClienteService);
  private dialog  = inject(MatDialog);
  private router  = inject(Router);
  private osService = inject(OrdemServicoService);

  clientes           = signal<ClienteExt[]>([]);
  clienteSelecionado = signal<ClienteExt | null>(null);

  termoBusca  = signal('');
  carregando  = signal(false);
  erro        = signal('');
  aviso       = signal('');
  excluindoId = signal<number | null>(null);

  filtros     = signal<string[]>(FILTROS_COMPLETOS);
  filtroAtivo = signal('Todos');

  /** A busca filtra enquanto se digita. */
  clientesFiltrados = computed(() => {
    const q = this.termoBusca().trim().toLowerCase();
    const filtro = this.filtroAtivo();
    return this.clientes().filter(c => {
      const matchQuery = !q || [c.nome, c.cpf, c.telefone1, c.email].join(' ').toLowerCase().includes(q);
      const matchFilter = filtro === 'Todos' || (c.tags || []).includes(filtro);
      return matchQuery && matchFilter;
    });
  });

  ngOnInit() {
    this.carregar();
  }

  carregar() {
    this.carregando.set(true);
    this.erro.set('');
    this.aviso.set('');
    forkJoin({
      clientes: this.service.listar(),
      ordens: this.osService.buscar().pipe(catchError(() => of(null)))
    }).subscribe({
      next: ({ clientes, ordens }) => {
        if (ordens === null) {
          this.aviso.set('Os clientes foram carregados, mas as situações e últimas visitas não estão disponíveis.');
          this.filtroAtivo.set('Todos');
          this.filtros.set(['Todos']);
        } else {
          this.filtros.set(FILTROS_COMPLETOS);
        }
        const limite = new Date();
        limite.setFullYear(limite.getFullYear() - 1);
        const lista = clientes.map(c => {
          const historico = (ordens ?? []).filter(o => o.veiculo?.cliente?.id === c.id);
          const ultima = historico.map(o => o.dataEntrada ?? '').filter(Boolean).sort().at(-1);
          const tags: string[] = [];
          if (historico.some(o => o.status === 'ABERTA' || o.status === 'EM_ANDAMENTO')) tags.push('Com OS aberta');
          if (ultima && tags.length === 0 && new Date(ultima + 'T00:00:00') < limite) tags.push('Inativos há 1 ano');
          return { ...c, ultimaVisita: ultima ? new Date(ultima + 'T00:00:00').toLocaleDateString('pt-BR') : undefined, tags };
        });
        this.clientes.set(lista);
        const selecionado = this.clienteSelecionado();
        if (selecionado) this.clienteSelecionado.set(lista.find(c => c.id === selecionado.id) ?? null);
        this.carregando.set(false);
      },
      error: () => {
        this.erro.set('Não foi possível carregar os clientes. Tente novamente.');
        this.carregando.set(false);
      }
    });
  }

  selecionarFiltro(f: string) {
    this.filtroAtivo.set(f);
  }

  selecionarCliente(c: ClienteExt) {
    this.clienteSelecionado.set(c);
    if (!c.id) return;
    const id = c.id;
    this.service.listarVeiculos(id).subscribe({
      next: veiculos => this.atualizarCliente(id, { totalVeiculos: veiculos.length }),
      error: () => this.atualizarCliente(id, { totalVeiculos: undefined })
    });
  }

  /** Troca um cliente da lista por uma cópia alterada (signals só percebem objeto novo). */
  private atualizarCliente(id: number, mudanca: Partial<ClienteExt>) {
    this.clientes.update(lista => lista.map(c => (c.id === id ? { ...c, ...mudanca } : c)));
    const selecionado = this.clienteSelecionado();
    if (selecionado?.id === id) this.clienteSelecionado.set({ ...selecionado, ...mudanca });
  }

  voltarLista() {
    this.clienteSelecionado.set(null);
  }

  obterSubtitulo(c: ClienteExt): string {
    if (c.tags?.includes('Com OS aberta')) return 'OS em aberto';
    return c.criadoEm ? `Cliente desde ${new Date(c.criadoEm).getFullYear()}` : 'Cliente cadastrado';
  }

  /** Na ficha, a Nova OS já abre com os carros deste cliente. */
  abrirNovaOs() {
    const id = this.clienteSelecionado()?.id;
    this.router.navigate(['/ordens/nova'], id ? { queryParams: { clienteId: id } } : {});
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

  /**
   * Apagar de verdade só funciona para cliente SEM nenhuma OS (cadastro
   * duplicado ou de teste). Se ele tiver histórico, o backend recusa (409) e
   * a tela oferece inativar: some da lista, mas as OS continuam guardadas.
   */
  excluirCliente(event: Event, c: ClienteExt) {
    event.stopPropagation();
    if (!c.id) return;
    const id = c.id;

    const confirma = window.confirm(
      `Apagar o cliente ${c.nome}${c.cpf ? ' (' + c.cpf + ')' : ''}?\n\n`
      + 'Use para cadastro duplicado ou de teste. Esta ação não pode ser desfeita.'
    );
    if (!confirma) return;

    this.excluindoId.set(id);
    this.service.excluir(id).subscribe({
      next: () => this.tirarDaLista(id),
      error: (e: HttpErrorResponse) => {
        this.excluindoId.set(null);
        if (e.status === 409 && window.confirm(`${mensagemDoErro(e)}\n\nInativar ${c.nome} agora?`)) {
          this.inativar(id);
        } else if (e.status !== 409) {
          window.alert(mensagemDoErro(e, 'Não foi possível apagar o cliente.'));
        }
      }
    });
  }

  private inativar(id: number) {
    this.excluindoId.set(id);
    this.service.inativar(id).subscribe({
      next: () => this.tirarDaLista(id),
      error: e => {
        this.excluindoId.set(null);
        window.alert(mensagemDoErro(e, 'Não foi possível inativar o cliente.'));
      }
    });
  }

  private tirarDaLista(id: number) {
    this.clientes.update(lista => lista.filter(x => x.id !== id));
    if (this.clienteSelecionado()?.id === id) this.clienteSelecionado.set(null);
    this.excluindoId.set(null);
  }
}
