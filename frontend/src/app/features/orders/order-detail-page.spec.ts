import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { OrderDetailPage } from './order-detail-page';
import { environment } from '../../../environments/environment';

describe('OrderDetailPage', () => {
  it('loads an order and cancels it after confirmation', () => {
    localStorage.setItem('role', 'CUSTOMER');
    TestBed.configureTestingModule({
      imports: [OrderDetailPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'order-1' } } } }]
    });
    const fixture = TestBed.createComponent(OrderDetailPage);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne(`${environment.api.order}/api/v1/orders/order-1`).flush({
      id: 'order-1', orderNumber: 'ORD-1', status: 'PENDING', totalAmount: 20,
      customerId: 'customer-1', items: [], createdAt: '2026-10-01T00:00:00Z'
    });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('ORD-1');
    spyOn(window, 'confirm').and.returnValue(true);
    fixture.componentInstance.cancel();
    const cancel = http.expectOne(`${environment.api.order}/api/v1/orders/order-1/cancel`);
    expect(cancel.request.method).toBe('POST');
    cancel.flush({ id: 'order-1', orderNumber: 'ORD-1', status: 'CANCELLED', totalAmount: 20,
      customerId: 'customer-1', items: [], createdAt: '2026-10-01T00:00:00Z' });
    expect(fixture.componentInstance.order?.status).toBe('CANCELLED');
    http.verify();
  });
});
