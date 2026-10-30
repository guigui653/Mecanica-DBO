import { formatarPlaca } from './placa.pipe';

describe('formatarPlaca', () => {
  it('põe hífen só no padrão antigo', () => {
    expect(formatarPlaca('JEH3030')).toBe('JEH-3030');
    expect(formatarPlaca('jeh3030')).toBe('JEH-3030');
    expect(formatarPlaca('JEH-3030')).toBe('JEH-3030');
  });
  it('deixa Mercosul e outros formatos como estão', () => {
    expect(formatarPlaca('ABC1D23')).toBe('ABC1D23');
    expect(formatarPlaca('TESTE001')).toBe('TESTE001');
  });
  it('trata vazio', () => {
    expect(formatarPlaca(null)).toBe('');
    expect(formatarPlaca(undefined)).toBe('');
  });
});
