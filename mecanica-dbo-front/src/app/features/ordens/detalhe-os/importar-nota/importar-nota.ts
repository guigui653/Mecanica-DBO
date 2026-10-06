import { Component, computed, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { OcrService, OcrItem, OcrMetadados } from '../../../../core/services/ocr';
import { OrdemServicoService } from '../../../../core/services/ordem-servico';
import { OrdemServico } from '../../../../shared/models/ordem-servico.model';

type Fase = 'inicio' | 'lendo' | 'revisao';

/** Uma linha da tabela de revisão. O id local só serve para o @for não perder o foco ao digitar. */
interface Linha {
  uid: number;
  selecionada: boolean;
  descricao: string;
  quantidade: number;
  valorUnitario: number | null;
  pagoPeloCliente: boolean;
}

const TIPOS = ['image/jpeg', 'image/png', 'image/webp', 'image/bmp'];
const LIMITE_BYTES = 10 * 1024 * 1024;

/**
 * Importa peças de uma foto de nota fiscal.
 *
 * Fluxo: escolher foto -> OCR sugere itens -> o mecânico CONFERE e corrige ->
 * só então as peças são lançadas, uma a uma, no mesmo endpoint da tela manual
 * (POST /ordens/{id}/pecas). O OCR erra (5 vira S, vírgula some), por isso nada
 * é gravado sem essa conferência.
 */
@Component({
  selector: 'app-importar-nota',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './importar-nota.html',
  styleUrl: './importar-nota.css'
})
export class ImportarNota {

  private ocr       = inject(OcrService);
  private osService = inject(OrdemServicoService);

  /** Id da OS que recebe as peças. */
  osId = input.required<number>();

  /** Emite a OS já atualizada (com totais recalculados) e quantas peças entraram. */
  importado = output<{ os: OrdemServico; quantidade: number }>();

  aberto    = signal(false);
  fase      = signal<Fase>('inicio');
  erro      = signal<string | null>(null);
  ocupado   = signal(false);

  nomeArquivo = signal('');
  metadados   = signal<OcrMetadados | null>(null);
  textoBruto  = signal('');
  linhas      = signal<Linha[]>([]);

  private contador = 0;

  selecionadas = computed(() => this.linhas().filter(l => l.selecionada));

  totalSelecionado = computed(() =>
    this.selecionadas().reduce((s, l) => s + (l.valorUnitario ?? 0) * (l.quantidade || 1), 0)
  );

  /** Diferença entre o que vai ser lançado e o total impresso na nota. Ajuda a achar item perdido ou lido errado. */
  diferencaComNota = computed(() => {
    const totalNf = this.metadados()?.valorTotalNf;
    if (totalNf == null) return null;
    const dif = Math.round((this.totalSelecionado() - totalNf) * 100) / 100;
    return dif === 0 ? null : dif;
  });

  // ── abrir / fechar ────────────────────────────────────────

  alternar(): void {
    if (this.ocupado()) return;
    this.aberto.update(v => !v);
    if (!this.aberto()) this.reiniciar();
  }

  reiniciar(): void {
    this.fase.set('inicio');
    this.erro.set(null);
    this.nomeArquivo.set('');
    this.metadados.set(null);
    this.textoBruto.set('');
    this.linhas.set([]);
  }

  // ── 1. foto -> OCR ────────────────────────────────────────

  aoEscolherArquivo(evento: Event): void {
    const campo = evento.target as HTMLInputElement;
    const arquivo = campo.files?.[0];
    campo.value = '';                      // permite escolher a mesma foto de novo
    if (!arquivo) return;

    if (!TIPOS.includes(arquivo.type)) {
      this.erro.set('Formato não aceito. Envie a nota como foto JPG, PNG ou WEBP.');
      return;
    }
    if (arquivo.size > LIMITE_BYTES) {
      this.erro.set('A foto passa de 10 MB. Tire de novo com qualidade menor.');
      return;
    }

    this.erro.set(null);
    this.nomeArquivo.set(arquivo.name);
    this.fase.set('lendo');

    this.ocr.lerNota(arquivo).subscribe({
      next: r => {
        this.metadados.set(r.metadados ?? null);
        this.textoBruto.set(r.textoBruto ?? '');
        this.linhas.set((r.itens ?? []).map(i => this.paraLinha(i)));
        this.fase.set('revisao');
      },
      error: (e: HttpErrorResponse) => {
        this.fase.set('inicio');
        this.erro.set(this.mensagemDoErro(e));
      }
    });
  }

  private paraLinha(i: OcrItem): Linha {
    return {
      uid: ++this.contador,
      selecionada: true,
      descricao: i.descricao,
      quantidade: i.quantidade || 1,
      valorUnitario: i.valorUnitario,
      pagoPeloCliente: false
    };
  }

  // ── 2. revisão ────────────────────────────────────────────

  editar(linha: Linha, mudanca: Partial<Linha>): void {
    this.linhas.update(ls => ls.map(l => (l === linha ? { ...l, ...mudanca } : l)));
  }

  descartar(linha: Linha): void {
    this.linhas.update(ls => ls.filter(l => l !== linha));
  }

  adicionarLinhaVazia(): void {
    this.linhas.update(ls => [...ls, {
      uid: ++this.contador, selecionada: true, descricao: '',
      quantidade: 1, valorUnitario: null, pagoPeloCliente: false
    }]);
  }

  // ── 3. lançar as peças ────────────────────────────────────

  lancar(): void {
    if (this.ocupado()) return;

    const fila = this.selecionadas();
    if (fila.length === 0) {
      this.erro.set('Marque ao menos um item para lançar.');
      return;
    }
    const ruim = fila.find(l => !l.descricao.trim() || l.valorUnitario === null);
    if (ruim) {
      this.erro.set('Há itens marcados sem descrição ou sem valor. Complete ou desmarque.');
      return;
    }

    this.erro.set(null);
    this.ocupado.set(true);
    this.lancarUm(fila, null, 0);
  }

  /**
   * Lança em sequência, parando no primeiro erro. A linha que entrou sai da
   * tabela — assim, se algo falhar no meio, tentar de novo não duplica peça.
   */
  private lancarUm(fila: Linha[], ultimaOs: OrdemServico | null, feitas: number): void {
    if (fila.length === 0) {
      this.ocupado.set(false);
      this.concluir(ultimaOs, feitas);
      return;
    }

    const [linha, ...resto] = fila;
    this.osService.adicionarPeca(this.osId(), {
      descricao: linha.descricao.trim(),
      quantidade: linha.quantidade || 1,
      valorUnitario: linha.valorUnitario!,
      pagoPeloCliente: linha.pagoPeloCliente
    }).subscribe({
      next: os => {
        this.linhas.update(ls => ls.filter(l => l !== linha));
        this.lancarUm(resto, os, feitas + 1);
      },
      error: (e: HttpErrorResponse) => {
        this.ocupado.set(false);
        const lancadas = feitas > 0 ? `${feitas} peça(s) já foram lançadas. ` : '';
        this.erro.set(`${lancadas}Parou em "${linha.descricao}": ${this.mensagemDoErro(e)}`);
        if (ultimaOs) this.importado.emit({ os: ultimaOs, quantidade: feitas });
      }
    });
  }

  private concluir(os: OrdemServico | null, quantidade: number): void {
    if (os) this.importado.emit({ os, quantidade });
    this.aberto.set(false);
    this.reiniciar();
  }

  // ── apoio ─────────────────────────────────────────────────

  private mensagemDoErro(e: HttpErrorResponse): string {
    if (e.error?.mensagem) return e.error.mensagem;
    if (e.status === 0)    return 'Sem conexão com a API.';
    return 'Ocorreu um erro inesperado.';
  }
}
