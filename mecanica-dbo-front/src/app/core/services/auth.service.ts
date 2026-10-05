import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, tap } from 'rxjs';

export interface AuthState {
  authenticated: boolean;
  username: string | null;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  readonly sessao = signal<AuthState>({ authenticated: false, username: null });

  verificarSessao(): Observable<AuthState> {
    return this.http.get<AuthState>('/auth/me').pipe(
      catchError(() => of({ authenticated: false, username: null })),
      tap(sessao => this.sessao.set(sessao))
    );
  }

  login(username: string, password: string): Observable<AuthState> {
    return this.http.post<AuthState>('/auth/login', { username, password }).pipe(
      tap(sessao => this.sessao.set(sessao))
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>('/auth/logout', {}).pipe(
      tap(() => this.sessao.set({ authenticated: false, username: null }))
    );
  }

  limparSessao(): void {
    this.sessao.set({ authenticated: false, username: null });
  }
}
