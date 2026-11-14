import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors, withNoXsrfProtection } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../../environments/environment';
import { apiInterceptor } from './api.interceptor';

/** O login e toda gravação dependem deste interceptor: ele é a única fonte do token CSRF. */
describe('apiInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([apiInterceptor]), withNoXsrfProtection()),
        provideHttpClientTesting()
      ]
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    document.cookie = 'XSRF-TOKEN=token-de-teste; path=/';
  });

  afterEach(() => {
    backend.verify();
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
  });

  it('prefixa a URL da API e manda o cookie de sessão', () => {
    http.get('/ordens').subscribe();
    const req = backend.expectOne(`${environment.apiUrl}/ordens`);
    expect(req.request.withCredentials).toBe(true);
    req.flush([]);
  });

  it('manda o token CSRF em gravações, inclusive no login', () => {
    http.post('/auth/login', {}).subscribe();
    http.delete('/clientes/7').subscribe();
    const login = backend.expectOne(`${environment.apiUrl}/auth/login`);
    const apagar = backend.expectOne(`${environment.apiUrl}/clientes/7`);
    expect(login.request.headers.get('X-XSRF-TOKEN')).toBe('token-de-teste');
    expect(apagar.request.headers.get('X-XSRF-TOKEN')).toBe('token-de-teste');
    login.flush({});
    apagar.flush(null);
  });

  it('não manda o token em leituras', () => {
    http.get('/clientes').subscribe();
    const req = backend.expectOne(`${environment.apiUrl}/clientes`);
    expect(req.request.headers.has('X-XSRF-TOKEN')).toBe(false);
    req.flush([]);
  });
});
