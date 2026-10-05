import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { catchError, of } from 'rxjs';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {

  private http = inject(HttpClient);

  /** Contadores exibidos ao lado dos itens do menu. */
  totalClientes = signal<number | null>(null);
  osAbertas     = signal<number | null>(null);
  apiOnline     = signal(false);

  ngOnInit(): void {
    this.carregarContadores();
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
      });

    this.http.get<any[]>('/clientes')
      .pipe(catchError(() => of(null)))
      .subscribe(clientes => {
        if (clientes) this.totalClientes.set(clientes.length);
      });
  }
}
