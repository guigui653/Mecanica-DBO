/** Valores aceitos pelo banco — use exatamente esses no select */
export type Combustivel =
  | 'GASOLINA'
  | 'ETANOL'
  | 'FLEX'
  | 'DIESEL'
  | 'ELETRICO'
  | 'GNV';

/**
 * O backend serializa Veiculo sem o objeto cliente (marcado com @JsonIgnore
 * para evitar referência circular). Para saber de quem é o veículo,
 * navegue a partir de GET /clientes/{id}/veiculos.
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
}
