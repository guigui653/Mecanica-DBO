import { Component, DestroyRef, inject, signal } from '@angular/core';
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
  imports: [CommonModule, FormsModule, PlacaPipe],
  templateUrl: './busca-veiculo.html',
  styleUrl: './busca-veiculo.css'
})
export class BuscaVeiculo {
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

  abrirCadastroVeiculo(): void {
    this.dialog.open(FormVeiculo, { width: '640px', maxWidth: '95vw', data: null })
      .afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((v: Veiculo | undefined) => {
        if (v?.placa) {
          this.placa.set(v.placa);
          this.buscar();
        }
      });
  }
}
