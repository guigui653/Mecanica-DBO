import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ListaOs } from './lista-os';

describe('Lista de OS com dados reais', () => {
  let tela: ListaOs;
  let http: HttpTestingController;
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ListaOs], providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    const fixture = TestBed.createComponent(ListaOs);
    tela = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });
  afterEach(() => http.verify());
  it('exibe placa, veículo e cliente do servidor, sem acrescentar exemplos', () => {
    http.expectOne('/ordens').flush([{ id: 17, status: 'CONCLUIDA', totalGeral: 50,
      veiculo: { placa: 'ABC1D23', marca: 'Fiat', modelo: 'Uno', cliente: { nome: 'Cliente real' } } }]);
    expect(tela.todasOrdens).toHaveLength(1);
    expect(tela.todasOrdens[0]).toMatchObject({ id: 17, placa: 'ABC1D23', carro: 'Fiat Uno', cliente: 'Cliente real', mec: '—' });
    tela.selecionarFiltro('Prontas'); expect(tela.ordensFiltradas).toHaveLength(1);
  });
  it('mantém o banco vazio e distingue falha de carregamento', () => {
    http.expectOne('/ordens').flush([]);
    expect(tela.todasOrdens).toHaveLength(0);
    expect(tela.erro).toBe('');
    tela.carregar();
    http.expectOne('/ordens').flush({}, { status: 500, statusText: 'Error' });
    expect(tela.todasOrdens).toHaveLength(0);
    expect(tela.erro).toBeTruthy();
  });
});
