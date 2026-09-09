import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { VeiculoService } from './veiculo';

describe('VeiculoService', () => {
  let service: VeiculoService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(VeiculoService);
  });

  it('deve ser criado', () => {
    expect(service).toBeTruthy();
  });
});
