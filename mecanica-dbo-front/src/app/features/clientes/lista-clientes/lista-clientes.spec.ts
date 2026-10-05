import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { ListaClientes } from './lista-clientes';

describe('Clientes com informações reais', () => {
  let tela: ListaClientes;
  let http: HttpTestingController;
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ListaClientes], providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations()] }).compileComponents();
    const fixture = TestBed.createComponent(ListaClientes); tela = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController); fixture.detectChanges();
  });
  afterEach(() => http.verify());
  it('deduz a última visita e OS em aberto a partir das ordens', () => {
    http.expectOne('/clientes').flush([{ id: 7, nome: 'Cliente real', telefone1: '123' }]);
    http.expectOne('/ordens').flush([{ id: 3, status: 'ABERTA', dataEntrada: '2026-09-01', veiculo: { cliente: { id: 7 } } }]);
    expect(tela.clientes[0].tags).toContain('Com OS aberta');
    expect(tela.clientes[0].ultimaVisita).toBe('01/09/2026');
    expect(tela.clientes[0].totalVeiculos).toBeUndefined();
    tela.selecionarCliente(tela.clientes[0]);
    http.expectOne('/clientes/7/veiculos').flush([{ id: 4 }, { id: 5 }]);
    expect(tela.clientes[0].totalVeiculos).toBe(2);
  });
  it('não fabrica visitas nem filtros quando as ordens estão indisponíveis', () => {
    http.expectOne('/clientes').flush([{ id: 7, nome: 'Cliente real', telefone1: '123' }]);
    http.expectOne('/ordens').error(new ProgressEvent('error'));
    expect(tela.clientes[0].ultimaVisita).toBeUndefined();
    expect(tela.clientes[0].tags).toHaveLength(0);
    expect(tela.aviso).toBeTruthy();
    expect(tela.filtros).toEqual(['Todos']);
  });
});
