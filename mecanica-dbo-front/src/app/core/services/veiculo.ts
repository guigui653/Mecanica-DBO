import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Veiculo } from '../../shared/models/veiculo.model';

@Injectable({ providedIn: 'root' })
export class VeiculoService {
  private http = inject(HttpClient);
  private base = '/veiculos';

  /**
   * NÃO há GET /veiculos genérico na API.
   * A busca parte sempre da placa (buscarPorPlaca) ou do cliente (listarPorCliente).
   */

  /** GET /veiculos/placa/{placa} */
  buscarPorPlaca(placa: string): Observable<Veiculo> {
    return this.http.get<Veiculo>(`${this.base}/placa/${placa}`);
  }

  /** GET /clientes/{clienteId}/veiculos */
  listarPorCliente(clienteId: number): Observable<Veiculo[]> {
    return this.http.get<Veiculo[]>(`/clientes/${clienteId}/veiculos`);
  }

  /** GET /veiculos/{id} */
  buscarPorId(id: number): Observable<Veiculo> {
    return this.http.get<Veiculo>(`${this.base}/${id}`);
  }

  /**
   * POST /veiculos/cliente/{clienteId}
   * Todo veículo precisa ser vinculado a um cliente na criação.
   */
  criar(clienteId: number, veiculo: Veiculo): Observable<Veiculo> {
    return this.http.post<Veiculo>(`${this.base}/cliente/${clienteId}`, veiculo);
  }

  /** PUT /veiculos/{id} */
  atualizar(id: number, veiculo: Veiculo): Observable<Veiculo> {
    return this.http.put<Veiculo>(`${this.base}/${id}`, veiculo);
  }

  /** DELETE /veiculos/{id} */
  inativar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
