import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { ClienteService } from '../../../core/services/cliente';
import { Cliente } from '../../../shared/models/cliente.model';

import { erroDocumento, formatarCpf, formatarDocumentoDigitando, limparDocumento } from '../../../shared/pipes/cpf.pipe';
import { mensagemDoErro } from '../../../core/utils/mensagem-erro';

@Component({
  selector: 'app-form-cliente',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatDialogModule,
            MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ data?.id ? 'Editar' : 'Novo' }} Cliente</h2>

    <mat-dialog-content>
      <form [formGroup]="form" class="form-grid">
        <mat-form-field appearance="outline">
          <mat-label>Nome *</mat-label>
          <input matInput formControlName="nome" maxlength="120">
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>CPF ou CNPJ</mat-label>
          <input matInput formControlName="cpf" maxlength="18" autocomplete="off"
                 (input)="formatarDocumento()">
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Telefone 1 *</mat-label>
          <input matInput formControlName="telefone1" maxlength="20">
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Telefone 2</mat-label>
          <input matInput formControlName="telefone2" maxlength="20">
        </mat-form-field>

        <mat-form-field appearance="outline" class="full">
          <mat-label>E-mail</mat-label>
          <input matInput formControlName="email" type="email" maxlength="100">
        </mat-form-field>
      </form>

      @if (erro) { <p style="color:red">{{ erro }}</p> }
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="fechar()">Cancelar</button>
      <button mat-flat-button color="primary"
              [disabled]="form.invalid || salvando"
              (click)="salvar()">
        {{ salvando ? 'Salvando...' : 'Salvar' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; min-width: 500px; padding-top: 8px; }
    .full { grid-column: 1 / -1; }
  `]
})
export class FormCliente {
  private fb = inject(FormBuilder);
  private service = inject(ClienteService);
  private ref = inject(MatDialogRef<FormCliente>);
  data = inject<Cliente | null>(MAT_DIALOG_DATA);

  salvando = false;
  erro = '';

  form = this.fb.group({
    nome: [this.data?.nome ?? '', [Validators.required, Validators.maxLength(120)]],
    cpf: [formatarCpf(this.data?.cpf ?? '')],
    telefone1: [this.data?.telefone1 ?? '', Validators.required],
    telefone2: [this.data?.telefone2 ?? ''],
    email: [this.data?.email ?? '', Validators.email],
  });

  formatarDocumento() {
    const ctrl = this.form.controls.cpf;
    ctrl.setValue(formatarDocumentoDigitando(ctrl.value ?? ''), { emitEvent: false });
  }

  salvar() {
    // Documento que já estava no cadastro não é reconferido (igual ao backend).
    const doc = this.form.value.cpf ?? '';
    const mudou = limparDocumento(doc) !== limparDocumento(this.data?.cpf);
    const erroDoc = mudou ? erroDocumento(doc) : null;
    if (erroDoc) {
      this.erro = erroDoc;
      return;
    }
    this.salvando = true;
    this.erro = '';
    const payload = this.form.value as Cliente;

    const req = this.data?.id
      ? this.service.atualizar(this.data.id, payload)
      : this.service.criar(payload);

    req.subscribe({
      next: () => this.ref.close(true),
      error: (e) => {
        this.erro = mensagemDoErro(e, 'Erro ao salvar cliente');
        this.salvando = false;
      }
    });
  }

  fechar() { this.ref.close(false); }
}
