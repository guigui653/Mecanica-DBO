import { Component, HostListener, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AtendimentoService } from '../../../core/services/atendimento';
import { ClienteService } from '../../../core/services/cliente';
import { Cliente } from '../../../shared/models/cliente.model';
import { Combustivel } from '../../../shared/models/veiculo.model';
import { VeiculoResumo } from '../../../shared/models/ordem-servico.model';
import { PlacaPipe } from '../../../shared/pipes/placa.pipe';
import { documentoDosCampos, formatarCnpjDigitando, formatarCpfDigitando } from '../../../shared/pipes/cpf.pipe';
import { mensagemDoErro } from '../../../core/utils/mensagem-erro';

@Component({
  selector: 'app-modal-novo-cliente',
  standalone: true,
  imports: [CommonModule, FormsModule, PlacaPipe],
  templateUrl: './modal-novo-cliente.html',
  styleUrl: './modal-novo-cliente.css'
})
export class ModalNovoCliente implements OnInit {
  private atendimentoService = inject(AtendimentoService);
  private clienteService = inject(ClienteService);

  /** Já abre com este cliente escolhido (ex.: "carro novo" na ficha do cliente). */
  clienteInicial = input<Cliente | null>(null);
  /** Começa no modo "Já é cliente" (ex.: botão "Novo veículo" da aba Veículos). */
  comecarComClienteExistente = input(false);

  // ── Proprietário: novo ou já cadastrado ──
  modoCliente = signal<'novo' | 'existente'>('novo');
  clienteEscolhido = signal<Cliente | null>(null);
  buscaCliente = signal('');
  private todosClientes = signal<Cliente[]>([]);
  clientesEncontrados = computed(() => {
    const q = this.buscaCliente().trim().toLowerCase();
    if (!q) return [];
    const digitos = q.replace(/\D/g, '');
    return this.todosClientes()
      .filter(c => c.nome.toLowerCase().includes(q)
        || (digitos.length >= 3 && (c.telefone1 ?? '').replace(/\D/g, '').includes(digitos)))
      .slice(0, 6);
  });
  private router = inject(Router);

  /** Emite true se algo foi salvo com sucesso (para o pai recarregar dados) */
  fechado = output<boolean>();

  // ── Formulário: Cliente ──
  nome = signal('');
  telefone = signal('');
  /** Pessoa física: CPF. Empresa: CNPJ. Preenche um ou outro (ou nenhum). */
  cpf = signal('');
  cnpj = signal('');

  /** Formata no próprio campo enquanto a pessoa digita. */
  alterarCpf(evento: Event): void {
    const campo = evento.target as HTMLInputElement;
    campo.value = formatarCpfDigitando(campo.value);
    this.cpf.set(campo.value);
    this.erro.set('');
  }

  alterarCnpj(evento: Event): void {
    const campo = evento.target as HTMLInputElement;
    campo.value = formatarCnpjDigitando(campo.value);
    this.cnpj.set(campo.value);
    this.erro.set('');
  }

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

  ngOnInit(): void {
    const inicial = this.clienteInicial();
    if (inicial) {
      this.modoCliente.set('existente');
      this.clienteEscolhido.set(inicial);
    } else if (this.comecarComClienteExistente()) {
      this.mudarModo('existente');
    }
  }

  mudarModo(modo: 'novo' | 'existente'): void {
    this.modoCliente.set(modo);
    this.erro.set('');
    if (modo === 'existente' && this.todosClientes().length === 0) {
      this.clienteService.listar().subscribe({
        next: lista => this.todosClientes.set(lista.filter(c => c.ativo !== false)),
        error: e => this.erro.set(mensagemDoErro(e))
      });
    }
  }

  escolherCliente(c: Cliente): void {
    this.clienteEscolhido.set(c);
    this.buscaCliente.set('');
    this.erro.set('');
  }

  trocarCliente(): void {
    this.clienteEscolhido.set(null);
    this.mudarModo('existente');
  }

  /** Esc age como o X: pergunta antes de jogar fora o que foi digitado. */
  @HostListener('document:keydown.escape')
  aoPressionarEsc(): void {
    if (!this.salvando()) {
      this.fechar();
    }
  }

  /**
   * Clique fora da janela NÃO fecha: era fácil perder tudo o que foi
   * digitado com um clique sem querer. Para sair, use o X ou "Cancelar".
   */
  aoClicarBackdrop(_evento: MouseEvent): void {}

  /** Algo foi digitado e ainda não foi salvo? */
  temDadosNaoSalvos(): boolean {
    if (this.sucesso()) return false;
    const textos = [this.nome(), this.telefone(), this.cpf(), this.cnpj(),
                    this.placa(), this.marca(), this.modelo(), this.cor()];
    return textos.some(t => t.trim() !== '') || this.anoFabricacao() !== null
      || (this.clienteEscolhido() !== null && !this.clienteInicial());
  }

  alterarPlaca(valor: string): void {
    this.placa.set(valor.toUpperCase());
    this.erro.set('');
  }

  salvar(): void {
    if (this.salvando()) return;

    const nome = this.nome().trim();
    const telefone = this.telefone().trim();
    const placa = this.placa().trim().toUpperCase().replace(/[\s-]/g, '');
    const marca = this.marca().trim();
    const modelo = this.modelo().trim();
    const cor = this.cor().trim();

    const existente = this.modoCliente() === 'existente';

    // ── Validações amigáveis ──
    if (existente) {
      if (!this.clienteEscolhido()?.id) {
        this.erro.set('Escolha o cliente: digite o nome ou o telefone e clique nele na lista.');
        return;
      }
    } else {
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
      const { erro: erroDoc } = documentoDosCampos(this.cpf(), this.cnpj());
      if (erroDoc) {
        this.erro.set(erroDoc);
        return;
      }
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
      ...(existente
        ? { clienteId: this.clienteEscolhido()!.id }
        : {
            novoCliente: {
              nome,
              telefone1: telefone,
              cpf: documentoDosCampos(this.cpf(), this.cnpj()).doc
            }
          }),
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
        this.erro.set(mensagemDoErro(e));
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
    if (this.temDadosNaoSalvos()
        && !confirm('Sair sem salvar? O que foi digitado neste cadastro será perdido.')) {
      return;
    }
    this.fechado.emit(this.sucesso());
  }
}
