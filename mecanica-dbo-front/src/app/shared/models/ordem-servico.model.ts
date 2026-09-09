export type StatusOs = 'ABERTA' | 'EM_ANDAMENTO' | 'CONCLUIDA' | 'ENTREGUE';

export interface ItemPeca {
  id?: number;
  descricao: string;
  quantidade: number;
  valorUnitario: number;
  valorTotal?: number;       // calculado pelo banco — somente leitura
  pagoPeloCliente?: boolean;
  criadoEm?: string;
}

export interface ItemServico {
  id?: number;
  descricao: string;
  valor: number;
  criadoEm?: string;
}

/**
 * OrdemServico é serializada sem veiculo, itensPeca, itensServico e notasFiscais
 * (todos anotados com @JsonIgnore para evitar ciclo de serialização).
 * Para montar a tela de detalhe, use chamadas separadas ou um DTO de resposta
 * no backend — a segunda opção é a recomendada.
 *
 * Os totais (totalPecas, totalServicos, totalGeral) são recalculados por
 * trigger no PostgreSQL a cada peça ou serviço inserido. O front nunca os
 * envia — apenas relê a OS após adicionar ou remover um item.
 */
export interface OrdemServico {
  id?: number;
  status: StatusOs;
  kmEntrada?: number;
  dataEntrada?: string;        // ISO yyyy-MM-dd
  dataSaidaPrevista?: string;
  dataSaidaReal?: string;      // preenchida pelo backend ao marcar ENTREGUE
  reclamacoes?: string;        // queixas do cliente
  diagnostico?: string;        // observações do mecânico
  totalPecas?: number;         // recalculado por trigger
  totalServicos?: number;
  totalGeral?: number;
  criadoEm?: string;
}
