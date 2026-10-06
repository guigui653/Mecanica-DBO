import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Espelha OcrResposta do backend. São sugestões: nada disto é gravado até o mecânico confirmar. */
export interface OcrItem {
  descricao: string;
  quantidade: number;
  valorUnitario: number;
  valorTotal: number;
}

export interface OcrMetadados {
  fornecedor?: string;
  cnpjFornecedor?: string;
  numeroNf?: string;
  dataEmissao?: string;      // AAAA-MM-DD
  valorTotalNf?: number;
}

export interface OcrResposta {
  sucesso: boolean;
  metadados: OcrMetadados | null;
  itens: OcrItem[];
  textoBruto?: string;
  mensagem?: string;
}

@Injectable({ providedIn: 'root' })
export class OcrService {

  private http = inject(HttpClient);

  /**
   * Envia a foto como multipart (campo "file"). O Content-Type com o boundary
   * é montado pelo navegador — não defina esse header à mão.
   */
  lerNota(arquivo: File): Observable<OcrResposta> {
    const form = new FormData();
    form.append('file', arquivo);
    return this.http.post<OcrResposta>('/ocr/nota', form);
  }

  status(): Observable<{ disponivel: boolean }> {
    return this.http.get<{ disponivel: boolean }>('/ocr/status');
  }
}
