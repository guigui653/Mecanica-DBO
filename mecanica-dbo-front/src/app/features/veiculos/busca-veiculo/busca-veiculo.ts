import { ChangeDetectorRef, Component, DestroyRef, inject } from '@angular/core';
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
import { OrdemServico, ROTULOS_STATUS } from '../../../shared/models/ordem-servico.model';

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
  status?: string;
  historico: HistoricoItem[];
}

@Component({
  selector: 'app-busca-veiculo',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './busca-veiculo.html',
  styleUrl: './busca-veiculo.css'
})
export class BuscaVeiculo {
  private service = inject(VeiculoService);
  private osService = inject(OrdemServicoService);
  private router = inject(Router);
  private dialog = inject(MatDialog);
  private cdr = inject(ChangeDetectorRef);
  private destroyRef = inject(DestroyRef);

  placa = '';
  veiculo: VeiculoExt | null = null;
  carregando = false;
  carregandoHistorico = false;
  buscaRealizada = false;
  recentes: string[] = [];
  erro = '';
  historicoErro = '';

  buscar(): void {
    if (this.carregando || this.carregandoHistorico) return;
    const placa = this.placa.trim().toUpperCase().replace(/[\s-]/g, '');
    this.erro = '';
    if (!/^[A-Z]{3}\d[A-Z0-9]\d{2}$/.test(placa)) {
      this.erro = 'Informe uma placa válida, como ABC1234 ou ABC1D23.';
      this.veiculo = null;
      this.buscaRealizada = false;
      return;
    }
    this.placa = placa;
    this.carregando = true;
    this.buscaRealizada = false;
    this.veiculo = null;
    this.historicoErro = '';

    this.service.buscarPorPlaca(placa).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: v => {
        this.veiculo = {
          ...v,
          dono: v.cliente?.nome,
          fone: v.cliente?.telefone1,
          historico: []
        };
        this.recentes = [placa, ...this.recentes.filter(p => p !== placa)].slice(0, 5);
        this.carregando = false;
        this.buscaRealizada = true;
        if (v.id) this.carregarHistorico(v.id);
        this.cdr.markForCheck();
      },
      error: (e: HttpErrorResponse) => {
        this.carregando = false;
        this.buscaRealizada = e.status === 404;
        if (e.status !== 404) this.erro = 'Não foi possível consultar o veículo. Tente novamente.';
        this.cdr.markForCheck();
      }
    });
  }

  private carregarHistorico(veiculoId: number): void {
    this.carregandoHistorico = true;
    this.osService.listarPorVeiculo(veiculoId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: ordens => {
        const ordenadas = [...ordens].sort((a, b) => (b.dataEntrada ?? '').localeCompare(a.dataEntrada ?? '') || (b.id ?? 0) - (a.id ?? 0));
        const ultima = ordenadas[0];
        const comKm = ordenadas.find(o => o.kmEntrada != null);
        if (this.veiculo) {
          this.veiculo.status = ultima ? ROTULOS_STATUS[ultima.status] : undefined;
          this.veiculo.km = comKm ? this.formatarKm(comKm) : undefined;
          this.veiculo.historico = ordenadas.map(o => ({
            id: o.id,
            data: o.dataEntrada ? new Date(o.dataEntrada + 'T00:00:00').toLocaleDateString('pt-BR') : '—',
            servico: o.diagnostico || o.reclamacoes || `OS #${o.id} · ${ROTULOS_STATUS[o.status]}`,
            km: this.formatarKm(o),
            valor: (o.totalGeral ?? 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
          }));
        }
        this.carregandoHistorico = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.carregandoHistorico = false;
        this.historicoErro = 'Não foi possível carregar o histórico deste veículo.';
        this.cdr.markForCheck();
      }
    });
  }

  private formatarKm(os: OrdemServico): string {
    return os.kmEntrada == null ? 'Não informada' : os.kmEntrada.toLocaleString('pt-BR') + ' km';
  }

  selecionarPlaca(placa: string): void {
    if (this.carregando || this.carregandoHistorico) return;
    this.placa = placa;
    this.buscar();
  }

  obterTagClass(status?: string): string {
    if (status === 'Em andamento') return 'tag-accent';
    if (status === 'Concluída') return 'tag-accent-2';
    if (status === 'Aberta') return 'tag-outline';
    return 'tag-neutral';
  }

  abrirNovaOs(): void {
    this.router.navigate(['/ordens/nova'], { queryParams: this.veiculo ? { placa: this.veiculo.placa } : {} });
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
          this.placa = v.placa;
          this.buscar();
          this.cdr.markForCheck();
        }
      });
  }
}
