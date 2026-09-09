import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ListaClientes } from './lista-clientes';

describe('ListaClientes', () => {
  let component: ListaClientes;
  let fixture: ComponentFixture<ListaClientes>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ListaClientes],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ListaClientes);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve ser criado', () => {
    expect(component).toBeTruthy();
  });
});
