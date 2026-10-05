import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { of } from 'rxjs';
import { DetalheOs } from './detalhe-os';
import { OrdemServico } from '../../../shared/models/ordem-servico.model';

registerLocaleData(localePt, 'pt-BR');
const ordem: OrdemServico = { id: 1, status: 'ABERTA', diagnostico: 'Laudo salvo', kmEntrada: 100, itensPeca: [], itensServico: [], totalGeral: 0 };

describe('Detalhe da OS: preservação e validação', () => {
  let fixture: ComponentFixture<DetalheOs>;
  let tela: DetalheOs;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DetalheOs],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: '1' })) } }]
    }).compileComponents();
    fixture = TestBed.createComponent(DetalheOs);
    tela = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });
  afterEach(() => { http.verify(); vi.restoreAllMocks(); });
  function carregar(os = ordem) { http.expectOne('/ordens/1').flush(os); }

  it('mantém o diagnóstico pendente ao adicionar uma peça e atualiza o total', () => {
    carregar();
    tela.fDiagnostico.set('Laudo ainda não salvo');
    tela.pDescricao.set('Filtro'); tela.pValor.set(50);
    tela.adicionarPeca();
    const req = http.expectOne('/ordens/1/pecas');
    expect(req.request.body.quantidade).toBe(1);
    req.flush({ ...ordem, totalGeral: 50, itensPeca: [{ id: 8, descricao: 'Filtro', quantidade: 1, valorUnitario: 50, valorTotal: 50 }] });
    expect(tela.fDiagnostico()).toBe('Laudo ainda não salvo');
    expect(tela.temAlteracoes()).toBe(true);
    expect(tela.os()?.totalGeral).toBe(50);
  });
  it('mantém os campos pendentes ao remover mão de obra', () => {
    carregar({ ...ordem, itensServico: [{ id: 9, descricao: 'Revisão', valor: 100 }] });
    tela.fReclamacoes.set('Relato pendente');
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    tela.removerServico({ id: 9, descricao: 'Revisão', valor: 100 });
    http.expectOne('/ordens/1/servicos/9').flush(ordem);
    expect(tela.fReclamacoes()).toBe('Relato pendente');
    expect(tela.temAlteracoes()).toBe(true);
  });
  it('repete a busca após uma falha na primeira carga', () => {
    http.expectOne('/ordens/1').flush({}, { status: 503, statusText: 'Unavailable' });
    tela.recarregar();
    carregar();
    expect(tela.erro()).toBeNull();
    expect(tela.os()?.id).toBe(1);
  });
  it.each([0, -1, null, NaN])('recusa quantidade inválida %s antes de enviar', quantidade => {
    carregar(); tela.pDescricao.set('Peça'); tela.pValor.set(10); tela.pQuantidade.set(quantidade);
    tela.adicionarPeca();
    http.expectNone('/ordens/1/pecas');
    expect(tela.pErro()).toContain('quantidade');
  });
  it('recusa valor negativo e aceita um serviço gratuito', () => {
    carregar(); tela.sDescricao.set('Verificação'); tela.sValor.set(-10);
    tela.adicionarServico(); http.expectNone('/ordens/1/servicos');
    tela.sValor.set(0); tela.adicionarServico();
    expect(http.expectOne('/ordens/1/servicos').request.body.valor).toBe(0);
  });
  it('recusa quilometragem inválida e permite limpar o campo', () => {
    carregar(); tela.fKm.set(-1); tela.salvar(); http.expectNone('/ordens/1');
    expect(tela.formErro()).toBeTruthy();
    tela.fKm.set(null); tela.salvar();
    const req = http.expectOne('/ordens/1');
    expect(req.request.body.kmEntrada).toBeNull();
    req.flush({ ...ordem, kmEntrada: null });
    expect(tela.temAlteracoes()).toBe(false);
  });
  it('pede para salvar antes de mudar status ou gerar recibo', () => {
    carregar(); tela.fDiagnostico.set('Rascunho');
    tela.avancarStatus(); tela.baixarPdf();
    http.expectNone('/ordens/1/status?status=EM_ANDAMENTO');
    http.expectNone('/ordens/1/pdf');
    expect(tela.fDiagnostico()).toBe('Rascunho');
  });
  it('permite cancelar a saída quando há alterações pendentes', () => {
    carregar(); tela.fDiagnostico.set('Rascunho');
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    expect(tela.podeSair()).toBe(false);
  });
  it('bloqueia a repetição de uma operação em andamento', () => {
    carregar(); tela.pDescricao.set('Filtro'); tela.pValor.set(50);
    tela.adicionarPeca(); tela.adicionarPeca();
    const req = http.expectOne('/ordens/1/pecas');
    req.flush(ordem);
    expect(tela.salvando()).toBe(false);
  });
  it('bloqueia edição de uma OS entregue mesmo chamando os métodos diretamente', () => {
    carregar({ ...ordem, status: 'ENTREGUE' });
    tela.fDiagnostico.set('Alteração'); tela.salvar();
    tela.pDescricao.set('Peça'); tela.pValor.set(1); tela.adicionarPeca();
    expect(http.match(() => true).length).toBe(0);
  });
});
