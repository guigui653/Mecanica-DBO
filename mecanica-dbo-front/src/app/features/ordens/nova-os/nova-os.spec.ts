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
    const req = http.expectOne('/ordens/veiculo/7');
    expect(req.request.body).toMatchObject({ kmEntrada: 12000, reclamacoes: 'Ruído nos freios' });
    expect(req.request.body.status).toBeUndefined();
    expect(req.request.body.totalGeral).toBeUndefined();
    req.flush({ id: 21, status: 'ABERTA' });
    expect(navegar).toHaveBeenCalledWith(['/ordens', 21]);
  });
  it('limpa o veículo selecionado quando a placa é alterada', () => {
    tela.placa.set('ABC1234'); tela.buscarVeiculo();
    http.expectOne('/veiculos/placa/ABC1234').flush({ id: 7, placa: 'ABC1234' });
    tela.alterarPlaca('DEF5678'); tela.criar();
    http.expectNone('/ordens/veiculo/7');
    expect(tela.veiculo()).toBeNull();
  });
});
