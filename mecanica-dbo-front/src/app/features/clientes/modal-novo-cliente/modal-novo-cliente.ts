import { Component, HostListener, inject, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AtendimentoService } from '../../../core/services/atendimento';
import { Cliente } from '../../../shared/models/cliente.model';
import { Combustivel } from '../../../shared/models/veiculo.model';
import { VeiculoResumo } from '../../../shared/models/ordem-servico.model';
import { PlacaPipe } from '../../../shared/pipes/placa.pipe';

@Component({
  selector: 'app-modal-novo-cliente',
  standalone: true,
  imports: [CommonModule, FormsModule, PlacaPipe],
  templateUrl: './modal-novo-cliente.html',
  styleUrl: './modal-novo-cliente.css'
})
export class ModalNovoCliente {
  private atendimentoService = inject(AtendimentoService);
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
  veiculoCadastrado = signal<VeiculoResumo | null>(null);

  readonly combustiveis: Combustivel[] = [
    'FLEX',
    'GASOLINA',
    'ETANOL',
    'DIESEL',
    'ELETRICO',
    'HIBRIDO',
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
    if (e.error?.campos) return Object.values(e.error.campos).join(' · ');
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

    // Uma chamada só: o backend grava cliente e veículo na mesma transação.
    // Se a placa já existir, o cliente também não é gravado — e um novo
    // clique não cria cliente repetido.
    this.atendimentoService.registrar({
      novoCliente: {
        nome,
        telefone1: telefone,
        cpf: this.cpf().trim() || undefined,
        email: this.email().trim() || undefined
      },
      novoVeiculo: {
        placa,
        marca,
        modelo,
        cor,
        combustivel: this.combustivel(),
        anoFabricacao: this.anoFabricacao() ? Number(this.anoFabricacao()) : undefined
      }
    }).subscribe({
      next: r => {
        this.veiculoCadastrado.set(r.veiculo);
        this.clienteCadastrado.set(r.veiculo.cliente ?? null);
        this.salvando.set(false);
        this.sucesso.set(true);
      },
      error: e => {
        this.salvando.set(false);
        this.erro.set(this.tratarErro(e));
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
