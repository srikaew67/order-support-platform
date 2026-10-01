import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ProductService } from './product.service';
import { environment } from '../../../environments/environment';

describe('ProductService', () => {
  let service: ProductService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ProductService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('requests a paginated public catalog', () => {
    let total = 0;
    service.list(1, 12).subscribe(page => total = page.totalElements);
    const request = http.expectOne(`${environment.api.order}/api/v1/products?page=1&size=12`);
    expect(request.request.method).toBe('GET');
    request.flush({ content: [], page: 1, size: 12, totalElements: 24, totalPages: 2 });
    expect(total).toBe(24);
  });

  it('sends product changes to the admin API', () => {
    const input = { sku: 'SKU-1', name: 'Desk', description: '', price: 20, stockQuantity: 3 };
    service.create(input).subscribe();
    const create = http.expectOne(`${environment.api.order}/api/v1/products`);
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual(input);
    create.flush({ id: 'product-1', ...input });

    service.update('product-1', input).subscribe();
    const update = http.expectOne(`${environment.api.order}/api/v1/products/product-1`);
    expect(update.request.method).toBe('PUT');
    update.flush({ id: 'product-1', ...input });

    service.delete('product-1').subscribe();
    const remove = http.expectOne(`${environment.api.order}/api/v1/products/product-1`);
    expect(remove.request.method).toBe('DELETE');
    remove.flush(null);
  });
});
