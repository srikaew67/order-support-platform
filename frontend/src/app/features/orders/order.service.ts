import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../environments/environment';

export type OrderStatus = 'PENDING' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
export interface OrderLine { productId: string; quantity: number; unitPrice: number; subtotal: number; }
export interface Order {
  id: string;
  orderNumber: string;
  customerId: string;
  status: OrderStatus;
  totalAmount: number;
  items: OrderLine[];
  createdAt: string;
}
export interface OrderPage {
  content: Order[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
export interface OrderItemInput { productId: string; quantity: number; }

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.api.order}/api/v1/orders`;

  list(page = 0, size = 10) {
    return this.http.get<OrderPage>(this.url, { params: { page, size } });
  }
  get(id: string) { return this.http.get<Order>(`${this.url}/${id}`); }
  create(items: OrderItemInput[]) { return this.http.post<Order>(this.url, { items }); }
  cancel(id: string) { return this.http.post<Order>(`${this.url}/${id}/cancel`, {}); }
  updateStatus(id: string, status: OrderStatus) {
    return this.http.patch<Order>(`${this.url}/${id}/status`, { status });
  }
}
