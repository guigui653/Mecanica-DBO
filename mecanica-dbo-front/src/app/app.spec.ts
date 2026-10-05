import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';

describe('Shell da oficina', () => {
  let http: HttpTestingController;
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [App], providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('mostra os contadores do servidor e os links das seções', () => {
    const fixture = TestBed.createComponent(App); fixture.detectChanges();
    http.expectOne('/ordens/abertas').flush([{ id: 1 }, { id: 2 }]);
    http.expectOne('/clientes').flush([{ id: 7 }]);
    fixture.detectChanges();
    expect(fixture.componentInstance.osAbertas()).toBe(2);
    expect(fixture.componentInstance.totalClientes()).toBe(1);
    expect(fixture.componentInstance.apiOnline()).toBe(true);
    expect(fixture.nativeElement.querySelector('a[href="/ordens"]')).toBeTruthy();
  });
  it('indica indisponibilidade quando a consulta da API falha', () => {
    const fixture = TestBed.createComponent(App); fixture.detectChanges();
    http.expectOne('/ordens/abertas').error(new ProgressEvent('error'));
    http.expectOne('/clientes').error(new ProgressEvent('error'));
    expect(fixture.componentInstance.apiOnline()).toBe(false);
    expect(fixture.componentInstance.osAbertas()).toBeNull();
  });
});
