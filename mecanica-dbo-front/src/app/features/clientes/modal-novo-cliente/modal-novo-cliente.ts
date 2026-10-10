import { Component, HostListener, inject, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ClienteService } from '../../../core/services/cliente';
import { VeiculoService } from '../../../core/services/veiculo';
import { Cliente } from '../../../shared/models/cliente.model';
import { Combustivel, Veiculo } from '../../../shared/models/veiculo.model';

@Component({
  selector: 'app-modal-novo-cliente',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './modal-novo-cliente.html',
  styleUrl: './modal-novo-cliente.css'
})
export class ModalNovoCliente {
  private clienteService = inject(ClienteService);
  private veiculoService = inject(VeiculoService);
  private router = inject(Router);

  /** Emite true se algo foi salvo com sucesso (para o pai recarregar dados) */
  fechado = output<boolean>();

  // ── Formulário: Cliente ──
  nome = signal('');
  telefone = signal('');
  cpf = signal('');
  email = signal('');

  // ── Formulário: Veículo ──
  placa = signal('');
  marca = signal('');
  modelo = signal('');
  cor = signal('');
  combustivel = signal<Combustivel>('FLEX');
  anoFabricacao = signal<number | null>(null);

  // ── Estados da interface ──
  salvando = signal(false);
  erro = signal('');
  sucesso = signal(false);

  clienteCadastrado = signal<Cliente | null>(null);
  veiculoCadastrado = signal<Veiculo | null>(null);

  readonly combustiveis: Combustivel[] = [
    'FLEX',
    'GASOLINA',
    'ETANOL',
    'DIESEL',
    'ELETRICO',
    'GNV'
  ];

  @HostListener('document:keydown.escape')
  aoPressionarEsc(): void {
    if (!this.salvando()) {
      this.fechar();
    }
  }

  aoClicarBackdrop(evento: MouseEvent): void {
    if (!this.salvando()) {
      this.fechar();
    }
  }

  alterarPlaca(valor: string): void {
    this.placa.set(valor.toUpperCase());
    this.erro.set('');
  }

  private tratarErro(e: any): string {
    if (!e) return 'Erro inesperado.';
    if (e.status === 0) return 'Não foi possível conectar ao servidor (API offline).';
    if (e.error?.mensagem) return e.error.mensagem;
    if (e.error?.message) return e.error.message;
    if (e.message) return e.message;
    return 'Não foi possível completar o cadastro. Verifique os dados e tente novamente.';
  }

  salvar(): void {
    if (this.salvando()) return;

    const nome = this.nome().trim();
    const telefone = this.telefone().trim();
    const placa = this.placa().trim().toUpperCase().replace(/[\s-]/g, '');
    const marca = this.marca().trim();
    const modelo = this.modelo().trim();
    const cor = this.cor().trim();

    // ── Validações amigáveis ──
    if (!nome) {
      this.erro.set('Informe o nome do proprietário.');
      return;
    }
    if (nome.length < 2) {
      this.erro.set('O nome deve ter pelo menos 2 caracteres.');
      return;
    }
    if (!telefone) {
      this.erro.set('Informe o telefone ou WhatsApp de contato.');
      return;
    }
    if (!placa) {
      this.erro.set('Informe a placa do veículo.');
      return;
    }
    if (placa.length < 7) {
      this.erro.set('A placa informada está incompleta. Padrão: 7 caracteres (ex: ABC1D23 ou ABC1234).');
      return;
    }
    if (!marca) {
      this.erro.set('Informe a marca do veículo.');
      return;
    }
    if (!modelo) {
      this.erro.set('Informe o modelo do veículo.');
      return;
    }
    if (!cor) {
      this.erro.set('Informe a cor do veículo.');
      return;
    }

    this.salvando.set(true);
    this.erro.set('');

    // Passo 1: Cadastrar o cliente
    const clientePayload: Cliente = {
      nome,
      telefone1: telefone,
      cpf: this.cpf().trim() || undefined,
      email: this.email().trim() || undefined
    };

    this.clienteService.criar(clientePayload).subscribe({
      next: clienteCriado => {
        if (!clienteCriado.id) {
          this.salvando.set(false);
          this.erro.set('Falha ao obter identificador do cliente salvo.');
          return;
        }

        this.clienteCadastrado.set(clienteCriado);

        // Passo 2: Cadastrar o veículo vinculado a esse cliente
        const veiculoPayload: Veiculo = {
          placa,
          marca,
          modelo,
          cor,
          combustivel: this.combustivel(),
          anoFabricacao: this.anoFabricacao() ? Number(this.anoFabricacao()) : undefined
        };

        this.veiculoService.criar(clienteCriado.id, veiculoPayload).subscribe({
          next: veiculoCriado => {
            this.veiculoCadastrado.set(veiculoCriado);
            this.salvando.set(false);
            this.sucesso.set(true);
          },
          error: errVeiculo => {
            this.salvando.set(false);
            this.erro.set('Cliente cadastrado com sucesso, mas ocorreu um erro no veículo: ' + this.tratarErro(errVeiculo));
          }
        });
      },
      error: errCliente => {
        this.salvando.set(false);
        this.erro.set(this.tratarErro(errCliente));
      }
    });
  }

  iniciarOs(): void {
    const p = this.veiculoCadastrado()?.placa;
    if (p) {
      this.router.navigate(['/ordens/nova'], { queryParams: { placa: p } });
    } else {
      this.router.navigate(['/ordens/nova']);
    }
    this.fechado.emit(true);
  }

  concluir(): void {
    this.fechado.emit(true);
  }

  fechar(): void {
    this.fechado.emit(this.sucesso());
  }
}
