export interface Cliente {
  id?: number;
  nome: string;
  cpf?: string;
  telefone1: string;
  telefone2?: string;
  email?: string;
  ativo?: boolean;
  criadoEm?: string;
}
