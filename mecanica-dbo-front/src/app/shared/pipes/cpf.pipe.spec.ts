import { formatarCpf, mascararCpf } from './cpf.pipe';

describe('CPF na tela', () => {
  it('mascara mostrando só o meio', () => {
    expect(mascararCpf('12345678900')).toBe('***.456.789-**');
    expect(mascararCpf('123.456.789-00')).toBe('***.456.789-**');
  });
  it('CPF fora do padrão não aparece', () => {
    expect(mascararCpf('7894561230')).toBe('***');
  });
  it('vazio continua vazio', () => {
    expect(mascararCpf(null)).toBe('');
    expect(mascararCpf(undefined)).toBe('');
  });
  it('formata o número completo', () => {
    expect(formatarCpf('12345678900')).toBe('123.456.789-00');
  });
});
