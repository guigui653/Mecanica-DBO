import { Pipe, PipeTransform } from '@angular/core';

/**
 * A placa fica gravada sem hífen (JEH3030). Na tela, o padrão antigo
 * (3 letras + 4 números) aparece com hífen, como no documento do carro:
 * "JEH3030" → "JEH-3030". Mercosul (ABC1D23) e outros ficam como estão.
 * É a mesma regra do Placas.formatar no backend (recibo em PDF).
 */
export function formatarPlaca(placa: string | null | undefined): string {
  if (!placa) return '';
  const p = placa.toUpperCase().replace(/[^A-Z0-9]/g, '');
  return /^[A-Z]{3}\d{4}$/.test(p) ? `${p.slice(0, 3)}-${p.slice(3)}` : placa;
}

@Pipe({ name: 'placa', standalone: true })
export class PlacaPipe implements PipeTransform {
  transform(placa: string | null | undefined): string {
    return formatarPlaca(placa);
  }
}
