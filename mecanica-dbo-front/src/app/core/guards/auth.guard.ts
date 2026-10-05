import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.verificarSessao().pipe(
    map(sessao => sessao.authenticated
      ? true
      : router.createUrlTree(['/login'], { queryParams: { retorno: state.url } })),
    catchError(() => of(router.createUrlTree(['/login'])))
  );
};
