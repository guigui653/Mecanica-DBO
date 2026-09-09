import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { VeiculoService } from '../../../core/services/veiculo';
import { Veiculo } from '../../../shared/models/veiculo.model';

@Component({
  selector: 'app-busca-veiculo',
  standalone: true,
  imports: [CommonModule, FormsModule, MatFormFieldModule, MatInputModule,
            MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './busca-veiculo.html',
  styleUrl: './busca-veiculo.css'
})
export class BuscaVeiculo {
  private service = inject(VeiculoService);
  private router  = inject(Router);

  placa = '';
  veiculo: Veiculo | null = null;
  carregando = false;
  erro = '';

  buscar() {
    if (!this.placa.trim()) return;
    this.carregando = true;
    this.erro = '';
    this.veiculo = null;
    this.service.buscarPorPlaca(this.placa.trim().toUpperCase()).subscribe({
      next: (v) => { this.veiculo = v; this.carregando = false; },
      error: () => { this.erro = 'Veículo não encontrado'; this.carregando = false; }
    });
  }

  abrirOs(id: number) {
    this.router.navigate(['/ordens', id]);
  }
}
