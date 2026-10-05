import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  if (req.url.startsWith('http')) return next(req);

  const url = `${environment.apiUrl}${req.url}`;
  return next(req.clone({ url, withCredentials: true })).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !url.endsWith('/auth/login') && !url.endsWith('/auth/me')) {
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};
