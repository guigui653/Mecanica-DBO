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

  it('mostra os números que o backend já contou, numa chamada só', () => {
    http.expectOne('/dashboard/resumo').flush({
      noPatio: 3, osAbertas: 2, faturamentoMes: 250, totalClientes: 7,
      patio: [{ id: 5, status: 'ABERTA', dataEntrada: '2026-10-05' }],
      prontas: [{ id: 9, status: 'CONCLUIDA' }],
      clientesDoMes: [{ id: 7, nome: 'Teste Dois', telefone1: '61999990002', criadoEm: '2026-11-02T10:00:00' }]
    });
    expect(tela.totalPatio()).toBe(3);
    expect(tela.osAbertas()).toBe(2);
    expect(tela.faturamentoMes()).toBe(250);
    expect(tela.prontas()).toHaveLength(1);
    expect(tela.clientesNovos()).toBe(1);
    expect(tela.labelStatus(tela.prontas()[0])).toBe('Pronta p/ entrega');
    expect(tela.badgeClass(tela.patioOrdenado()[0])).toBe('badge badge-aberta');
  });

  it('distingue falha da API de um pátio vazio', () => {
    http.expectOne('/dashboard/resumo').error(new ProgressEvent('error'));
    expect(tela.erro()).toBe(true);
    expect(tela.carregando()).toBe(false);
    expect(tela.totalPatio()).toBe(0);
  });
});
