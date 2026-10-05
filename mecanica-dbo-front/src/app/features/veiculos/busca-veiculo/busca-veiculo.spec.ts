import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';
import { BuscaVeiculo } from './busca-veiculo';

describe('Consulta de veículo sem dados fictícios', () => {
  let tela: BuscaVeiculo;
  let http: HttpTestingController;
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [BuscaVeiculo], providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(),
      { provide: MatDialog, useValue: { open: vi.fn(() => ({ afterClosed: () => of(undefined) })) } }]
    }).compileComponents();
    const fixture = TestBed.createComponent(BuscaVeiculo); tela = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController); fixture.detectChanges();
  });
  afterEach(() => http.verify());
  it('usa o proprietário e o histórico real inclusive para uma placa do antigo protótipo', () => {
    tela.placa = 'QPD8734'; tela.buscar();
    http.expectOne('/veiculos/placa/QPD8734').flush({ id: 5, placa: 'QPD8734', cliente: { nome: 'Proprietário real', telefone1: '123' } });
    http.expectOne('/ordens/veiculo/5').flush([{ id: 10, status: 'ENTREGUE', kmEntrada: 0, totalGeral: 50, dataEntrada: '2026-09-01', reclamacoes: 'Serviço real' }]);
    expect(tela.veiculo?.dono).toBe('Proprietário real');
    expect(tela.veiculo?.km).toBe('0 km');
    expect(tela.veiculo?.historico).toHaveLength(1);
    expect(tela.veiculo?.historico[0].servico).toBe('Serviço real');
  });
  it('não inventa um veículo quando a placa não existe', () => {
    tela.placa = 'QPD8734'; tela.buscar();
    http.expectOne('/veiculos/placa/QPD8734').flush({}, { status: 404, statusText: 'Not Found' });
    expect(tela.veiculo).toBeNull(); expect(tela.buscaRealizada).toBe(true); expect(tela.erro).toBe('');
  });
  it('distingue indisponibilidade da API de placa não cadastrada', () => {
    tela.placa = 'QPD8734'; tela.buscar();
    http.expectOne('/veiculos/placa/QPD8734').error(new ProgressEvent('error'));
    expect(tela.buscaRealizada).toBe(false); expect(tela.erro).toBeTruthy();
    expect(tela.recentes).toHaveLength(0);
  });
});
