import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { Order, OrderService, OrderStatus } from './order.service';

@Component({
  selector: 'app-order-detail-page',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  template: `
    <section class="order-detail">
      <a routerLink="/orders">← Orders</a>
      @if (loading) { <p>Loading order…</p> }
      @if (error) { <p role="alert">{{ error }}</p> }
      @if (order) {
        <h2>{{ order.orderNumber }}</h2>
        <p>Status: <strong>{{ order.status }}</strong> · {{ order.createdAt | date:'medium' }}</p>
        <table><thead><tr><th>Product ID</th><th>Quantity</th><th>Unit price</th><th>Subtotal</th></tr></thead>
          <tbody>@for (item of order.items; track item.productId) {
            <tr><td>{{ item.productId }}</td><td>{{ item.quantity }}</td>
              <td>{{ item.unitPrice | currency }}</td><td>{{ item.subtotal | currency }}</td></tr>
          }</tbody>
        </table>
        <p>Total: <strong>{{ order.totalAmount | currency }}</strong></p>
        @if (auth.role() === 'CUSTOMER') { <a routerLink="/tickets/new" [queryParams]="{ orderId: order.id }">Open support ticket</a> }
        @if (canCancel) { <button type="button" (click)="cancel()" [disabled]="saving">{{ saving ? 'Cancelling…' : 'Cancel order' }}</button> }
        @if (nextStatus) { <button type="button" (click)="advance()" [disabled]="saving">Move to {{ nextStatus }}</button> }
      }
    </section>
  `,
  styles: [`
    .order-detail { max-width: 900px; margin: 2rem auto; padding: 0 1.5rem; }
    table { width: 100%; border-collapse: collapse; } th, td { padding: .7rem; border-bottom: 1px solid #ddd; text-align: left; }
    button { margin-right: .7rem; padding: .7rem; } [role=alert] { color: #a42121; }
  `]
})
export class OrderDetailPage implements OnInit {
  private readonly orders = inject(OrderService);
  private readonly route = inject(ActivatedRoute);
  readonly auth = inject(AuthService);
  order: Order | null = null;
  loading = false;
  saving = false;
  error = '';

  get canCancel(): boolean {
    return !!this.order && (this.order.status === 'PENDING' || this.order.status === 'PROCESSING')
      && (this.auth.role() === 'CUSTOMER' || this.auth.role() === 'ADMIN');
  }
  get nextStatus(): OrderStatus | null {
    if (this.auth.role() !== 'ADMIN') return null;
    switch (this.order?.status) {
      case 'PENDING': return 'PROCESSING';
      case 'PROCESSING': return 'SHIPPED';
      case 'SHIPPED': return 'DELIVERED';
      default: return null;
    }
  }
  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) { this.error = 'Order not found.'; return; }
    this.loading = true;
    this.orders.get(id).subscribe({
      next: order => { this.order = order; this.loading = false; },
      error: () => { this.error = 'Unable to load this order.'; this.loading = false; }
    });
  }
  cancel(): void {
    if (!this.order || !this.canCancel || this.saving || !window.confirm('Cancel this order?')) return;
    this.saving = true;
    this.orders.cancel(this.order.id).subscribe({
      next: order => { this.order = order; this.saving = false; },
      error: failure => { this.error = failure.error?.message || 'Unable to cancel this order.'; this.saving = false; }
    });
  }
  advance(): void {
    if (!this.order || !this.nextStatus || this.saving) return;
    this.saving = true;
    this.orders.updateStatus(this.order.id, this.nextStatus).subscribe({
      next: order => { this.order = order; this.saving = false; },
      error: failure => { this.error = failure.error?.message || 'Unable to update this order.'; this.saving = false; }
    });
  }
}
