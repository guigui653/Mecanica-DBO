import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Cliente } from '../../shared/models/cliente.model';
import { Veiculo } from '../../shared/models/veiculo.model';

@Injectable({ providedIn: 'root' })
export class ClienteService {
  private http = inject(HttpClient);
  private base = '/clientes';

  /** GET /clientes?q= — lista ou filtra por nome, CPF ou telefone */
  listar(q?: string): Observable<Cliente[]> {
    const params = q ? new HttpParams().set('q', q) : undefined;
    return this.http.get<Cliente[]>(this.base, { params });
  }

  /** GET /clientes/{id} */
  buscarPorId(id: number): Observable<Cliente> {
    return this.http.get<Cliente>(`${this.base}/${id}`);
  }

  /** GET /clientes/{id}/veiculos */
  listarVeiculos(id: number): Observable<Veiculo[]> {
    return this.http.get<Veiculo[]>(`${this.base}/${id}/veiculos`);
  }

  /** POST /clientes — 201 Created */
  criar(cliente: Cliente): Observable<Cliente> {
    return this.http.post<Cliente>(this.base, cliente);
  }

  /** PUT /clientes/{id} */
  atualizar(id: number, cliente: Cliente): Observable<Cliente> {
    return this.http.put<Cliente>(`${this.base}/${id}`, cliente);
  }

  /** DELETE /clientes/{id} — 204 No Content */
  inativar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
