import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Cliente } from '../../shared/models/cliente.model';
import { Veiculo } from '../../shared/models/veiculo.model';
import { OrdemServico, VeiculoResumo } from '../../shared/models/ordem-servico.model';

/** Dados da OS na abertura (espelha o CriarOsRequest do backend). */
export interface DadosAbertura {
  kmEntrada?: number;
  dataSaidaPrevista?: string;
  reclamacoes: string;
}

/**
 * Espelha o AtendimentoRequest. Combinações aceitas:
 *  - veiculoId + os
 *  - clienteId + novoVeiculo [+ os]
 *  - novoCliente + novoVeiculo [+ os]
 */
export interface AtendimentoRequest {
  clienteId?: number;
  novoCliente?: Cliente;
  veiculoId?: number;
  novoVeiculo?: Veiculo;
  os?: DadosAbertura;
}

export interface AtendimentoResposta {
  veiculo: VeiculoResumo;
  /** Só vem quando a OS foi aberta. */
  os?: OrdemServico;
}

@Injectable({ providedIn: 'root' })
export class AtendimentoService {
  private http = inject(HttpClient);

  /**
   * POST /atendimentos — grava tudo numa transação só no backend.
   * Se qualquer parte falhar (placa repetida, CPF repetido…), nada é gravado.
   */
  registrar(pedido: AtendimentoRequest): Observable<AtendimentoResposta> {
    return this.http.post<AtendimentoResposta>('/atendimentos', pedido);
  }
}
