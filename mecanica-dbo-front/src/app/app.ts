import { Component, effect, inject, signal } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from './core/services/auth';
import { DashboardService } from './core/services/dashboard';
import { TemaService } from './core/services/tema';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected auth = inject(AuthService);
  private dashboard = inject(DashboardService);
  /** Injetado aqui para o tema valer em todas as telas, inclusive no login. */
  protected tema = inject(TemaService);

  /** Contadores exibidos ao lado dos itens do menu. */
  totalClientes = signal<number | null>(null);
  osAbertas     = signal<number | null>(null);
  apiOnline     = signal(false);
  horaSync      = signal<string>('');

  constructor() {
    effect(() => {
      if (this.auth.logado()) {
        this.atualizarHora();
        this.carregarContadores();
      } else {
        this.totalClientes.set(null);
        this.osAbertas.set(null);
        this.apiOnline.set(false);
      }
    });
  }

  private atualizarHora(): void {
    const agora = new Date();
    this.horaSync.set(
      agora.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
    );
  }

  /**
   * Uma chamada ao resumo do painel alimenta os dois contadores do menu e
   * serve de sinal de vida: se responder, a API está no ar.
   */
  private carregarContadores(): void {
    this.dashboard.resumo().subscribe({
      next: r => {
        this.apiOnline.set(true);
        this.osAbertas.set(r.osAbertas);
        this.totalClientes.set(r.totalClientes);
        this.atualizarHora();
      },
      error: () => this.apiOnline.set(false)
    });
  }
}

/** Alias para compatibilidade com main.ts que importa AppComponent */
export { App as AppComponent };
