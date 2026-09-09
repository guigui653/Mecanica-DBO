import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.url.startsWith('http')) {
    return next(req);
  }
  return next(req.clone({
    url: `${environment.apiUrl}${req.url}`
  }));
};
