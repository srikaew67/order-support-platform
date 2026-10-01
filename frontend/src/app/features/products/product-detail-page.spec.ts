import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { ProductDetailPage } from './product-detail-page';
import { environment } from '../../../environments/environment';

describe('ProductDetailPage', () => {
  it('loads and displays the selected product', () => {
    TestBed.configureTestingModule({
      imports: [ProductDetailPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'product-1' } } } }]
    });
    const fixture = TestBed.createComponent(ProductDetailPage);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne(`${environment.api.order}/api/v1/products/product-1`).flush({
      id: 'product-1', sku: 'DESK', name: 'Desk', description: 'Oak desk', price: 20, stockQuantity: 3
    });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Oak desk');
    http.verify();
  });
});
