import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormCliente } from './form-cliente';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

describe('FormCliente', () => {
  let component: FormCliente;
  let fixture: ComponentFixture<FormCliente>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormCliente],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(),
        { provide: MAT_DIALOG_DATA, useValue: null },
        { provide: MatDialogRef, useValue: { close: vi.fn() } }],
    }).compileComponents();

    fixture = TestBed.createComponent(FormCliente);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
