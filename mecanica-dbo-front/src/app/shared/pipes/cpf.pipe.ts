import { Pipe, PipeTransform } from '@angular/core';

/**
 * Documento do cliente: CPF (pessoa física) ou CNPJ (empresa).
 * Mesmas regras do Documentos.java no backend (Receita Federal), inclusive
 * o CNPJ alfanumérico (letras nos 12 primeiros caracteres, desde jul/2026).
 *
 * O backend entrega o documento limpo (só números e letras maiúsculas).
 * Na tela: CPF mascarado ("***.456.789-**"), CNPJ completo (é dado público).
 */

export type TipoDocumento = 'CPF' | 'CNPJ';

/** " 12.abc.345/01de-35 " → "12ABC34501DE35". */
export function limparDocumento(doc: string | null | undefined): string {
  return (doc ?? '').toUpperCase().replace(/[^0-9A-Z]/g, '');
}

/** CPF, CNPJ ou null (documento incompleto ou com formato estranho). */
export function tipoDocumento(doc: string | null | undefined): TipoDocumento | null {
  const d = limparDocumento(doc);
  if (/^\d{11}$/.test(d)) return 'CPF';
  if (/^[0-9A-Z]{12}\d{2}$/.test(d)) return 'CNPJ';
  return null;
}

function valor(c: string): number {
  return c.charCodeAt(0) - 48;   // "0" = 0 ... "9" = 9, "A" = 17 ... "Z" = 42
}

function dv(resto: number): number {
  return resto < 2 ? 0 : 11 - resto;
}

export function cpfValido(doc: string | null | undefined): boolean {
  const d = limparDocumento(doc);
  if (!/^\d{11}$/.test(d) || /^(\d)\1{10}$/.test(d)) return false;
  for (const pos of [9, 10]) {
    let soma = 0;
    for (let i = 0; i < pos; i++) soma += valor(d[i]) * (pos + 1 - i);
    if (dv(soma % 11) !== valor(d[pos])) return false;
  }
  return true;
}

export function cnpjValido(doc: string | null | undefined): boolean {
  const d = limparDocumento(doc);
  if (!/^[0-9A-Z]{12}\d{2}$/.test(d) || /^(.)\1{13}$/.test(d)) return false;
  for (const pos of [12, 13]) {
    let soma = 0;
    let peso = 2;
    for (let i = pos - 1; i >= 0; i--) {
      soma += valor(d[i]) * peso;
      peso = peso === 9 ? 2 : peso + 1;
    }
    if (dv(soma % 11) !== valor(d[pos])) return false;
  }
  return true;
}

/**
 * Confere o documento digitado. Devolve a mensagem de erro, ou null se
 * estiver certo ou vazio (o documento é opcional).
 */
export function erroDocumento(doc: string | null | undefined): string | null {
  const d = limparDocumento(doc);
  if (!d) return null;
  const tipo = tipoDocumento(d);
  if (!tipo) return 'Use o CPF (11 números) ou o CNPJ (14 caracteres).';
  if (tipo === 'CPF' && !cpfValido(d)) return 'CPF inválido: confira os números.';
  if (tipo === 'CNPJ' && !cnpjValido(d)) return 'CNPJ inválido: confira os caracteres.';
  return null;
}

/** Formata enquanto a pessoa digita: até 11 números → CPF; mais que isso (ou com letra) → CNPJ. */
export function formatarDocumentoDigitando(texto: string): string {
  const d = limparDocumento(texto).slice(0, 14);
  const pareceCpf = d.length <= 11 && /^\d*$/.test(d);
  if (pareceCpf) {
    return d.replace(/^(\d{3})(\d)/, '$1.$2')
            .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
            .replace(/\.(\d{3})(\d{1,2})$/, '.$1-$2');
  }
  let r = d.slice(0, 2);
  if (d.length > 2) r += '.' + d.slice(2, 5);
  if (d.length > 5) r += '.' + d.slice(5, 8);
  if (d.length > 8) r += '/' + d.slice(8, 12);
  if (d.length > 12) r += '-' + d.slice(12);
  return r;
}

/** Campo de CPF: só números, até 11, como 000.000.000-00. */
export function formatarCpfDigitando(texto: string): string {
  return formatarDocumentoDigitando(texto.replace(/\D/g, '').slice(0, 11));
}

/** Campo de CNPJ: números e letras (CNPJ alfanumérico), até 14, como 00.000.000/0000-00. */
export function formatarCnpjDigitando(texto: string): string {
  const d = limparDocumento(texto).slice(0, 14);
  let r = d.slice(0, 2);
  if (d.length > 2) r += '.' + d.slice(2, 5);
  if (d.length > 5) r += '.' + d.slice(5, 8);
  if (d.length > 8) r += '/' + d.slice(8, 12);
  if (d.length > 12) r += '-' + d.slice(12);
  return r;
}

/**
 * Formulário com um campo de CPF e outro de CNPJ: confere e devolve o
 * documento a gravar (ou a mensagem de erro). Os dois vazios = sem documento.
 */
export function documentoDosCampos(cpf: string, cnpj: string): { doc?: string; erro?: string } {
  const c = limparDocumento(cpf);
  const j = limparDocumento(cnpj);
  if (c && j) return { erro: 'Preencha só o CPF (pessoa física) ou só o CNPJ (empresa), não os dois.' };
  if (c) {
    if (c.length !== 11) return { erro: 'CPF incompleto: são 11 números.' };
    if (!cpfValido(c)) return { erro: 'CPF inválido: confira os números.' };
    return { doc: c };
  }
  if (j) {
    if (j.length !== 14) return { erro: 'CNPJ incompleto: são 14 caracteres.' };
    if (!cnpjValido(j)) return { erro: 'CNPJ inválido: confira os caracteres.' };
    return { doc: j };
  }
  return {};
}

/** Documento completo, formatado: "529.982.247-25" ou "12.ABC.345/01DE-35". */
export function formatarCpf(doc: string | null | undefined): string {
  const d = limparDocumento(doc);
  const tipo = tipoDocumento(d);
  if (tipo === 'CPF') return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9)}`;
  if (tipo === 'CNPJ') return `${d.slice(0, 2)}.${d.slice(2, 5)}.${d.slice(5, 8)}/${d.slice(8, 12)}-${d.slice(12)}`;
  return doc ?? '';
}

/** Para mostrar: CPF mascarado, CNPJ completo. */
export function mascararCpf(doc: string | null | undefined): string {
  const d = limparDocumento(doc);
  if (!d) return '';
  const tipo = tipoDocumento(d);
  if (tipo === 'CPF') return `***.${d.slice(3, 6)}.${d.slice(6, 9)}-**`;
  if (tipo === 'CNPJ') return formatarCpf(d);
  return '***';
}

/** "CPF", "CNPJ" ou "CPF/CNPJ" (quando não dá para saber). */
export function rotuloDocumento(doc: string | null | undefined): string {
  return tipoDocumento(doc) ?? 'CPF/CNPJ';
}

/**
 * `{{ c.cpf | cpf }}` → CPF mascarado ou CNPJ completo;
 * `{{ c.cpf | cpf:'completo' }}` → inteiro (só onde a pessoa pediu para ver).
 */
@Pipe({ name: 'cpf', standalone: true })
export class CpfPipe implements PipeTransform {
  transform(cpf: string | null | undefined, modo: 'mascarado' | 'completo' = 'mascarado'): string {
    return modo === 'completo' ? formatarCpf(cpf) : mascararCpf(cpf);
  }
}
