import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { VeiculoService } from '../../../core/services/veiculo';
import { OrdemServicoService } from '../../../core/services/ordem-servico';
import { Veiculo } from '../../../shared/models/veiculo.model';

@Component({
  selector: 'app-nova-os',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './nova-os.html',
  styleUrl: './nova-os.css'
})
export class NovaOs implements OnInit {
  private veiculoService = inject(VeiculoService);
  private osService = inject(OrdemServicoService);
  private rota = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  placa = signal('');
  veiculo = signal<Veiculo | null>(null);
  km = signal<number | null>(null);
  previsao = signal('');
  reclamacoes = signal('');
  buscando = signal(false);
  salvando = signal(false);
  erro = signal('');

  ngOnInit(): void {
    const placa = this.rota.snapshot.queryParamMap.get('placa');
    if (placa) {
      this.placa.set(placa);
      this.buscarVeiculo();
    }
  }

  alterarPlaca(placa: string): void {
    this.placa.set(placa);
    this.veiculo.set(null);
  }

  buscarVeiculo(): void {
    if (this.buscando() || this.salvando()) return;
    const placa = this.placa().trim().toUpperCase().replace(/[\s-]/g, '');
    this.veiculo.set(null);
    if (!/^[A-Z]{3}\d[A-Z0-9]\d{2}$/.test(placa)) {
      this.erro.set('Informe uma placa válida, como ABC1234 ou ABC1D23.');
      return;
    }
    this.placa.set(placa);
    this.buscando.set(true);
    this.erro.set('');
    this.veiculoService.buscarPorPlaca(placa).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: veiculo => {
        this.veiculo.set(veiculo);
        this.buscando.set(false);
      },
      error: e => {
        this.buscando.set(false);
        this.erro.set(e.status === 404
          ? 'Veículo não cadastrado. Cadastre-o na seção Veículos antes de abrir a OS.'
          : 'Não foi possível consultar o veículo. Tente novamente.');
      }
    });
  }

  criar(): void {
    const veiculoId = this.veiculo()?.id;
    if (this.salvando() || this.buscando()) return;
    if (!veiculoId) {
      this.erro.set('Busque e selecione um veículo cadastrado antes de criar a OS.');
      return;
    }
    const km = this.km();
    if (km !== null && (!Number.isInteger(km) || km < 0 || km > 2147483647)) {
      this.erro.set('Informe uma quilometragem inteira entre 0 e 2.147.483.647.');
      return;
    }
    this.salvando.set(true);
    this.erro.set('');
    this.osService.criar(veiculoId, {
      kmEntrada: km ?? undefined,
      dataSaidaPrevista: this.previsao() || undefined,
      reclamacoes: this.reclamacoes().trim() || undefined
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: os => {
        this.salvando.set(false);
        if (os.id) this.router.navigate(['/ordens', os.id]);
        else this.router.navigate(['/ordens']);
      },
      error: e => {
        this.salvando.set(false);
        this.erro.set(e.error?.mensagem ?? 'Não foi possível criar a OS. Tente novamente.');
      }
    });
  }
}
