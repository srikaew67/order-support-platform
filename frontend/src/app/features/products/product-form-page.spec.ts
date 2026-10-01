import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { ProductFormPage } from './product-form-page';
import { environment } from '../../../environments/environment';

describe('ProductFormPage', () => {
  it('submits a new product to the API', () => {
    TestBed.configureTestingModule({
      imports: [ProductFormPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => null } } } }]
    });
    const fixture = TestBed.createComponent(ProductFormPage);
    fixture.detectChanges();
    const form = fixture.componentInstance;
    form.product = { sku: 'DESK', name: 'Desk', description: 'Oak', price: 20, stockQuantity: 3 };
    form.submit();
    const http = TestBed.inject(HttpTestingController);
    const request = http.expectOne(`${environment.api.order}/api/v1/products`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body.name).toBe('Desk');
    request.flush({ id: 'product-1', ...form.product });
    http.verify();
  });
});
