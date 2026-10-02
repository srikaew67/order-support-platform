import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Product, ProductService } from '../products/product.service';
import { OrderItemInput, OrderService } from './order.service';

@Component({
  selector: 'app-order-create-page',
  imports: [CurrencyPipe, FormsModule, RouterLink],
  template: `
    <section class="order-form">
      <a routerLink="/orders">← Orders</a><h2>Create order</h2>
      @if (loading) { <p>Loading products…</p> }
      @if (error) { <p role="alert">{{ error }}</p> }
      <form (ngSubmit)="submit()" #orderForm="ngForm">
        @for (item of items; track $index) {
          <div class="item">
            <label>Product
              <select [name]="'product-' + $index" [(ngModel)]="item.productId" required>
                <option value="">Select a product</option>
                @for (product of products; track product.id) {
                  <option [value]="product.id" [disabled]="product.stockQuantity < 1">{{ product.name }} · {{ product.price | currency }} · {{ product.stockQuantity }} in stock</option>
                }
              </select>
            </label>
            <label>Quantity <input [name]="'quantity-' + $index" type="number" [(ngModel)]="item.quantity" required min="1" step="1" /></label>
            @if (items.length > 1) { <button type="button" (click)="removeItem($index)">Remove</button> }
          </div>
        }
        @if (hasMoreProducts) {
          <button type="button" (click)="loadMoreProducts()" [disabled]="loadingMore">
            {{ loadingMore ? 'Loading products…' : 'Load more products' }}
          </button>
        }
        <button type="button" (click)="addItem()">Add another product</button>
        <button type="submit" [disabled]="orderForm.invalid || saving || loading || products.length === 0">{{ saving ? 'Placing order…' : 'Place order' }}</button>
      </form>
    </section>
  `,
  styles: [`
    .order-form { max-width: 720px; margin: 2rem auto; padding: 0 1.5rem; }
    form, .item, label { display: grid; gap: .6rem; } form { gap: 1.3rem; }
    .item { padding: 1rem; border: 1px solid #ddd; border-radius: .5rem; }
    select, input, button { padding: .7rem; font: inherit; } [role=alert] { color: #a42121; }
  `]
})
export class OrderCreatePage implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly orders = inject(OrderService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  products: Product[] = [];
  items: OrderItemInput[] = [{ productId: this.route.snapshot.queryParamMap.get('productId') || '', quantity: 1 }];
  loading = false;
  loadingMore = false;
  catalogPage = 0;
  hasMoreProducts = false;
  saving = false;
  error = '';

  ngOnInit(): void {
    this.loading = true;
    this.productService.list(0, 100).subscribe({
      next: page => {
        this.products = page.content;
        this.catalogPage = page.page;
        this.hasMoreProducts = page.page + 1 < page.totalPages;
        const selected = this.items[0].productId;
        if (selected && !this.products.some(product => product.id === selected)) {
          this.productService.get(selected).subscribe({
            next: product => { this.products.push(product); this.loading = false; },
            error: () => {
              this.items[0].productId = '';
              this.error = 'The selected product is unavailable.';
              this.loading = false;
            }
          });
        } else {
          this.loading = false;
        }
      },
      error: () => { this.error = 'Unable to load products.'; this.loading = false; }
    });
  }
  loadMoreProducts(): void {
    if (!this.hasMoreProducts || this.loadingMore) return;
    this.loadingMore = true;
    this.productService.list(this.catalogPage + 1, 100).subscribe({
      next: page => {
        const known = new Set(this.products.map(product => product.id));
        this.products.push(...page.content.filter(product => !known.has(product.id)));
        this.catalogPage = page.page;
        this.hasMoreProducts = page.page + 1 < page.totalPages;
        this.loadingMore = false;
      },
      error: () => { this.error = 'Unable to load more products.'; this.loadingMore = false; }
    });
  }
  addItem(): void { this.items.push({ productId: '', quantity: 1 }); }
  removeItem(index: number): void { if (this.items.length > 1) this.items.splice(index, 1); }
  submit(): void {
    if (this.saving || this.loading) return;
    this.saving = true;
    this.error = '';
    this.orders.create(this.items).subscribe({
      next: order => void this.router.navigate(['/orders', order.id]),
      error: failure => { this.error = failure.error?.message || 'Unable to place the order.'; this.saving = false; }
    });
  }
}
