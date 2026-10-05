export type StatusOs = 'ABERTA' | 'EM_ANDAMENTO' | 'CONCLUIDA' | 'ENTREGUE';

/** Espelha o ClienteResumoDTO do backend — o que vem aninhado no veículo. */
export interface ClienteResumo {
  id: number;
  nome: string;
  cpf?: string;
  telefone1: string;
}

export interface VeiculoResumo {
  id: number;
  placa: string;
  marca: string;
  modelo: string;
  cor: string;
  combustivel: string;
  chassi?: string;
  anoFabricacao?: number;
  cliente?: ClienteResumo;
}

export interface ItemPeca {
  id?: number;
  descricao: string;
  quantidade: number;
  valorUnitario: number;
  /** Coluna gerada pelo banco (quantidade x valorUnitario) — somente leitura. */
  valorTotal?: number;
  pagoPeloCliente?: boolean;
}

export interface ItemServico {
  id?: number;
  descricao: string;
  valor: number;
}

export interface OrdemServico {
  id?: number;
  status: StatusOs;
  kmEntrada?: number;
  dataEntrada?: string;
  dataSaidaPrevista?: string;
  dataSaidaReal?: string;
  reclamacoes?: string;
  diagnostico?: string;
  /** Recalculados por trigger no PostgreSQL — o front nunca envia. */
  totalPecas?: number;
  totalServicos?: number;
  totalGeral?: number;
  veiculo?: VeiculoResumo;
  itensPeca?: ItemPeca[];
  itensServico?: ItemServico[];
}

/** Campos que o PUT /ordens/{id} aceita. */
export interface AtualizarOsRequest {
  kmEntrada?: number | null;
  dataSaidaPrevista?: string | null;
  reclamacoes?: string | null;
  diagnostico?: string | null;
}

export const ROTULOS_STATUS: Record<StatusOs, string> = {
  ABERTA: 'Aberta',
  EM_ANDAMENTO: 'Em andamento',
  CONCLUIDA: 'Concluída',
  ENTREGUE: 'Entregue'
};

/**
 * Próximo passo do fluxo. O backend só impõe uma regra — ENTREGUE exige
 * CONCLUIDA — mas avançar um passo por vez é o que o mecânico faz na prática.
 */
export const PROXIMO_STATUS: Partial<Record<StatusOs, { status: StatusOs; rotulo: string }>> = {
  ABERTA:       { status: 'EM_ANDAMENTO', rotulo: 'Iniciar serviço' },
  EM_ANDAMENTO: { status: 'CONCLUIDA',    rotulo: 'Concluir serviço' },
  CONCLUIDA:    { status: 'ENTREGUE',     rotulo: 'Entregar ao cliente' }
};
