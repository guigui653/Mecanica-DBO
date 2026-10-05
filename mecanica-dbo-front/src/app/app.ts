import { Component, OnInit, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { catchError, filter, of } from 'rxjs';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {

  private http = inject(HttpClient);
  private router = inject(Router);

  /** Contadores exibidos ao lado dos itens do menu. */
  totalClientes = signal<number | null>(null);
  osAbertas     = signal<number | null>(null);
  apiOnline     = signal(false);
  horaSync      = signal<string>('');
  naTelaLogin   = signal(this.router.url.startsWith('/login'));

  ngOnInit(): void {
    this.router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe(event => {
      const naTelaLogin = event.urlAfterRedirects.startsWith('/login');
      this.naTelaLogin.set(naTelaLogin);
      if (!naTelaLogin) this.carregarContadores();
    });
    if (!this.naTelaLogin()) {
      this.atualizarHora();
      this.carregarContadores();
    }
  }

  private atualizarHora(): void {
    const agora = new Date();
    this.horaSync.set(
      agora.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
    );
  }

  /**
   * O backend não expõe /health. Usamos /ordens/abertas como sinal de vida:
   * se responder, a API está no ar — e o resultado já alimenta o contador.
   */
  private carregarContadores(): void {
    this.http.get<any[]>('/ordens/abertas')
      .pipe(catchError(() => of(null)))
      .subscribe(ordens => {
        if (ordens === null) {
          this.apiOnline.set(false);
          return;
        }
        this.apiOnline.set(true);
        this.osAbertas.set(ordens.length);
        this.atualizarHora();
      });

    this.http.get<any[]>('/clientes')
      .pipe(catchError(() => of(null)))
      .subscribe(clientes => {
        if (clientes) {
          this.totalClientes.set(clientes.length);
          this.atualizarHora();
        }
      });
  }
}

/** Alias para compatibilidade com main.ts que importa AppComponent */
export { App as AppComponent };
