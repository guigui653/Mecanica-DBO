import { Component, DestroyRef, HostListener, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription } from 'rxjs';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { OrdemServicoService } from '../../../core/services/ordem-servico';
import { ClienteService } from '../../../core/services/cliente';
import { VeiculoService } from '../../../core/services/veiculo';
import { MatDialog } from '@angular/material/dialog';
import { FormCliente } from '../../clientes/form-cliente/form-cliente';
import { FormVeiculo } from '../../veiculos/form-veiculo/form-veiculo';
import { PlacaPipe } from '../../../shared/pipes/placa.pipe';
import { mensagemDoErro } from '../../../core/utils/mensagem-erro';
import {
  OrdemServico, ItemPeca, ItemServico, StatusOs,
  ROTULOS_STATUS, PROXIMO_STATUS, classeBadge as badgeDoStatus
} from '../../../shared/models/ordem-servico.model';

interface Toast {
  id: number;
  texto: string;
  erro: boolean;
}

@Component({
  selector: 'app-detalhe-os',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, PlacaPipe],
  templateUrl: './detalhe-os.html',
  styleUrl: './detalhe-os.css'
})
export class DetalheOs implements OnInit {

  private rota    = inject(ActivatedRoute);
  private router  = inject(Router);
  private service = inject(OrdemServicoService);
  private destroyRef = inject(DestroyRef);
  private clienteService = inject(ClienteService);
  private veiculoService = inject(VeiculoService);
  private dialog = inject(MatDialog);

  /** Abrindo a janela de edição do cliente ou do veículo. */
  editandoCadastro = signal(false);
  private idSolicitado: number | null = null;
  private carga?: Subscription;

  os         = signal<OrdemServico | null>(null);
  carregando = signal(true);
  salvando   = signal(false);
  erro       = signal<string | null>(null);
  toasts     = signal<Toast[]>([]);
  formErro   = signal<string | null>(null);
  gerandoPdf = signal(false);

  // ── Formulário de dados da OS ─────────────────────────────
  fKm            = signal<number | null>(null);
  fSaidaPrevista = signal<string>('');
  fReclamacoes   = signal<string>('');
  fDiagnostico   = signal<string>('');

  /** Valores como vieram do servidor — base para detectar alteração. */
  private original = signal('');

  temAlteracoes = computed(() => this.os() !== null && this.snapshotForm() !== this.original());

  // ── Formulário de nova peça ───────────────────────────────
  pDescricao  = signal('');
  pQuantidade = signal('');
  pValor      = signal<number | null>(null);
  pPagoCliente = signal(false);
  /** Peça sendo corrigida (o formulário de baixo vira "editar"); null = lançando nova. */
  editandoPecaId = signal<number | null>(null);
  /** Serviço sendo corrigido; null = lançando novo. */
  editandoServicoId = signal<number | null>(null);
  pErro       = signal<string | null>(null);

  // ── Formulário de novo serviço ────────────────────────────
  sDescricao = signal('');
  sValor     = signal<number | null>(null);
  sErro      = signal<string | null>(null);

  // ── Derivados ─────────────────────────────────────────────

  /** OS entregue vira registro histórico: nada mais é editável. */
  somenteLeitura = computed(() => this.os()?.status === 'ENTREGUE');

  /** Barra de etapas: Aberta → Em andamento → Concluída → Entregue. */
  etapas = computed(() => {
    const ordem: StatusOs[] = ['ABERTA', 'EM_ANDAMENTO', 'CONCLUIDA', 'ENTREGUE'];
    const cores: Record<StatusOs, string> = {
      ABERTA: 'var(--status-aberta)',
      EM_ANDAMENTO: 'var(--status-em-andamento)',
      CONCLUIDA: 'var(--status-concluida)',
      ENTREGUE: 'var(--status-entregue)'
    };
    const atual = this.os()?.status;
    const posicao = atual ? ordem.indexOf(atual) : -1;
    return ordem.map((status, i) => ({
      status,
      numero: i + 1,
      rotulo: this.rotulo(status),
      cor: cores[status],
      feita: i <= posicao,
      atual: i === posicao
    }));
  });

  proximoPasso = computed(() => {
    const st = this.os()?.status;
    return st ? PROXIMO_STATUS[st] ?? null : null;
  });

  pecas    = computed(() => this.os()?.itensPeca    ?? []);
  servicos = computed(() => this.os()?.itensServico ?? []);

  /**
   * Peças marcadas como pagas pelo cliente ficam fora do total cobrado —
   * é o trigger do banco que faz esse corte. Somamos aqui só para exibir.
   */
  totalPagoPeloCliente = computed(() =>
    this.pecas()
      .filter(p => p.pagoPeloCliente)
      .reduce((s, p) => s + (p.valorTotal ?? 0), 0)
  );

  diasNaOficina = computed(() => {
    const entrada = this.os()?.dataEntrada;
    if (!entrada) return 0;
    const d = new Date(entrada + 'T00:00:00');
    return Math.max(0, Math.floor((Date.now() - d.getTime()) / 86400000));
  });

  ngOnInit(): void {
    this.rota.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      const id = Number(params.get('id'));
      this.carga?.unsubscribe();
      this.idSolicitado = null;
      this.os.set(null);
      if (!Number.isSafeInteger(id) || id <= 0) {
        this.erro.set('Número da ordem de serviço inválido.');
        this.carregando.set(false);
        return;
      }
      this.carregar(id);
    });
  }

  // ── Carga ─────────────────────────────────────────────────

  private carregar(id: number): void {
    this.idSolicitado = id;
    this.carregando.set(true);
    this.erro.set(null);

    this.carga = this.service.buscarPorId(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: os => {
        this.aplicar(os);
        this.carregando.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.erro.set(
          e.status === 404
            ? `Ordem de serviço #${id} não encontrada.`
            : 'Não foi possível carregar a OS. Verifique se a API está no ar.'
        );
        this.carregando.set(false);
      }
    });
  }

  /** Atualiza a OS na tela e sincroniza o formulário com o que veio do servidor. */
  private aplicar(os: OrdemServico, sincronizarFormulario = true): void {
    this.os.set(os);
    if (!sincronizarFormulario) return;
    this.fKm.set(os.kmEntrada ?? null);
    this.fSaidaPrevista.set(os.dataSaidaPrevista ?? '');
    this.fReclamacoes.set(os.reclamacoes ?? '');
    this.fDiagnostico.set(os.diagnostico ?? '');
    this.original.set(this.snapshotForm());
    this.formErro.set(null);
  }

  private snapshotForm(): string {
    return JSON.stringify([
      this.fKm(), this.fSaidaPrevista(), this.fReclamacoes(), this.fDiagnostico()
    ]);
  }

  recarregar(): void {
    if (this.idSolicitado) this.carregar(this.idSolicitado);
  }

  // ── Corrigir cadastro do proprietário e do veículo ───────
  //
  // Busca o cadastro completo (a OS só traz um resumo; abrir o formulário com o
  // resumo apagaria telefone 2 e e-mail ao salvar). Depois de salvar, atualiza
  // só o quadro do veículo/proprietário: o que estiver sendo digitado na OS
  // (diagnóstico, km...) não se perde.

  editarCliente(): void {
    const id = this.os()?.veiculo?.cliente?.id;
    if (!id || this.editandoCadastro()) return;
    this.editandoCadastro.set(true);
    this.clienteService.buscarPorId(id).subscribe({
      next: cliente => {
        this.dialog.open(FormCliente, { data: cliente, disableClose: true })
          .afterClosed().subscribe(salvou => {
            this.editandoCadastro.set(false);
            if (salvou) this.atualizarQuadroDoVeiculo('Cadastro do cliente atualizado');
          });
      },
      error: e => {
        this.editandoCadastro.set(false);
        this.notificar(mensagemDoErro(e), true);
      }
    });
  }

  editarVeiculo(): void {
    const v = this.os()?.veiculo;
    if (!v?.id || this.editandoCadastro()) return;
    this.editandoCadastro.set(true);
    this.veiculoService.buscarPorId(v.id).subscribe({
      next: veiculo => {
        this.dialog.open(FormVeiculo, {
          width: '640px', maxWidth: '95vw', disableClose: true,
          data: { veiculo, clienteId: v.cliente?.id }
        }).afterClosed().subscribe(salvo => {
          this.editandoCadastro.set(false);
          if (salvo) this.atualizarQuadroDoVeiculo('Dados do veículo atualizados');
        });
      },
      error: e => {
        this.editandoCadastro.set(false);
        this.notificar(mensagemDoErro(e), true);
      }
    });
  }

  private atualizarQuadroDoVeiculo(mensagem: string): void {
    const id = this.os()?.id;
    if (!id) return;
    this.service.buscarPorId(id).subscribe({
      next: os => {
        this.os.update(atual => atual ? { ...atual, veiculo: os.veiculo } : os);
        this.notificar(mensagem);
      },
      error: e => this.notificar(mensagemDoErro(e), true)
    });
  }

  // ── Salvar dados da OS ────────────────────────────────────

  salvar(): void {
    const id = this.os()?.id;
    if (!id || this.salvando() || this.somenteLeitura()) return;
    const km = this.fKm();
    if (km !== null && (!Number.isInteger(km) || km < 0 || km > 2147483647)) {
      this.formErro.set('Informe uma quilometragem inteira entre 0 e 2.147.483.647.');
      return;
    }
    this.formErro.set(null);

    this.salvando.set(true);
    this.service.atualizar(id, {
      kmEntrada:         this.fKm(),
      dataSaidaPrevista: this.fSaidaPrevista() || null,
      reclamacoes:       this.fReclamacoes() || null,
      diagnostico:       this.fDiagnostico() || null
    }).subscribe({
      next: os => {
        this.aplicar(os);
        this.salvando.set(false);
        this.notificar('Alterações salvas');
      },
      error: (e: HttpErrorResponse) => {
        this.salvando.set(false);
        this.notificar(mensagemDoErro(e), true);
      }
    });
  }

  // ── Status ────────────────────────────────────────────────

  avancarStatus(): void {
    const id = this.os()?.id;
    const passo = this.proximoPasso();
    if (!id || !passo || this.salvando()) return;
    if (this.temAlteracoes()) {
      this.notificar('Salve as alterações do diagnóstico antes de mudar a situação da OS.', true);
      return;
    }

    if (passo.status === 'ENTREGUE' &&
        !confirm('Marcar como entregue? A OS fica somente leitura depois disso.')) {
      return;
    }

    this.salvando.set(true);
    this.service.alterarStatus(id, passo.status).subscribe({
      next: os => {
        this.aplicar(os);
        this.salvando.set(false);
        this.notificar(`OS marcada como ${ROTULOS_STATUS[passo.status].toLowerCase()}`);
      },
      error: (e: HttpErrorResponse) => {
        this.salvando.set(false);
        this.notificar(mensagemDoErro(e), true);
      }
    });
  }

  // ── Peças ─────────────────────────────────────────────────

  adicionarPeca(): void {
    const id = this.os()?.id;
    if (!id || this.salvando() || this.somenteLeitura()) return;

    const desc = this.pDescricao().trim();
    if (!desc) {
      this.pErro.set('Informe a descrição da peça');
      return;
    }
    if (desc.length > 200) {
      this.pErro.set('A descrição deve ter no máximo 200 caracteres');
      return;
    }
    // Digitada pela pessoa: aceita "2", "1,5" ou "1.5" (no máximo 2 casas, como no banco).
    const qtdDigitada = this.pQuantidade().trim();
    if (!qtdDigitada) {
      this.pErro.set('Informe a quantidade');
      return;
    }
    if (!/^\d+([.,]\d{1,2})?$/.test(qtdDigitada)) {
      this.pErro.set('Quantidade inválida. Use números, como 2 ou 1,5');
      return;
    }
    const quantidade = Number(qtdDigitada.replace(',', '.'));
    if (quantidade <= 0) {
      this.pErro.set('Informe uma quantidade maior que zero');
      return;
    }
    // Valor negativo é permitido: é assim que se lança o "Crédito mecânico".
    if (this.pValor() === null || !Number.isFinite(this.pValor())) {
      this.pErro.set('Informe o valor unitário (use valor negativo para crédito)');
      return;
    }
    this.pErro.set(null);

    const item: ItemPeca = {
      descricao: desc,
      quantidade,
      valorUnitario: this.pValor()!,
      pagoPeloCliente: this.pPagoCliente()
    };

    const editando = this.editandoPecaId();
    this.salvando.set(true);
    const pedido = editando
      ? this.service.atualizarPeca(id, editando, item)
      : this.service.adicionarPeca(id, item);
    pedido.subscribe({
      next: os => {
        this.aplicar(os, false);
        this.limparFormPeca();
        this.salvando.set(false);
        this.notificar(editando ? 'Peça corrigida' : 'Peça adicionada');
      },
      error: (e: HttpErrorResponse) => {
        this.salvando.set(false);
        this.notificar(mensagemDoErro(e), true);
      }
    });
  }

  removerPeca(item: ItemPeca): void {
    const id = this.os()?.id;
    if (!id || !item.id || this.salvando() || this.somenteLeitura()) return;
    if (!confirm(`Remover "${item.descricao}"?`)) return;

    this.salvando.set(true);
    this.service.removerPeca(id, item.id).subscribe({
      next: os => {
        this.aplicar(os, false);
        this.salvando.set(false);
        this.notificar('Peça removida');
      },
      error: (e: HttpErrorResponse) => {
        this.salvando.set(false);
        this.notificar(mensagemDoErro(e), true);
      }
    });
  }

  /** Leva a peça para o formulário de baixo, para corrigir. */
  editarPeca(item: ItemPeca): void {
    if (!item.id || this.salvando() || this.somenteLeitura()) return;
    this.editandoPecaId.set(item.id);
    this.pDescricao.set(item.descricao);
    this.pQuantidade.set(String(item.quantidade ?? 1).replace('.', ','));
    this.pValor.set(item.valorUnitario);
    this.pPagoCliente.set(!!item.pagoPeloCliente);
    this.pErro.set(null);
    queueMicrotask(() => document.getElementById('pdesc')?.focus());
  }

  cancelarEdicaoPeca(): void {
    this.limparFormPeca();
  }

  private limparFormPeca(): void {
    this.editandoPecaId.set(null);
    this.pDescricao.set('');
    this.pQuantidade.set('');
    this.pValor.set(null);
    this.pPagoCliente.set(false);
    this.pErro.set(null);
  }

  // ── Serviços ──────────────────────────────────────────────

  adicionarServico(): void {
    const id = this.os()?.id;
    if (!id || this.salvando() || this.somenteLeitura()) return;

    const desc = this.sDescricao().trim();
    if (!desc) {
      this.sErro.set('Informe a descrição do serviço');
      return;
    }
    if (desc.length > 200) {
      this.sErro.set('A descrição deve ter no máximo 200 caracteres');
      return;
    }
    if (this.sValor() === null || !Number.isFinite(this.sValor()) || this.sValor()! < 0) {
      this.sErro.set('Informe um valor igual ou maior que zero');
      return;
    }
    this.sErro.set(null);

    const item: ItemServico = { descricao: desc, valor: this.sValor()! };

    const editando = this.editandoServicoId();
    this.salvando.set(true);
    const pedido = editando
      ? this.service.atualizarServico(id, editando, item)
      : this.service.adicionarServico(id, item);
    pedido.subscribe({
      next: os => {
        this.aplicar(os, false);
        this.limparFormServico();
        this.salvando.set(false);
        this.notificar(editando ? 'Serviço corrigido' : 'Serviço adicionado');
      },
      error: (e: HttpErrorResponse) => {
        this.salvando.set(false);
        this.notificar(mensagemDoErro(e), true);
      }
    });
  }

  editarServico(item: ItemServico): void {
    if (!item.id || this.salvando() || this.somenteLeitura()) return;
    this.editandoServicoId.set(item.id);
    this.sDescricao.set(item.descricao);
    this.sValor.set(item.valor);
    this.sErro.set(null);
    queueMicrotask(() => document.getElementById('sdesc')?.focus());
  }

  cancelarEdicaoServico(): void {
    this.limparFormServico();
  }

  private limparFormServico(): void {
    this.editandoServicoId.set(null);
    this.sDescricao.set('');
    this.sValor.set(null);
    this.sErro.set(null);
  }

  removerServico(item: ItemServico): void {
    const id = this.os()?.id;
    if (!id || !item.id || this.salvando() || this.somenteLeitura()) return;
    if (!confirm(`Remover "${item.descricao}"?`)) return;

    this.salvando.set(true);
    this.service.removerServico(id, item.id).subscribe({
      next: os => {
        this.aplicar(os, false);
        this.salvando.set(false);
        this.notificar('Serviço removido');
      },
      error: (e: HttpErrorResponse) => {
        this.salvando.set(false);
        this.notificar(mensagemDoErro(e), true);
      }
    });
  }

  // ── PDF ───────────────────────────────────────────────────

  baixarPdf(): void {
    const id = this.os()?.id;
    if (!id || this.salvando() || this.gerandoPdf()) return;
    if (this.temAlteracoes() && !this.somenteLeitura()) {
      this.notificar('Salve as alterações antes de baixar o recibo.', true);
      return;
    }

    this.gerandoPdf.set(true);
    this.notificar('Gerando recibo…');
    this.service.gerarPdf(id).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `recibo_os_${id}.pdf`;
        a.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
        this.gerandoPdf.set(false);
      },
      error: () => {
        this.gerandoPdf.set(false);
        this.notificar('Não foi possível gerar o recibo. Tente novamente.', true);
      }
    });
  }

  // ── Apoio ─────────────────────────────────────────────────

  classeBadge(status?: StatusOs): string {
    return badgeDoStatus(status);
  }

  rotulo(status?: StatusOs): string {
    return status ? ROTULOS_STATUS[status] : '—';
  }

  voltar(): void {
    this.router.navigate(['/ordens']);
  }

  podeSair(): boolean {
    if (this.salvando()) {
      this.notificar('Aguarde a operação terminar antes de sair.', true);
      return false;
    }
    return !this.temAlteracoes() || this.somenteLeitura() ||
      confirm('Há alterações não salvas. Sair e descartar essas alterações?');
  }

  @HostListener('window:beforeunload', ['$event'])
  avisarAoFechar(event: BeforeUnloadEvent): void {
    if (this.salvando() || (this.temAlteracoes() && !this.somenteLeitura())) {
      event.preventDefault();
      event.returnValue = '';
    }
  }

  private notificar(texto: string, erro = false): void {
    const id = Date.now() + Math.random();
    this.toasts.update(ts => [...ts, { id, texto, erro }]);
    setTimeout(
      () => this.toasts.update(ts => ts.filter(t => t.id !== id)),
      erro ? 6000 : 3000
    );
  }
}
