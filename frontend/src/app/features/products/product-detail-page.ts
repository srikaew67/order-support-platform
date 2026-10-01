import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { Product, ProductService } from './product.service';

@Component({
  selector: 'app-product-detail-page',
  imports: [RouterLink, CurrencyPipe],
  template: `
    <section class="detail">
      <a routerLink="/products">← Products</a>
      @if (loading) { <p>Loading product…</p> }
      @if (error) { <p role="alert">{{ error }}</p> }
      @if (product) {
        <h2>{{ product.name }}</h2>
        <p>{{ product.description }}</p>
        <dl><dt>SKU</dt><dd>{{ product.sku }}</dd>
          <dt>Price</dt><dd>{{ product.price | currency }}</dd>
          <dt>In stock</dt><dd>{{ product.stockQuantity }}</dd></dl>
        @if (isAdmin) {
          <a [routerLink]="['/products', product.id, 'edit']">Edit product</a>
          <button type="button" (click)="remove()" [disabled]="deleting">{{ deleting ? 'Deleting…' : 'Delete product' }}</button>
        }
      }
    </section>
  `,
  styles: [`
    .detail { max-width: 720px; margin: 2rem auto; padding: 0 1.5rem; }
    dt { font-weight: bold; } dd { margin: 0 0 1rem; }
    button { margin-left: 1rem; } [role=alert] { color: #a42121; }
  `]
})
export class ProductDetailPage implements OnInit {
  private readonly products = inject(ProductService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  readonly isAdmin = this.auth.role() === 'ADMIN';
  product: Product | null = null;
  loading = false;
  deleting = false;
  error = '';

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) { this.error = 'Product not found.'; return; }
    this.loading = true;
    this.products.get(id).subscribe({
      next: product => { this.product = product; this.loading = false; },
      error: () => { this.error = 'Unable to load this product.'; this.loading = false; }
    });
  }

  remove(): void {
    if (!this.product || this.deleting) return;
    if (!window.confirm(`Delete ${this.product.name}?`)) return;
    this.deleting = true;
    this.products.delete(this.product.id).subscribe({
      next: () => void this.router.navigateByUrl('/products'),
      error: () => { this.error = 'Unable to delete this product.'; this.deleting = false; }
    });
  }
}
