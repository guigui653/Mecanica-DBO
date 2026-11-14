import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';

import { AuthService } from './core/services/auth';

describe('Shell da oficina', () => {
  let http: HttpTestingController;
  beforeEach(async () => {
    const fakeAuth = {
      usuario: () => 'admin',
      logado: () => true,
      logout: () => {},
      limparSessao: () => {}
    };
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: fakeAuth }
      ]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('mostra os contadores do servidor e os links das seções', () => {
    const fixture = TestBed.createComponent(App); fixture.detectChanges();
    http.expectOne('/dashboard/resumo').flush({
      noPatio: 3, osAbertas: 2, faturamentoMes: 0, totalClientes: 7, patio: [], prontas: [], clientesDoMes: []
    });
    fixture.detectChanges();
    expect(fixture.componentInstance.osAbertas()).toBe(2);
    expect(fixture.componentInstance.totalClientes()).toBe(7);
    expect(fixture.componentInstance.apiOnline()).toBe(true);
    expect(fixture.nativeElement.querySelector('a[href="/ordens"]')).toBeTruthy();
  });
  it('indica indisponibilidade quando a consulta da API falha', () => {
    const fixture = TestBed.createComponent(App); fixture.detectChanges();
    http.expectOne('/dashboard/resumo').error(new ProgressEvent('error'));
    expect(fixture.componentInstance.apiOnline()).toBe(false);
    expect(fixture.componentInstance.osAbertas()).toBeNull();
  });
});
