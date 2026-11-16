import { TestBed } from '@angular/core/testing';
import { CHAVE_TEMA, TemaService } from './tema';

describe('TemaService', () => {
  afterEach(() => {
    localStorage.removeItem(CHAVE_TEMA);
    delete document.documentElement.dataset['tema'];
  });

  it('começa claro e alterna para escuro, guardando a escolha', () => {
    const tema = TestBed.inject(TemaService);
    TestBed.tick();
    expect(tema.tema()).toBe('claro');
    expect(document.documentElement.dataset['tema']).toBe('claro');

    tema.alternar();
    TestBed.tick();
    expect(document.documentElement.dataset['tema']).toBe('escuro');
    expect(localStorage.getItem(CHAVE_TEMA)).toBe('escuro');
  });

  it('volta com o tema que estava salvo', () => {
    localStorage.setItem(CHAVE_TEMA, 'escuro');
    const tema = TestBed.inject(TemaService);
    expect(tema.tema()).toBe('escuro');
  });
});
