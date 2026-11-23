import { cnpjValido, cpfValido, erroDocumento, formatarCpf, formatarDocumentoDigitando, mascararCpf, tipoDocumento } from './cpf.pipe';

describe('CPF e CNPJ na tela', () => {
  it('CPF: mascara mostrando só o meio', () => {
    expect(mascararCpf('52998224725')).toBe('***.982.247-**');
    expect(mascararCpf('529.982.247-25')).toBe('***.982.247-**');
  });
  it('CNPJ é público: aparece completo, inclusive o alfanumérico', () => {
    expect(mascararCpf('11222333000181')).toBe('11.222.333/0001-81');
    expect(mascararCpf('12ABC34501DE35')).toBe('12.ABC.345/01DE-35');
  });
  it('documento fora do padrão não aparece; vazio continua vazio', () => {
    expect(mascararCpf('7894561230')).toBe('***');
    expect(mascararCpf(null)).toBe('');
    expect(mascararCpf(undefined)).toBe('');
  });
  it('formata o documento completo', () => {
    expect(formatarCpf('52998224725')).toBe('529.982.247-25');
    expect(formatarCpf('12abc34501de35')).toBe('12.ABC.345/01DE-35');
  });
  it('confere os dígitos verificadores (regra da Receita)', () => {
    expect(cpfValido('529.982.247-25')).toBe(true);
    expect(cpfValido('529.982.247-24')).toBe(false);
    expect(cpfValido('111.111.111-11')).toBe(false);
    expect(cnpjValido('11.222.333/0001-81')).toBe(true);
    expect(cnpjValido('12.ABC.345/01DE-35')).toBe(true);   // exemplo oficial do CNPJ alfanumérico
    expect(cnpjValido('12.ABC.345/01DE-36')).toBe(false);
  });
  it('mensagem de erro só quando há algo errado', () => {
    expect(erroDocumento('')).toBeNull();
    expect(erroDocumento('529.982.247-25')).toBeNull();
    expect(erroDocumento('123')).toContain('CPF (11 números)');
    expect(erroDocumento('529.982.247-24')).toContain('CPF inválido');
    expect(erroDocumento('11.222.333/0001-82')).toContain('CNPJ inválido');
  });
  it('formata enquanto digita: CPF até 11 números, depois CNPJ', () => {
    expect(formatarDocumentoDigitando('5299822')).toBe('529.982.2');
    expect(formatarDocumentoDigitando('52998224725')).toBe('529.982.247-25');
    expect(formatarDocumentoDigitando('112223330001')).toBe('11.222.333/0001');
    expect(formatarDocumentoDigitando('11222333000181')).toBe('11.222.333/0001-81');
    expect(formatarDocumentoDigitando('12abc')).toBe('12.ABC');
    expect(tipoDocumento('12.ABC.345/01DE-35')).toBe('CNPJ');
  });
});
