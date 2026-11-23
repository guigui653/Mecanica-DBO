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
import { ModalNovoCliente } from '../modal-novo-cliente/modal-novo-cliente';
import { VeiculosDoCliente } from '../veiculos-do-cliente/veiculos-do-cliente';
import { CpfPipe, formatarCpf, rotuloDocumento, tipoDocumento } from '../../../shared/pipes/cpf.pipe';

export interface ClienteExt extends Cliente {
  totalVeiculos?: number;
  ultimaVisita?: string;
  tags?: string[];
}

interface AvisoExclusao {
  etapa: 'confirmar' | 'historico';
  /** Texto do backend explicando por que não pode apagar (etapa 'historico'). */
  mensagem?: string;
  erro?: string;
}

const FILTROS_COMPLETOS =['Todos', 'Com OS aberta', 'Inativos há 1 ano'];

@Component({
  selector: 'app-lista-clientes',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, VeiculosDoCliente, CpfPipe, ModalNovoCliente],
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
  /** Aviso de apagar/inativar mostrado na ficha (null = fechado). */
  avisoExclusao = signal<AvisoExclusao | null>(null);
  /** CPF inteiro na ficha só quando a pessoa clica em "Mostrar"; volta a esconder ao trocar de cliente. */
  mostrarCpf = signal(false);
  readonly rotuloDoc = rotuloDocumento;
  ehEmpresa(c: { cpf?: string }): boolean { return tipoDocumento(c.cpf) === 'CNPJ'; }

  filtros     = signal<string[]>(FILTROS_COMPLETOS);
  filtroAtivo = signal('Todos');

  /** A busca filtra enquanto se digita. */
  clientesFiltrados = computed(() => {
    const q = this.termoBusca().trim().toLowerCase();
    const filtro = this.filtroAtivo();
    return this.clientes().filter(c => {
      // CPF com e sem pontos, para "123.456.789-00" e "12345678900" acharem o mesmo cliente.
      const matchQuery = !q || [c.nome, c.cpf, formatarCpf(c.cpf), c.telefone1, c.email].join(' ').toLowerCase().includes(q);
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
    this.avisoExclusao.set(null);
    this.mostrarCpf.set(false);
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
    this.avisoExclusao.set(null);
    this.mostrarCpf.set(false);
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

  /** Cadastro de cliente novo: o mesmo cadastro completo do painel (cliente + carro). */
  cadastroAberto = signal(false);

  aoFecharCadastro(salvou: boolean) {
    this.cadastroAberto.set(false);
    if (salvou) this.carregar();
  }

  /** Editar cadastro de um cliente que já existe. */
  abrirForm(cliente?: ClienteExt) {
    if (!cliente) {
      this.cadastroAberto.set(true);
      return;
    }
    // disableClose: clique fora ou Esc não fecham e não perdem o que foi digitado.
    const ref = this.dialog.open(FormCliente, { data: cliente, disableClose: true });
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

  // ── Apagar / inativar ─────────────────────────────────────
  //
  // Apagar de verdade só funciona para cliente SEM nenhuma OS (cadastro
  // duplicado ou de teste). Nada acontece sem passar pelo aviso da ficha:
  //   'confirmar' → explica e pede confirmação para apagar;
  //   'historico' → o backend recusou (tem OS): explica e oferece inativar,
  //                 que tira da lista mas mantém o histórico.

  /** Lixeira da lista ou "Apagar cliente" da ficha: abre a ficha com o aviso. */
  pedirExclusao(event: Event, c: ClienteExt) {
    event.stopPropagation();
    if (this.clienteSelecionado()?.id !== c.id) this.selecionarCliente(c);
    this.avisoExclusao.set({ etapa: 'confirmar' });
  }

  cancelarExclusao() {
    this.avisoExclusao.set(null);
  }

  confirmarExclusao() {
    const id = this.clienteSelecionado()?.id;
    if (!id || this.excluindoId()) return;

    this.excluindoId.set(id);
    this.service.excluir(id).subscribe({
      next: () => this.tirarDaLista(id),
      error: (e: HttpErrorResponse) => {
        this.excluindoId.set(null);
        this.avisoExclusao.set(e.status === 409
          ? { etapa: 'historico', mensagem: mensagemDoErro(e) }
          : { etapa: 'confirmar', erro: mensagemDoErro(e, 'Não foi possível apagar o cliente.') });
      }
    });
  }

  confirmarInativacao() {
    const id = this.clienteSelecionado()?.id;
    if (!id || this.excluindoId()) return;

    this.excluindoId.set(id);
    this.service.inativar(id).subscribe({
      next: () => this.tirarDaLista(id),
      error: e => {
        this.excluindoId.set(null);
        const atual = this.avisoExclusao();
        this.avisoExclusao.set({ etapa: 'historico', mensagem: atual?.mensagem,
          erro: mensagemDoErro(e, 'Não foi possível inativar o cliente.') });
      }
    });
  }

  private tirarDaLista(id: number) {
    this.clientes.update(lista => lista.filter(x => x.id !== id));
    if (this.clienteSelecionado()?.id === id) this.clienteSelecionado.set(null);
    this.avisoExclusao.set(null);
    this.excluindoId.set(null);
  }
}
