import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { OrderListPage } from './order-list-page';
import { environment } from '../../../environments/environment';

describe('OrderListPage', () => {
  it('loads a page of orders and can request the next page', () => {
    TestBed.configureTestingModule({
      imports: [OrderListPage], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });
    const fixture = TestBed.createComponent(OrderListPage);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne(`${environment.api.order}/api/v1/orders?page=0&size=10`).flush({
      content: [{ id: 'order-1', orderNumber: 'ORD-1', status: 'PENDING', totalAmount: 20,
        customerId: 'customer-1', items: [], createdAt: '2026-10-01T00:00:00Z' }],
      page: 0, size: 10, totalElements: 11, totalPages: 2
    });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('ORD-1');
    fixture.componentInstance.nextPage();
    http.expectOne(`${environment.api.order}/api/v1/orders?page=1&size=10`)
      .flush({ content: [], page: 1, size: 10, totalElements: 11, totalPages: 2 });
    expect(fixture.componentInstance.page).toBe(1);
    http.verify();
  });
});
