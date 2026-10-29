import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { NovaOs } from './nova-os';

describe('Abertura de uma OS', () => {
  let tela: NovaOs;
  let http: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [NovaOs], providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(),
      { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap({}) } } }]
    }).compileComponents();
    const fixture = TestBed.createComponent(NovaOs); tela = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
  });
  afterEach(() => { http.verify(); vi.restoreAllMocks(); });
  it('exige um veículo cadastrado', () => {
    tela.criar(); expect(tela.erro()).toContain('veículo cadastrado');
    expect(http.match(() => true)).toHaveLength(0);
  });
  it('normaliza a placa e cria uma OS vinculada ao veículo encontrado', () => {
    tela.placa.set('abc-1234'); tela.buscarVeiculo();
    http.expectOne('/veiculos/placa/ABC1234').flush({ id: 7, placa: 'ABC1234', marca: 'Fiat', modelo: 'Uno' });
    tela.km.set(12000); tela.reclamacoes.set('Ruído nos freios'); tela.criar(); tela.criar();
    const req = http.expectOne('/atendimentos');
    expect(req.request.body).toEqual({ veiculoId: 7, os: { kmEntrada: 12000, reclamacoes: 'Ruído nos freios' } });
    req.flush({ veiculo: { id: 7, placa: 'ABC1234' }, os: { id: 21, status: 'ABERTA' } });
    expect(navegar).toHaveBeenCalledWith(['/ordens', 21]);
  });
  it('limpa o veículo selecionado quando a placa é alterada', () => {
    tela.placa.set('ABC1234'); tela.buscarVeiculo();
    http.expectOne('/veiculos/placa/ABC1234').flush({ id: 7, placa: 'ABC1234' });
    tela.alterarPlaca('DEF5678'); tela.reclamacoes.set('Ruído'); tela.criar();
    http.expectNone('/atendimentos');
    expect(tela.veiculo()).toBeNull();
  });
  it('placa nova: grava cliente, veículo e OS numa chamada só e mantém os dados se der erro', () => {
    tela.placa.set('TST1A23'); tela.buscarVeiculo();
    http.expectOne('/veiculos/placa/TST1A23').flush(null, { status: 404, statusText: 'Not Found' });
    http.expectOne('/veiculos/placa/TST-1A23').flush(null, { status: 404, statusText: 'Not Found' });
    expect(tela.placaNaoCadastrada()).toBe(true);

    tela.modoCliente.set('novo');
    tela.novoNomeCliente.set('Teste Transacao'); tela.novoTelefoneCliente.set('61900000002');
    tela.marca.set('Fiat'); tela.modelo.set('Uno'); tela.cor.set('Branco');
    tela.reclamacoes.set('Barulho'); tela.criar();

    const req = http.expectOne('/atendimentos');
    expect(req.request.body).toEqual({
      novoCliente: { nome: 'Teste Transacao', telefone1: '61900000002' },
      novoVeiculo: { placa: 'TST1A23', marca: 'Fiat', modelo: 'Uno', cor: 'Branco', combustivel: 'FLEX' },
      os: { reclamacoes: 'Barulho' }
    });
    http.expectNone('/clientes');
    req.flush({ status: 409, mensagem: 'Placa já cadastrada: TST1A23' }, { status: 409, statusText: 'Conflict' });

    expect(tela.erro()).toBe('Placa já cadastrada: TST1A23');
    expect(tela.novoNomeCliente()).toBe('Teste Transacao');
    expect(tela.marca()).toBe('Fiat');
    expect(tela.salvando()).toBe(false);
    expect(navegar).not.toHaveBeenCalled();
  });
});
