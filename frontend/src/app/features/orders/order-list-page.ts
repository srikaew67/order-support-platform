import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { OrderPage, OrderService } from './order.service';

@Component({
  selector: 'app-order-list-page',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  template: `
    <section class="orders">
      <header><h2>Orders</h2>@if (isCustomer) { <a routerLink="/orders/new">Create order</a> }</header>
      @if (loading) { <p>Loading orders…</p> }
      @if (error) { <p role="alert">{{ error }}</p> }
      @if (data) {
        @if (data.content.length === 0) { <p>No orders yet.</p> }
        <ul>
          @for (order of data.content; track order.id) {
            <li><a [routerLink]="['/orders', order.id]">{{ order.orderNumber }}</a>
              <span>{{ order.status }}</span> · {{ order.totalAmount | currency }} · {{ order.createdAt | date:'mediumDate' }}</li>
          }
        </ul>
        <nav aria-label="Order pages">
          <button type="button" (click)="previousPage()" [disabled]="page === 0">Previous</button>
          <span>Page {{ page + 1 }} of {{ data.totalPages || 1 }}</span>
          <button type="button" (click)="nextPage()" [disabled]="page + 1 >= data.totalPages">Next</button>
        </nav>
      }
    </section>
  `,
  styles: [`
    .orders { max-width: 900px; margin: 2rem auto; padding: 0 1.5rem; }
    header, nav { display: flex; justify-content: space-between; align-items: center; gap: 1rem; }
    li { padding: 1rem; border-bottom: 1px solid #ddd; } ul { padding: 0; list-style: none; }
    li span { margin-left: 1rem; } nav { justify-content: center; margin: 2rem 0; }
    [role=alert] { color: #a42121; }
  `]
})
export class OrderListPage implements OnInit {
  private readonly orders = inject(OrderService);
  private readonly auth = inject(AuthService);
  readonly isCustomer = this.auth.role() === 'CUSTOMER';
  data: OrderPage | null = null;
  page = 0;
  loading = false;
  error = '';

  ngOnInit(): void { this.load(); }
  load(): void {
    this.loading = true;
    this.error = '';
    this.orders.list(this.page).subscribe({
      next: data => { this.data = data; this.loading = false; },
      error: () => { this.data = null; this.error = 'Unable to load orders.'; this.loading = false; }
    });
  }
  nextPage(): void { if (this.data && this.page + 1 < this.data.totalPages) { this.page++; this.load(); } }
  previousPage(): void { if (this.page > 0) { this.page--; this.load(); } }
}
