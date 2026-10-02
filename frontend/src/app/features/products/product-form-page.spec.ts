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

  it('sends the loaded version when editing a product', () => {
    TestBed.configureTestingModule({
      imports: [ProductFormPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'product-1' } } } }]
    });
    const fixture = TestBed.createComponent(ProductFormPage);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne(`${environment.api.order}/api/v1/products/product-1`).flush({
      id: 'product-1', sku: 'DESK', name: 'Desk', description: 'Oak', price: 20, stockQuantity: 3, version: 7
    });
    fixture.componentInstance.product.price = 25;
    fixture.componentInstance.submit();
    const update = http.expectOne(`${environment.api.order}/api/v1/products/product-1`);
    expect(update.request.method).toBe('PUT');
    expect(update.request.body.version).toBe(7);
    update.flush({ id: 'product-1', ...update.request.body, version: 8 });
    http.verify();
  });
});
