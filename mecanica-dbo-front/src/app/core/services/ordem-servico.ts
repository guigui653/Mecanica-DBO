import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { OrdemServico, ItemPeca, ItemServico, StatusOs } from '../../shared/models/ordem-servico.model';

@Injectable({ providedIn: 'root' })
export class OrdemServicoService {
  private http = inject(HttpClient);
  private base = '/ordens';

  /** Lista com filtro opcional por status e termo */
  listar(status?: StatusOs, q?: string): Observable<OrdemServico[]> {
    if (!q && status === 'ABERTA') {
      return this.listarAbertas();
    }
    return this.buscar(q).pipe(
      map(ordens => status ? ordens.filter(o => o.status === status) : ordens)
    );
  }

  /** GET /ordens?q= — busca por cliente ou placa */
  buscar(q?: string): Observable<OrdemServico[]> {
    const params = q ? new HttpParams().set('q', q) : undefined;
    return this.http.get<OrdemServico[]>(this.base, { params });
  }

  /** GET /ordens/abertas — OS com status ABERTA */
  listarAbertas(): Observable<OrdemServico[]> {
    return this.http.get<OrdemServico[]>(`${this.base}/abertas`);
  }

  /**
   * GET /ordens/periodo?inicio=&fim=
   * Datas no formato ISO yyyy-MM-dd
   */
  listarPorPeriodo(inicio: string, fim: string): Observable<OrdemServico[]> {
    const params = new HttpParams().set('inicio', inicio).set('fim', fim);
    return this.http.get<OrdemServico[]>(`${this.base}/periodo`, { params });
  }

  /** GET /ordens/{id} */
  buscarPorId(id: number): Observable<OrdemServico> {
    return this.http.get<OrdemServico>(`${this.base}/${id}`);
  }

  /** GET /ordens/veiculo/{veiculoId} — OS anteriores de um veículo */
  listarPorVeiculo(veiculoId: number): Observable<OrdemServico[]> {
    return this.http.get<OrdemServico[]>(`${this.base}/veiculo/${veiculoId}`);
  }

  /**
   * POST /ordens/veiculo/{veiculoId} — 201 Created
   * A OS é sempre criada vinculada a um veículo já cadastrado.
   */
  criar(veiculoId: number, os: Partial<OrdemServico>): Observable<OrdemServico> {
    return this.http.post<OrdemServico>(`${this.base}/veiculo/${veiculoId}`, os);
  }

  /**
   * PATCH /ordens/{id}/status?status=
   * O status vai como query param, não no corpo.
   * Regra: ENTREGUE só é aceito se a OS estiver CONCLUIDA (backend valida com 409).
   * Ao marcar ENTREGUE, o backend preenche dataSaidaReal automaticamente.
   */
  alterarStatus(id: number, status: StatusOs): Observable<OrdemServico> {
    return this.http.patch<OrdemServico>(
      `${this.base}/${id}/status`,
      null,
      { params: { status } }
    );
  }

  /** POST /ordens/{osId}/pecas — 201 Created */
  adicionarPeca(osId: number, item: ItemPeca): Observable<ItemPeca> {
    return this.http.post<ItemPeca>(`${this.base}/${osId}/pecas`, item);
  }

  /** DELETE /ordens/{osId}/pecas/{itemId} — 204 No Content */
  removerPeca(osId: number, itemId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${osId}/pecas/${itemId}`);
  }

  /** POST /ordens/{osId}/servicos — 201 Created */
  adicionarServico(osId: number, item: ItemServico): Observable<ItemServico> {
    return this.http.post<ItemServico>(`${this.base}/${osId}/servicos`, item);
  }

  /** DELETE /ordens/{osId}/servicos/{itemId} — 204 No Content */
  removerServico(osId: number, itemId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${osId}/servicos/${itemId}`);
  }

  /** GET /ordens/{osId}/pdf — retorna Blob (application/pdf) */
  gerarPdf(osId: number): Observable<Blob> {
    return this.http.get(`${this.base}/${osId}/pdf`, { responseType: 'blob' });
  }
}
