import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { VeiculoService } from '../../../core/services/veiculo';
import { ClienteService } from '../../../core/services/cliente';
import { Veiculo, Combustivel } from '../../../shared/models/veiculo.model';
import { Cliente } from '../../../shared/models/cliente.model';

@Component({
  selector: 'app-form-veiculo',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatDialogModule, MatFormFieldModule,
            MatInputModule, MatButtonModule, MatSelectModule],
  template: `
    <h2 mat-dialog-title>{{ data?.veiculo?.id ? 'Editar' : 'Novo' }} Veículo</h2>

    <mat-dialog-content>
      <form [formGroup]="form" class="form-grid">
        <mat-form-field appearance="outline">
          <mat-label>Placa *</mat-label>
          <input matInput formControlName="placa" maxlength="8"
                 style="text-transform: uppercase;">
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Cliente *</mat-label>
          <mat-select formControlName="clienteId">
            @for (c of clientes; track c.id) {
              <mat-option [value]="c.id">{{ c.nome }} — {{ c.telefone1 }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Marca *</mat-label>
          <input matInput formControlName="marca">
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Modelo *</mat-label>
          <input matInput formControlName="modelo">
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Ano Fabricação</mat-label>
          <input matInput formControlName="anoFabricacao" type="number">
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Cor *</mat-label>
          <input matInput formControlName="cor">
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Combustível *</mat-label>
          <mat-select formControlName="combustivel">
            <mat-option value="FLEX">Flex</mat-option>
            <mat-option value="GASOLINA">Gasolina</mat-option>
            <mat-option value="ETANOL">Etanol</mat-option>
            <mat-option value="DIESEL">Diesel</mat-option>
            <mat-option value="GNV">GNV</mat-option>
            <mat-option value="ELETRICO">Elétrico</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Chassi</mat-label>
          <input matInput formControlName="chassi">
        </mat-form-field>
      </form>

      @if (erro) {
        <p style="color: red; margin: 8px 0;">{{ erro }}</p>
      }
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="fechar()">Cancelar</button>
      <button mat-flat-button color="primary"
              [disabled]="form.invalid || salvando" (click)="salvar()">
        {{ salvando ? 'Salvando...' : 'Salvar' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; min-width: 520px; padding-top: 8px; }
    .full { grid-column: 1 / -1; }
  `]
})
export class FormVeiculo implements OnInit {
  private fb = inject(FormBuilder);
  private service = inject(VeiculoService);
  private clienteService = inject(ClienteService);
  private ref = inject(MatDialogRef<FormVeiculo>);
  data = inject<{ veiculo?: Veiculo; clienteId?: number } | null>(MAT_DIALOG_DATA);

  clientes: Cliente[] = [];
  salvando = false;
  erro = '';

  form = this.fb.group({
    placa: [this.data?.veiculo?.placa ?? '', Validators.required],
    clienteId: [this.data?.clienteId ?? null, Validators.required],
    marca: [this.data?.veiculo?.marca ?? '', Validators.required],
    modelo: [this.data?.veiculo?.modelo ?? '', Validators.required],
    anoFabricacao: [this.data?.veiculo?.anoFabricacao ?? null],
    cor: [this.data?.veiculo?.cor ?? '', Validators.required],
    combustivel: [this.data?.veiculo?.combustivel ?? 'FLEX', Validators.required],
    chassi: [this.data?.veiculo?.chassi ?? ''],
  });

  ngOnInit() {
    this.clienteService.listar().subscribe({
      next: (c) => (this.clientes = c),
      error: () => (this.erro = 'Não foi possível carregar os clientes')
    });
  }

  salvar() {
    this.salvando = true;
    this.erro = '';
    const v = this.form.value;

    const veiculoPayload: Veiculo = {
      placa: (v.placa ?? '').toUpperCase().trim(),
      marca: v.marca ?? '',
      modelo: v.modelo ?? '',
      cor: v.cor ?? '',
      combustivel: (v.combustivel ?? 'FLEX') as Combustivel,
      anoFabricacao: v.anoFabricacao ? Number(v.anoFabricacao) : undefined,
      chassi: v.chassi || undefined
    };

    const clienteId = Number(v.clienteId);
    const veiculoId = this.data?.veiculo?.id;

    const req = veiculoId
      ? this.service.atualizar(veiculoId, veiculoPayload)
      : this.service.criar(clienteId, veiculoPayload);

    req.subscribe({
      next: (resp) => this.ref.close(resp),
      error: (e) => {
        this.erro = e.error?.mensagem ?? e.error?.message ?? 'Erro ao salvar veículo';
        this.salvando = false;
      }
    });
  }

  fechar() { this.ref.close(false); }
}
