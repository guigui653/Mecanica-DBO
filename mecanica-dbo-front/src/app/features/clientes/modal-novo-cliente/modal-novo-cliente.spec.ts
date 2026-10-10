import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ModalNovoCliente } from './modal-novo-cliente';

describe('ModalNovoCliente', () => {
  let component: ModalNovoCliente;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ModalNovoCliente],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(ModalNovoCliente);
    component = fixture.componentInstance;
  });

  it('deve ser instanciado corretamente', () => {
    expect(component).toBeTruthy();
  });

  it('deve exigir nome do cliente antes de salvar', () => {
    component.nome.set('');
    component.salvar();
    expect(component.erro()).toContain('nome');
  });

  it('deve exigir telefone do cliente antes de salvar', () => {
    component.nome.set('Carlos Silva');
    component.telefone.set('');
    component.salvar();
    expect(component.erro()).toContain('telefone');
  });

  it('deve exigir placa com 7 caracteres', () => {
    component.nome.set('Carlos Silva');
    component.telefone.set('61999998888');
    component.placa.set('ABC12');
    component.salvar();
    expect(component.erro()).toContain('placa');
  });
});
