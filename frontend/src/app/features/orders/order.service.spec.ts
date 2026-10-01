import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { OrderService } from './order.service';
import { environment } from '../../../environments/environment';

describe('OrderService', () => {
  let service: OrderService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(OrderService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('lists and reads orders through the order API', () => {
    service.list(1, 10).subscribe();
    const list = http.expectOne(`${environment.api.order}/api/v1/orders?page=1&size=10`);
    expect(list.request.method).toBe('GET');
    list.flush({ content: [], page: 1, size: 10, totalElements: 0, totalPages: 0 });
    service.get('order-1').subscribe();
    const detail = http.expectOne(`${environment.api.order}/api/v1/orders/order-1`);
    expect(detail.request.method).toBe('GET');
    detail.flush({ id: 'order-1' });
  });

  it('creates and cancels an order', () => {
    service.create([{ productId: 'product-1', quantity: 2 }]).subscribe();
    const create = http.expectOne(`${environment.api.order}/api/v1/orders`);
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual({ items: [{ productId: 'product-1', quantity: 2 }] });
    create.flush({ id: 'order-1' });
    service.cancel('order-1').subscribe();
    const cancel = http.expectOne(`${environment.api.order}/api/v1/orders/order-1/cancel`);
    expect(cancel.request.method).toBe('POST');
    cancel.flush({ id: 'order-1', status: 'CANCELLED' });
  });
});
