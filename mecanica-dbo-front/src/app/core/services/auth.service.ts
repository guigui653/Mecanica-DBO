import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, map, of, switchMap, tap } from 'rxjs';

interface EstadoSessao {
  autenticado: boolean;
  usuario?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  readonly usuario = signal<string | null>(null);
  readonly logado = computed(() => this.usuario() !== null);

  /** Pergunta ao backend se há sessão ativa (cookie JSESSIONID). */
  verificarSessao(): Observable<boolean> {
    return this.http.get<EstadoSessao>('/auth/me').pipe(
      tap((r) => this.usuario.set(r.autenticado ? (r.usuario ?? null) : null)),
      map((r) => r.autenticado),
      catchError(() => {
        this.usuario.set(null);
        return of(false);
      }),
    );
  }

  /**
   * O GET /auth/me antes do POST garante que o cookie XSRF-TOKEN já existe
   * (o backend o grava em qualquer resposta), senão o login seria barrado pelo CSRF.
   */
  login(username: string, password: string): Observable<EstadoSessao> {
    return this.http.get<EstadoSessao>('/auth/me').pipe(
      switchMap(() => this.http.post<EstadoSessao>('/auth/login', { username, password })),
      tap((r) => this.usuario.set(r.usuario ?? null)),
    );
  }

  logout(): void {
    this.http
      .post<void>('/auth/logout', {})
      .pipe(
        catchError(() => of(null)),
        finalize(() => {
          this.limparSessao();
          this.router.navigate(['/login']);
        }),
      )
      .subscribe();
  }

  /** Usado pelo interceptor quando a API responde 401. */
  limparSessao(): void {
    this.usuario.set(null);
  }
}
