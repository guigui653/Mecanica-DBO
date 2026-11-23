import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MatDialog } from '@angular/material/dialog';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { VeiculoService } from '../../../core/services/veiculo';
import { OrdemServicoService } from '../../../core/services/ordem-servico';
import { FormVeiculo } from '../form-veiculo/form-veiculo';
import { Veiculo } from '../../../shared/models/veiculo.model';
import { OrdemServico, ROTULOS_STATUS, StatusOs, TAG_STATUS } from '../../../shared/models/ordem-servico.model';
import { PlacaPipe } from '../../../shared/pipes/placa.pipe';
import { ModalNovoCliente } from '../../clientes/modal-novo-cliente/modal-novo-cliente';
import { mensagemDoErro } from '../../../core/utils/mensagem-erro';

interface HistoricoItem {
  id?: number;
  data: string;
  servico: string;
  km: string;
  valor: string;
}

interface VeiculoExt extends Veiculo {
  dono?: string;
  fone?: string;
  km?: string;
  /** Status da OS mais recente do carro. */
  status?: StatusOs;
  historico: HistoricoItem[];
}

@Component({
  selector: 'app-busca-veiculo',
  standalone: true,
  imports: [CommonModule, FormsModule, PlacaPipe, ModalNovoCliente],
  templateUrl: './busca-veiculo.html',
  styleUrl: './busca-veiculo.css'
})
export class BuscaVeiculo implements OnInit {
  private service = inject(VeiculoService);
  private osService = inject(OrdemServicoService);
  private router = inject(Router);
  private dialog = inject(MatDialog);
  private destroyRef = inject(DestroyRef);

  placa               = signal('');
  veiculo             = signal<VeiculoExt | null>(null);
  carregando          = signal(false);
  carregandoHistorico = signal(false);
  buscaRealizada      = signal(false);
  recentes            = signal<string[]>([]);
  erro                = signal('');
  historicoErro       = signal('');

  // ── Lista de todos os veículos ──
  todos          = signal<Veiculo[]>([]);
  carregandoLista = signal(false);
  erroLista      = signal('');
  termo          = signal('');
  cadastroAberto = signal(false);
  lista = computed(() => {
    const q = this.termo().trim().toLowerCase();
    const qPlaca = q.toUpperCase().replace(/[^A-Z0-9]/g, '');
    if (!q) return this.todos();
    return this.todos().filter(v =>
      (qPlaca && v.placa.includes(qPlaca))
      || `${v.marca} ${v.modelo} ${v.cor ?? ''} ${v.cliente?.nome ?? ''}`.toLowerCase().includes(q));
  });

  ngOnInit(): void {
    this.carregarLista();
  }

  carregarLista(): void {
    this.carregandoLista.set(true);
    this.erroLista.set('');
    this.service.listar().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: l => {
        this.todos.set([...l].sort((a, b) => a.placa.localeCompare(b.placa)));
        this.carregandoLista.set(false);
      },
      error: e => {
        this.carregandoLista.set(false);
        this.erroLista.set(mensagemDoErro(e, 'Não foi possível carregar os veículos.'));
      }
    });
  }

  /** Abre a ficha (dados, dono e histórico) do veículo clicado na lista. */
  abrirFicha(v: Veiculo): void {
    this.selecionarPlaca(v.placa);
  }

  voltarParaLista(): void {
    this.veiculo.set(null);
    this.buscaRealizada.set(false);
    this.erro.set('');
    this.placa.set('');
  }

  editar(v: Veiculo, evento?: Event): void {
    evento?.stopPropagation();
    this.dialog.open(FormVeiculo, {
      width: '640px', maxWidth: '95vw', disableClose: true,
      data: { veiculo: v, clienteId: v.cliente?.id }
    }).afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((salvo: Veiculo | false) => {
      if (!salvo) return;
      this.carregarLista();
      if (this.veiculo()?.id === v.id) this.selecionarPlaca(salvo.placa);
    });
  }

  inativar(v: Veiculo, evento?: Event): void {
    evento?.stopPropagation();
    if (!v.id) return;
    if (!confirm(`Tirar ${v.marca} ${v.modelo} (${v.placa}) da lista de veículos?\n\n`
        + 'O histórico de ordens de serviço dele continua guardado.')) return;
    this.service.inativar(v.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        if (this.veiculo()?.id === v.id) this.voltarParaLista();
        this.carregarLista();
      },
      error: e => alert(mensagemDoErro(e, 'Não foi possível inativar o veículo.'))
    });
  }

  aoFecharCadastro(salvou: boolean): void {
    this.cadastroAberto.set(false);
    if (salvou) this.carregarLista();
  }

  buscar(): void {
    if (this.carregando() || this.carregandoHistorico()) return;
    const placa = this.placa().toUpperCase().replace(/[^A-Z0-9]/g, '');
    this.erro.set('');
    if (!/^[A-Z]{3}\d[A-Z0-9]\d{2}$/.test(placa)) {
      this.erro.set('Informe uma placa válida, como ABC1234 ou ABC1D23.');
      this.veiculo.set(null);
      this.buscaRealizada.set(false);
      return;
    }
    this.placa.set(placa);
    this.carregando.set(true);
    this.buscaRealizada.set(false);
    this.veiculo.set(null);
    this.historicoErro.set('');

    this.service.buscarPorPlaca(placa).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: v => {
        this.veiculo.set({ ...v, dono: v.cliente?.nome, fone: v.cliente?.telefone1, historico: [] });
        this.recentes.update(r => [placa, ...r.filter(p => p !== placa)].slice(0, 5));
        this.carregando.set(false);
        this.buscaRealizada.set(true);
        if (v.id) this.carregarHistorico(v.id);
      },
      error: (e: HttpErrorResponse) => {
        this.carregando.set(false);
        this.buscaRealizada.set(e.status === 404);
        if (e.status !== 404) this.erro.set('Não foi possível consultar o veículo. Tente novamente.');
      }
    });
  }

  private carregarHistorico(veiculoId: number): void {
    this.carregandoHistorico.set(true);
    this.osService.listarPorVeiculo(veiculoId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: ordens => {
        const ordenadas = [...ordens].sort((a, b) =>
          (b.dataEntrada ?? '').localeCompare(a.dataEntrada ?? '') || (b.id ?? 0) - (a.id ?? 0));
        const comKm = ordenadas.find(o => o.kmEntrada != null);
        this.veiculo.update(v => v && {
          ...v,
          status: ordenadas[0]?.status,
          km: comKm ? this.formatarKm(comKm) : undefined,
          historico: ordenadas.map(o => ({
            id: o.id,
            data: o.dataEntrada ? new Date(o.dataEntrada + 'T00:00:00').toLocaleDateString('pt-BR') : '—',
            servico: o.diagnostico || o.reclamacoes || `OS #${o.id} · ${ROTULOS_STATUS[o.status]}`,
            km: this.formatarKm(o),
            valor: (o.totalGeral ?? 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
          }))
        });
        this.carregandoHistorico.set(false);
      },
      error: () => {
        this.carregandoHistorico.set(false);
        this.historicoErro.set('Não foi possível carregar o histórico deste veículo.');
      }
    });
  }

  private formatarKm(os: OrdemServico): string {
    return os.kmEntrada == null ? 'Não informada' : os.kmEntrada.toLocaleString('pt-BR') + ' km';
  }

  selecionarPlaca(placa: string): void {
    if (this.carregando() || this.carregandoHistorico()) return;
    this.placa.set(placa);
    this.buscar();
  }

  rotuloStatus(status?: StatusOs): string {
    return status ? ROTULOS_STATUS[status] : 'Sem situação informada';
  }

  classeTag(status?: StatusOs): string {
    return status ? TAG_STATUS[status] : 'tag-neutral';
  }

  abrirNovaOs(): void {
    const v = this.veiculo();
    this.router.navigate(['/ordens/nova'], { queryParams: v?.id ? { veiculoId: v.id } : {} });
  }

  irParaCliente(): void {
    this.router.navigate(['/clientes']);
  }

  imprimirHistorico(): void {
    window.print();
  }

  /** Veículo novo: o mesmo cadastro completo do painel (já começa em "Já é cliente"). */
  abrirCadastroVeiculo(): void {
    this.cadastroAberto.set(true);
  }
}
