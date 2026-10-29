/** Valores aceitos pelo banco — use exatamente esses no select */
export type Combustivel =
  | 'GASOLINA'
  | 'ETANOL'
  | 'FLEX'
  | 'DIESEL'
  | 'ELETRICO'
  | 'HIBRIDO'
  | 'GNV';

/**
 * As consultas retornam VeiculoResumoDTO, incluindo o cliente aninhado.
 * Os campos de cadastro são enviados sem esse relacionamento.
 */
export interface Veiculo {
  id?: number;
  placa: string;
  marca: string;
  modelo: string;
  cor: string;
  combustivel: Combustivel;
  chassi?: string;
  anoFabricacao?: number;
  ativo?: boolean;
  criadoEm?: string;
  cliente?: { id: number; nome: string; telefone1: string; cpf?: string };
}
