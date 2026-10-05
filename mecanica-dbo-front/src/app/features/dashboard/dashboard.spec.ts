import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { Dashboard } from './dashboard';
registerLocaleData(localePt, 'pt-BR');

describe('Painel com dados reais', () => {
  let tela: Dashboard;
  let http: HttpTestingController;
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Dashboard], providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    const fixture = TestBed.createComponent(Dashboard); tela = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController); fixture.detectChanges();
  });
  afterEach(() => http.verify());
  it('calcula faturamento com uma entrega no primeiro dia do mês local', () => {
    const hoje = new Date();
    const primeiroDia = `${hoje.getFullYear()}-${String(hoje.getMonth() + 1).padStart(2, '0')}-01`;
    http.expectOne('/ordens').flush([
      { id: 1, status: 'ENTREGUE', dataSaidaReal: primeiroDia, totalGeral: 250 },
      { id: 2, status: 'ABERTA', dataEntrada: primeiroDia, totalGeral: 50 }
    ]);
    http.expectOne('/clientes').flush([]);
    expect(tela.faturamentoMes()).toBe(250);
    expect(tela.totalPatio()).toBe(1);
  });
  it('distingue falha da API de um pátio vazio', () => {
    http.expectOne('/ordens').error(new ProgressEvent('error'));
    http.expectOne('/clientes').flush([]);
    expect(tela.erro()).toBe(true);
    expect(tela.carregando()).toBe(false);
  });
});
