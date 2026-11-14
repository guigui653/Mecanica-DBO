import { HttpErrorResponse } from '@angular/common/http';
import { mensagemDoErro } from './mensagem-erro';

describe('mensagemDoErro', () => {
  const resposta = (status: number, error: unknown) => new HttpErrorResponse({ status, error });

  it('usa a mensagem do backend', () => {
    expect(mensagemDoErro(resposta(409, { status: 409, mensagem: 'Placa já cadastrada: JEH-3030' })))
      .toBe('Placa já cadastrada: JEH-3030');
  });
  it('junta as mensagens dos campos de validação', () => {
    expect(mensagemDoErro(resposta(400, { mensagem: 'Dados inválidos', campos: { nome: 'Nome é obrigatório', cor: 'Cor é obrigatória' } })))
      .toBe('Nome é obrigatório · Cor é obrigatória');
  });
  it('avisa quando não há conexão', () => {
    expect(mensagemDoErro(resposta(0, null))).toContain('Sem conexão');
  });
  it('cai no texto padrão quando não há mensagem', () => {
    expect(mensagemDoErro(resposta(500, null))).toContain('erro inesperado');
    expect(mensagemDoErro(resposta(500, null), 'Falhou o PDF.')).toBe('Falhou o PDF.');
    expect(mensagemDoErro(undefined)).toContain('erro inesperado');
  });
});
