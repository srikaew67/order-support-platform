import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ProductListPage } from './product-list-page';
import { environment } from '../../../environments/environment';

describe('ProductListPage', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ProductListPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('renders a product and supports the next page', () => {
    const fixture = TestBed.createComponent(ProductListPage);
    fixture.detectChanges();
    http.expectOne(`${environment.api.order}/api/v1/products?page=0&size=12`)
      .flush({ content: [{ id: 'one', sku: 'A', name: 'Apple', description: '', price: 10, stockQuantity: 2 }], page: 0, size: 12, totalElements: 13, totalPages: 2 });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Apple');
    fixture.componentInstance.nextPage();
    http.expectOne(`${environment.api.order}/api/v1/products?page=1&size=12`)
      .flush({ content: [], page: 1, size: 12, totalElements: 13, totalPages: 2 });
    expect(fixture.componentInstance.page).toBe(1);
  });

  it('shows an error if the catalog request fails', () => {
    const fixture = TestBed.createComponent(ProductListPage);
    fixture.detectChanges();
    http.expectOne(`${environment.api.order}/api/v1/products?page=0&size=12`)
      .flush({}, { status: 503, statusText: 'Unavailable' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('catalog');
  });
});
