import { HttpErrorResponse } from '@angular/common/http';

/**
 * Texto para mostrar ao usuário a partir de um erro da API.
 *
 * O backend sempre responde { timestamp, status, mensagem, campos }:
 *  - campos (validação) → as mensagens de cada campo, juntas;
 *  - mensagem           → o texto que o backend já escreveu em português;
 *  - status 0           → o navegador nem chegou no servidor.
 */
export function mensagemDoErro(e: unknown, padrao = 'Ocorreu um erro inesperado. Tente novamente.'): string {
  const erro = e as HttpErrorResponse | null | undefined;
  if (erro?.status === 0) return 'Sem conexão com o servidor. Verifique se o sistema está ligado.';

  const corpo = erro?.error;
  if (corpo?.campos && Object.keys(corpo.campos).length > 0) {
    return Object.values(corpo.campos).join(' · ');
  }
  if (typeof corpo?.mensagem === 'string' && corpo.mensagem) return corpo.mensagem;
  return padrao;
}
