import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { OrdemServicoService } from './ordem-servico';

describe('OrdemServicoService', () => {
  let service: OrdemServicoService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(OrdemServicoService);
  });

  it('deve ser criado', () => {
    expect(service).toBeTruthy();
  });
});
