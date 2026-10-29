import { Component, DestroyRef, effect, inject, input, signal } from '@angular/core';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ClienteService } from '../../../core/services/cliente';
import { Veiculo } from '../../../shared/models/veiculo.model';

/**
 * Lista de veículos na ficha do cliente, com "Abrir OS" em cada carro.
 * Os botões levam à Nova OS já com o veículo (?veiculoId=) ou com o cliente
 * (?clienteId=, para cadastrar um carro novo dele).
 */
@Component({
  selector: 'app-veiculos-do-cliente',
  standalone: true,
  templateUrl: './veiculos-do-cliente.html',
  styleUrl: './veiculos-do-cliente.css'
})
export class VeiculosDoCliente {
  private clienteService = inject(ClienteService);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  clienteId = input.required<number>();

  veiculos   = signal<Veiculo[]>([]);
  carregando = signal(true);
  erro       = signal(false);

  constructor() {
    effect(() => this.carregar(this.clienteId()));
  }

  private carregar(id: number): void {
    this.carregando.set(true);
    this.erro.set(false);
    this.clienteService.listarVeiculos(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: lista => {
          this.veiculos.set(lista ?? []);
          this.carregando.set(false);
        },
        error: () => {
          this.erro.set(true);
          this.carregando.set(false);
        }
      });
  }

  abrirOs(v: Veiculo): void {
    this.router.navigate(['/ordens/nova'], { queryParams: { veiculoId: v.id } });
  }

  novoVeiculo(): void {
    this.router.navigate(['/ordens/nova'], { queryParams: { clienteId: this.clienteId() } });
  }
}
