import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';

const METODOS_SEGUROS = ['GET', 'HEAD', 'OPTIONS'];

function lerCookie(nome: string): string | null {
  const par = document.cookie.split('; ').find((c) => c.startsWith(nome + '='));
  return par ? decodeURIComponent(par.substring(nome.length + 1)) : null;
}

/**
 * 1) prefixa URLs relativas com apiUrl
 * 2) envia o cookie de sessão (withCredentials)
 * 3) devolve o token CSRF no header X-XSRF-TOKEN em POST/PUT/PATCH/DELETE
 *    (feito à mão porque o do Angular ignora URLs absolutas de outra origem, como :8080)
 * 4) 401 em qualquer chamada -> volta para /login
 */
export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const url = req.url.startsWith('http') ? req.url : `${environment.apiUrl}${req.url}`;
  if (!url.startsWith(environment.apiUrl)) {
    return next(req.clone({ url })); // chamada externa: não mexe
  }

  let headers = req.headers;
  if (!METODOS_SEGUROS.includes(req.method)) {
    const token = lerCookie('XSRF-TOKEN');
    if (token) headers = headers.set('X-XSRF-TOKEN', token);
  }

  return next(req.clone({ url, headers, withCredentials: true })).pipe(
    catchError((err: unknown) => {
      const rotaDeAuth = url.includes('/auth/');
      if (err instanceof HttpErrorResponse && err.status === 401 && !rotaDeAuth) {
        auth.limparSessao();
        if (!router.url.startsWith('/login')) {
          router.navigate(['/login'], { queryParams: { returnUrl: router.url } });
        }
      }
      return throwError(() => err);
    }),
  );
};
