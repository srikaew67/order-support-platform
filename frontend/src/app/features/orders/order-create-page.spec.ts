import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { OrderCreatePage } from './order-create-page';
import { environment } from '../../../environments/environment';

describe('OrderCreatePage', () => {
  it('uses the selected product and submits quantity to the order API', () => {
    TestBed.configureTestingModule({
      imports: [OrderCreatePage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: { get: () => 'product-1' } } } }]
    });
    const fixture = TestBed.createComponent(OrderCreatePage);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne(`${environment.api.order}/api/v1/products?page=0&size=100`).flush({
      content: [{ id: 'product-1', sku: 'SKU', name: 'Desk', description: '', price: 20, stockQuantity: 3 }],
      page: 0, size: 100, totalElements: 1, totalPages: 1
    });
    expect(fixture.componentInstance.items[0].productId).toBe('product-1');
    fixture.componentInstance.items[0].quantity = 2;
    fixture.componentInstance.submit();
    const create = http.expectOne(`${environment.api.order}/api/v1/orders`);
    expect(create.request.body).toEqual({ items: [{ productId: 'product-1', quantity: 2 }] });
    create.flush({ id: 'order-1' });
    http.verify();
  });

  it('loads a preselected product outside the first catalog page', () => {
    TestBed.configureTestingModule({
      imports: [OrderCreatePage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: { get: () => 'product-101' } } } }]
    });
    const fixture = TestBed.createComponent(OrderCreatePage);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne(`${environment.api.order}/api/v1/products?page=0&size=100`)
      .flush({ content: [], page: 0, size: 100, totalElements: 101, totalPages: 2 });
    http.expectOne(`${environment.api.order}/api/v1/products/product-101`).flush({
      id: 'product-101', sku: 'LATE', name: 'Late product', description: '', price: 30, stockQuantity: 2
    });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Late product');
    http.verify();
  });

  it('loads another catalog page for a second order line', () => {
    TestBed.configureTestingModule({
      imports: [OrderCreatePage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: { get: () => null } } } }]
    });
    const fixture = TestBed.createComponent(OrderCreatePage);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne(`${environment.api.order}/api/v1/products?page=0&size=100`).flush({
      content: [{ id: 'product-1', sku: 'ONE', name: 'First', description: '', price: 10, stockQuantity: 2 }],
      page: 0, size: 100, totalElements: 101, totalPages: 2
    });
    fixture.componentInstance.addItem();
    fixture.componentInstance.loadMoreProducts();
    http.expectOne(`${environment.api.order}/api/v1/products?page=1&size=100`).flush({
      content: [{ id: 'product-101', sku: 'LATE', name: 'Late product', description: '', price: 30, stockQuantity: 2 }],
      page: 1, size: 100, totalElements: 101, totalPages: 2
    });
    fixture.detectChanges();
    const selects = fixture.nativeElement.querySelectorAll('select') as NodeListOf<HTMLSelectElement>;
    expect(selects.length).toBe(2);
    expect(selects[1].textContent).toContain('Late product');
    http.verify();
  });
});
