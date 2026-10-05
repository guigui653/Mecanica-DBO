import { HttpInterceptorFn } from '@angular/common/http';

const metodosQueAlteramDados = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

function lerCookie(nome: string): string | null {
  const prefixo = `${encodeURIComponent(nome)}=`;
  return document.cookie.split('; ').reduce<string | null>((valor, item) => {
    if (valor || !item.startsWith(prefixo)) return valor;
    return decodeURIComponent(item.substring(prefixo.length));
  }, null);
}

export const csrfInterceptor: HttpInterceptorFn = (req, next) => {
  if (!metodosQueAlteramDados.has(req.method) || req.url.endsWith('/auth/login')) {
    return next(req);
  }

  const token = lerCookie('XSRF-TOKEN');
  return next(token ? req.clone({ setHeaders: { 'X-XSRF-TOKEN': token } }) : req);
};
