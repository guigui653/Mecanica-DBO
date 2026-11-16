import { Injectable, effect, signal } from '@angular/core';

export type Tema = 'claro' | 'escuro';

/** Mesma chave lida pelo script do index.html, que aplica o tema antes do Angular subir. */
export const CHAVE_TEMA = 'mecanica-dbo.tema';

/**
 * Tema claro/escuro. A escolha fica no navegador deste computador e é
 * aplicada como data-tema="escuro" no <html>; o styles.css troca as cores.
 */
@Injectable({ providedIn: 'root' })
export class TemaService {
  readonly tema = signal<Tema>(lerTemaSalvo());

  constructor() {
    effect(() => {
      const tema = this.tema();
      document.documentElement.dataset['tema'] = tema;
      try {
        localStorage.setItem(CHAVE_TEMA, tema);
      } catch {
        // Navegador sem armazenamento (janela anônima, bloqueio): o tema vale só até fechar.
      }
    });
  }

  alternar(): void {
    this.tema.update(t => (t === 'escuro' ? 'claro' : 'escuro'));
  }
}

function lerTemaSalvo(): Tema {
  try {
    return localStorage.getItem(CHAVE_TEMA) === 'escuro' ? 'escuro' : 'claro';
  } catch {
    return 'claro';
  }
}
