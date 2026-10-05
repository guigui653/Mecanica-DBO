import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { VeiculoService } from '../../../core/services/veiculo';
import { OrdemServicoService } from '../../../core/services/ordem-servico';
import { ClienteService } from '../../../core/services/cliente';
import { Veiculo, Combustivel } from '../../../shared/models/veiculo.model';
import { Cliente } from '../../../shared/models/cliente.model';

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
  private clienteService = inject(ClienteService);
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
    'GNV'
  ];

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
    this.placaNaoCadastrada.set(false);
    this.erro.set('');
  }

  private tratarErro(e: any): string {
    if (!e) return 'Erro desconhecido.';
    if (e.status === 0) return 'Sem conexão com a API.';
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
          this.veiculo.set(veiculo);
          this.placa.set(veiculo.placa);
          this.buscando.set(false);
          this.placaNaoCadastrada.set(false);
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

  salvarVeiculoENovaOs(): void {
    if (this.salvando()) return;
    this.erro.set('');

    const placaFinal = this.placa().trim().toUpperCase().replace(/\s/g, '');
    if (!placaFinal) {
      this.erro.set('Informe a placa do veículo.');
      return;
    }

    if (this.modoCliente() === 'buscar') {
      const c = this.clienteSelecionado();
      if (!c || !c.id) {
        this.erro.set('Selecione o proprietário do veículo ou cadastre um novo.');
        return;
      }
      this.cadastrarVeiculoEOpenOs(c.id);
    } else {
      // Validar dados do novo cliente
      const nome = this.novoNomeCliente().trim();
      const telefone = this.novoTelefoneCliente().trim();
      if (!nome) {
        this.erro.set('Nome do cliente é obrigatório.');
        return;
      }
      if (!telefone) {
        this.erro.set('Telefone do cliente é obrigatório.');
        return;
      }

      this.salvando.set(true);
      this.clienteService.criar({
        nome,
        telefone1: telefone,
        cpf: this.novoCpfCliente().trim() || undefined
      }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: novoC => {
          if (!novoC.id) {
            this.salvando.set(false);
            this.erro.set('Erro ao cadastrar cliente.');
            return;
          }
          this.clienteSelecionado.set(novoC);
          this.cadastrarVeiculoEOpenOs(novoC.id);
        },
        error: e => {
          this.salvando.set(false);
          this.erro.set(this.tratarErro(e));
        }
      });
    }
  }

  private cadastrarVeiculoEOpenOs(clienteId: number): void {
    const marca = this.marca().trim();
    const modelo = this.modelo().trim();
    const cor = this.cor().trim();
    const combustivel = this.combustivel();
    const ano = this.anoFabricacao();
    const placaFinal = this.placa().trim().toUpperCase().replace(/\s/g, '');

    if (!marca) {
      this.salvando.set(false);
      this.erro.set('Marca do veículo é obrigatória.');
      return;
    }
    if (!modelo) {
      this.salvando.set(false);
      this.erro.set('Modelo do veículo é obrigatório.');
      return;
    }
    if (!cor) {
      this.salvando.set(false);
      this.erro.set('Cor do veículo é obrigatória.');
      return;
    }

    this.salvando.set(true);
    const veiculoReq: Veiculo = {
      placa: placaFinal,
      marca,
      modelo,
      cor,
      combustivel,
      anoFabricacao: ano ? Number(ano) : undefined
    };

    this.veiculoService.criar(clienteId, veiculoReq)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: veiculoCriado => {
          this.veiculo.set(veiculoCriado);
          this.placaNaoCadastrada.set(false);
          // Continua para abrir a OS
          this.enviarAberturaOs(veiculoCriado.id!);
        },
        error: e => {
          this.salvando.set(false);
          this.erro.set(this.tratarErro(e));
        }
      });
  }

  criar(): void {
    if (this.salvando() || this.buscando()) return;
    this.erro.set('');

    if (this.placaNaoCadastrada()) {
      this.salvarVeiculoENovaOs();
      return;
    }

    const veiculoId = this.veiculo()?.id;
    if (!veiculoId) {
      this.erro.set('Busque e selecione um veículo cadastrado antes de criar a OS.');
      return;
    }

    this.enviarAberturaOs(veiculoId);
  }

  private enviarAberturaOs(veiculoId: number): void {
    const relato = this.reclamacoes().trim();
    if (!relato) {
      this.salvando.set(false);
      this.erro.set('Relato do cliente é obrigatório.');
      return;
    }

    const km = this.km();
    if (km !== null && km !== undefined && (!Number.isInteger(Number(km)) || Number(km) < 0 || Number(km) > 2147483647)) {
      this.salvando.set(false);
      this.erro.set('Informe uma quilometragem inteira entre 0 e 2.147.483.647.');
      return;
    }

    this.salvando.set(true);
    this.osService.criar(veiculoId, {
      kmEntrada: km !== null && km !== undefined ? Number(km) : undefined,
      dataSaidaPrevista: this.previsao() || undefined,
      reclamacoes: relato
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: os => {
        this.salvando.set(false);
        if (os.id) this.router.navigate(['/ordens', os.id]);
        else this.router.navigate(['/ordens']);
      },
      error: e => {
        this.salvando.set(false);
        this.erro.set(this.tratarErro(e));
      }
    });
  }
}
