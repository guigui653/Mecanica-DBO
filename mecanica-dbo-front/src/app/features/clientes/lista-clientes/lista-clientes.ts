import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { ClienteService } from '../../../core/services/cliente';
import { Cliente } from '../../../shared/models/cliente.model';
import { FormCliente } from '../form-cliente/form-cliente';

@Component({
  selector: 'app-lista-clientes',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatTableModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule
  ],
  templateUrl: './lista-clientes.html',
  styleUrl: './lista-clientes.css'
})
export class ListaClientes implements OnInit {
  private service = inject(ClienteService);
  private dialog = inject(MatDialog);

  clientes: Cliente[] = [];
  colunas = ['nome', 'cpf', 'telefone1', 'email', 'acoes'];
  termoBusca = '';
  carregando = false;
  erro = '';

  ngOnInit() {
    this.carregar();
  }

  carregar() {
    this.carregando = true;
    this.erro = '';
    this.service.listar(this.termoBusca || undefined).subscribe({
      next: (dados) => {
        this.clientes = dados;
        this.carregando = false;
      },
      error: (e) => {
        this.erro = 'Erro ao carregar clientes: ' + e.message;
        this.carregando = false;
      }
    });
  }

  abrirForm(cliente?: Cliente) {
    const ref = this.dialog.open(FormCliente, { data: cliente ?? null });
    ref.afterClosed().subscribe(salvou => {
      if (salvou) this.carregar();
    });
  }

  inativar(cliente: Cliente) {
    if (!cliente.id) return;
    if (!confirm(`Inativar o cliente ${cliente.nome}?`)) return;

    this.service.inativar(cliente.id).subscribe({
      next: () => this.carregar(),
      error: (e) => this.erro = 'Erro ao inativar: ' + e.message
    });
  }
}
