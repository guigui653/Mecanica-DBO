import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { VeiculoService } from '../../../core/services/veiculo';
import { ClienteService } from '../../../core/services/cliente';
import { AtendimentoRequest, AtendimentoService, DadosAbertura } from '../../../core/services/atendimento';
import { Veiculo, Combustivel } from '../../../shared/models/veiculo.model';
import { Cliente } from '../../../shared/models/cliente.model';

/**
 * Abertura de OS.
 *
 * Cliente e veículo novos NÃO são gravados no meio do caminho: a tela só junta
 * os dados, e o botão final faz uma única chamada (POST /atendimentos), que o
 * backend grava numa transação. Se a placa já existir, nada fica gravado —
 * antes, o cliente ficava salvo e um novo clique criava um cliente repetido.
 *
 * Aceita na URL:
 *   ?placa=ABC1234  → já busca a placa
 *   ?veiculoId=3    → já seleciona o veículo (vindo da ficha do cliente)
 *   ?clienteId=7    → mostra os veículos do cliente; carro novo já sai com ele como dono
 */
@Component({
  selector: 'app-nova-os',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './nova-os.html',
  styleUrl: './nova-os.css'
})
export class NovaOs implements OnInit {
  private veiculoService = inject(VeiculoService);
  private clienteService = inject(ClienteService);
  private atendimentoService = inject(AtendimentoService);
  private rota = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  // Etapa 1: Busca e identificação da placa
  placa = signal('');
  veiculo = signal<Veiculo | null>(null);
  buscando = signal(false);
  salvando = signal(false);
  erro = signal('');
  placaNaoCadastrada = signal(false);

  // Veículos do cliente, quando a tela é aberta pela ficha (?clienteId=)
  clienteDaFicha = signal<Cliente | null>(null);
  veiculosDoCliente = signal<Veiculo[]>([]);

  // Etapa 2: Seleção ou cadastro de Proprietário (se veículo não cadastrado)
  modoCliente = signal<'buscar' | 'novo'>('buscar');
  termoCliente = signal('');
  buscandoCliente = signal(false);
  clientesEncontrados = signal<Cliente[]>([]);
  clienteSelecionado = signal<Cliente | null>(null);

  // Form Novo Cliente
  novoNomeCliente = signal('');
  novoTelefoneCliente = signal('');
  novoCpfCliente = signal('');

  // Form Novo Veículo
  marca = signal('');
  modelo = signal('');
  cor = signal('');
  combustivel = signal<Combustivel>('FLEX');
  anoFabricacao = signal<number | null>(null);

  // Etapa 3: Dados da Abertura da Ordem de Serviço
  km = signal<number | null>(null);
  previsao = signal('');
  reclamacoes = signal('');

  readonly combustiveis: Combustivel[] = [
    'GASOLINA',
    'ETANOL',
    'FLEX',
    'DIESEL',
    'ELETRICO',
    'HIBRIDO',
    'GNV'
  ];

  ngOnInit(): void {
    const params = this.rota.snapshot.queryParamMap;
    const veiculoId = Number(params.get('veiculoId'));
    const clienteId = Number(params.get('clienteId'));
    const placa = params.get('placa');

    if (veiculoId > 0) {
      this.carregarVeiculo(veiculoId);
    } else if (clienteId > 0) {
      this.carregarCliente(clienteId);
    } else if (placa) {
      this.placa.set(placa);
      this.buscarVeiculo();
    }
  }

  private carregarVeiculo(id: number): void {
    this.buscando.set(true);
    this.veiculoService.buscarPorId(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: v => {
          this.selecionarVeiculo(v);
          this.buscando.set(false);
        },
        error: e => {
          this.buscando.set(false);
          this.erro.set(this.tratarErro(e));
        }
      });
  }

  /** Vindo da ficha: o cliente já é o dono de qualquer carro novo cadastrado aqui. */
  private carregarCliente(id: number): void {
    this.clienteService.buscarPorId(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: c => {
          this.clienteDaFicha.set(c);
          this.clienteSelecionado.set(c);
          this.modoCliente.set('buscar');
        },
        error: e => this.erro.set(this.tratarErro(e))
      });
    this.clienteService.listarVeiculos(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: lista => this.veiculosDoCliente.set(lista ?? []),
        error: () => this.veiculosDoCliente.set([])
      });
  }

  selecionarVeiculo(v: Veiculo): void {
    this.veiculo.set(v);
    this.placa.set(v.placa);
    this.placaNaoCadastrada.set(false);
    this.erro.set('');
  }

  alterarPlaca(placa: string): void {
    this.placa.set(placa);
    this.veiculo.set(null);
    this.placaNaoCadastrada.set(false);
    this.erro.set('');
  }

  private tratarErro(e: any): string {
    if (!e) return 'Erro desconhecido.';
    if (e.status === 0) return 'Sem conexão com a API.';
    if (e.error?.campos) return Object.values(e.error.campos).join(' · ');
    if (e.error?.mensagem) return e.error.mensagem;
    if (e.message) return e.message;
    return 'Não foi possível completar a operação. Tente novamente.';
  }

  buscarVeiculo(): void {
    if (this.buscando() || this.salvando()) return;
    const digitada = this.placa().trim().toUpperCase();
    const semEspeciais = digitada.replace(/[\s-]/g, '');

    this.veiculo.set(null);
    this.placaNaoCadastrada.set(false);
    this.erro.set('');

    if (!semEspeciais) {
      this.erro.set('Informe uma placa válida, como ABC1234 ou ABC1D23.');
      return;
    }

    // Gerar tentativas de grafia: primeiro limpa (sem espaço/hífen), depois com hífen AAA-0000, depois como digitado
    const tentativas: string[] = [];
    if (semEspeciais) tentativas.push(semEspeciais);
    if (semEspeciais.length === 7) {
      const comHifen = `${semEspeciais.substring(0, 3)}-${semEspeciais.substring(3)}`;
      if (!tentativas.includes(comHifen)) {
        tentativas.push(comHifen);
      }
    }
    if (digitada && !tentativas.includes(digitada)) {
      tentativas.push(digitada);
    }

    this.buscando.set(true);
    this.tentarBuscar(tentativas, 0);
  }

  private tentarBuscar(tentativas: string[], index: number): void {
    if (index >= tentativas.length) {
      this.buscando.set(false);
      this.placaNaoCadastrada.set(true);
      return;
    }

    const placaAtual = tentativas[index];
    this.veiculoService.buscarPorPlaca(placaAtual)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: veiculo => {
          this.selecionarVeiculo(veiculo);
          this.buscando.set(false);
        },
        error: (e: HttpErrorResponse) => {
          if (e.status === 404) {
            this.tentarBuscar(tentativas, index + 1);
          } else {
            this.buscando.set(false);
            this.erro.set(this.tratarErro(e));
          }
        }
      });
  }

  buscarClientes(): void {
    const termo = this.termoCliente().trim();
    if (!termo) {
      this.erro.set('Digite o nome, CPF ou telefone para buscar o cliente.');
      return;
    }
    this.buscandoCliente.set(true);
    this.erro.set('');
    this.clienteService.listar(termo)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: lista => {
          this.clientesEncontrados.set(lista || []);
          this.buscandoCliente.set(false);
          if (!lista || lista.length === 0) {
            this.erro.set('Nenhum cliente encontrado com esse termo.');
          }
        },
        error: e => {
          this.buscandoCliente.set(false);
          this.erro.set(this.tratarErro(e));
        }
      });
  }

  selecionarCliente(cliente: Cliente): void {
    this.clienteSelecionado.set(cliente);
    this.erro.set('');
  }

  criar(): void {
    if (this.salvando() || this.buscando()) return;
    this.erro.set('');

    // Ordem das checagens = ordem da tela: veículo/dono primeiro, depois a OS.
    let pedido: AtendimentoRequest;
    if (this.placaNaoCadastrada()) {
      const cadastro = this.montarCadastro();
      if (!cadastro) return;
      const os = this.montarAbertura();
      if (!os) return;
      pedido = { ...cadastro, os };
    } else {
      const veiculoId = this.veiculo()?.id;
      if (!veiculoId) {
        this.erro.set('Busque e selecione um veículo cadastrado antes de criar a OS.');
        return;
      }
      const os = this.montarAbertura();
      if (!os) return;
      pedido = { veiculoId, os };
    }

    this.salvando.set(true);
    this.atendimentoService.registrar(pedido)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: r => {
          this.salvando.set(false);
          if (r.os?.id) this.router.navigate(['/ordens', r.os.id]);
          else this.router.navigate(['/ordens']);
        },
        error: e => {
          // Nada foi gravado: os dados digitados continuam na tela para corrigir.
          this.salvando.set(false);
          this.erro.set(this.tratarErro(e));
          this.focarCampoDoErro(e);
        }
      });
  }

  /** Dados da OS. Devolve null (e mostra o erro) se algo estiver faltando. */
  private montarAbertura(): DadosAbertura | null {
    const relato = this.reclamacoes().trim();
    if (!relato) {
      this.erro.set('Relato do cliente é obrigatório.');
      return null;
    }

    const km = this.km();
    if (km !== null && km !== undefined && (!Number.isInteger(Number(km)) || Number(km) < 0 || Number(km) > 2147483647)) {
      this.erro.set('Informe uma quilometragem inteira entre 0 e 2.147.483.647.');
      return null;
    }

    return {
      kmEntrada: km !== null && km !== undefined ? Number(km) : undefined,
      dataSaidaPrevista: this.previsao() || undefined,
      reclamacoes: relato
    };
  }

  /** Dono e veículo novos. Devolve null (e mostra o erro) se algo estiver faltando. */
  private montarCadastro(): Pick<AtendimentoRequest, 'clienteId' | 'novoCliente' | 'novoVeiculo'> | null {
    const placaFinal = this.placa().trim().toUpperCase().replace(/\s/g, '');
    if (!placaFinal) {
      this.erro.set('Informe a placa do veículo.');
      return null;
    }

    let dono: Pick<AtendimentoRequest, 'clienteId' | 'novoCliente'>;
    if (this.modoCliente() === 'buscar') {
      const c = this.clienteSelecionado();
      if (!c || !c.id) {
        this.erro.set('Selecione o proprietário do veículo ou cadastre um novo.');
        return null;
      }
      dono = { clienteId: c.id };
    } else {
      const nome = this.novoNomeCliente().trim();
      const telefone = this.novoTelefoneCliente().trim();
      if (!nome) {
        this.erro.set('Nome do cliente é obrigatório.');
        return null;
      }
      if (!telefone) {
        this.erro.set('Telefone do cliente é obrigatório.');
        return null;
      }
      dono = { novoCliente: { nome, telefone1: telefone, cpf: this.novoCpfCliente().trim() || undefined } };
    }

    const marca = this.marca().trim();
    const modelo = this.modelo().trim();
    const cor = this.cor().trim();
    if (!marca) {
      this.erro.set('Marca do veículo é obrigatória.');
      return null;
    }
    if (!modelo) {
      this.erro.set('Modelo do veículo é obrigatório.');
      return null;
    }
    if (!cor) {
      this.erro.set('Cor do veículo é obrigatória.');
      return null;
    }

    const ano = this.anoFabricacao();
    return {
      ...dono,
      novoVeiculo: {
        placa: placaFinal,
        marca,
        modelo,
        cor,
        combustivel: this.combustivel(),
        anoFabricacao: ano ? Number(ano) : undefined
      }
    };
  }

  /** Leva o cursor ao campo que causou o erro (placa repetida → campo da placa, etc.). */
  private focarCampoDoErro(e: any): void {
    const texto = `${e?.error?.mensagem ?? ''} ${Object.keys(e?.error?.campos ?? {}).join(' ')}`;
    let id: string | null = null;
    if (/placa/i.test(texto)) id = 'nova-placa';
    else if (/cpf/i.test(texto)) id = 'novo-cpf';
    else if (/novoCliente/.test(texto)) id = 'novo-nome';
    else if (/novoVeiculo/.test(texto)) id = 'veic-marca';
    else if (/previs/i.test(texto)) id = 'nova-previsao';
    if (id) setTimeout(() => document.getElementById(id!)?.focus());
  }
}
