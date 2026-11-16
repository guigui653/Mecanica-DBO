import { Pipe, PipeTransform } from '@angular/core';

/**
 * CPF na tela. O backend guarda o CPF criptografado e entrega só os números;
 * aqui ele aparece mascarado ("***.456.789-**"), como nos sites do governo.
 * É a mesma regra do Cpfs.mascarar no backend (recibo em PDF).
 */
export function mascararCpf(cpf: string | null | undefined): string {
  const d = (cpf ?? '').replace(/\D/g, '');
  if (!d) return '';
  if (d.length !== 11) return '***';
  return `***.${d.slice(3, 6)}.${d.slice(6, 9)}-**`;
}

/** Número completo, formatado: "12345678900" → "123.456.789-00". */
export function formatarCpf(cpf: string | null | undefined): string {
  const d = (cpf ?? '').replace(/\D/g, '');
  if (d.length !== 11) return cpf ?? '';
  return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9)}`;
}

/** `{{ c.cpf | cpf }}` mascarado; `{{ c.cpf | cpf:'completo' }}` inteiro (só onde a pessoa pediu para ver). */
@Pipe({ name: 'cpf', standalone: true })
export class CpfPipe implements PipeTransform {
  transform(cpf: string | null | undefined, modo: 'mascarado' | 'completo' = 'mascarado'): string {
    return modo === 'completo' ? formatarCpf(cpf) : mascararCpf(cpf);
  }
}
