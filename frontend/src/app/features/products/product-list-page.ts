import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ProductPage, ProductService } from './product.service';

@Component({
  selector: 'app-product-list-page',
  imports: [RouterLink, CurrencyPipe],
  template: `
    <section class="catalog">
      <header><div><h2>Products</h2><p>Browse the current catalog.</p></div>
        @if (isAdmin) { <a routerLink="/products/new">Add product</a> }
      </header>
      @if (loading) { <p>Loading catalog…</p> }
      @if (error) { <p role="alert">{{ error }}</p> }
      @if (data) {
        @if (data.content.length === 0) { <p>No products available.</p> }
        <div class="products">
          @for (product of data.content; track product.id) {
            <article>
              <h3><a [routerLink]="['/products', product.id]">{{ product.name }}</a></h3>
              <p>{{ product.description }}</p>
              <p>{{ product.price | currency }}</p>
              <p>{{ product.stockQuantity }} in stock</p>
            </article>
          }
        </div>
        <nav aria-label="Catalog pages">
          <button type="button" (click)="previousPage()" [disabled]="page === 0">Previous</button>
          <span>Page {{ page + 1 }} of {{ data.totalPages || 1 }}</span>
          <button type="button" (click)="nextPage()" [disabled]="page + 1 >= data.totalPages">Next</button>
        </nav>
      }
    </section>
  `,
  styles: [`
    .catalog { max-width: 960px; margin: 2rem auto; padding: 0 1.5rem; }
    header, nav { display: flex; justify-content: space-between; align-items: center; gap: 1rem; }
    .products { display: grid; grid-template-columns: repeat(auto-fit, minmax(210px, 1fr)); gap: 1rem; }
    article { border: 1px solid #ddd; border-radius: .5rem; padding: 1rem; background: #fff; }
    nav { justify-content: center; margin: 2rem 0; }
    [role=alert] { color: #a42121; }
  `]
})
export class ProductListPage implements OnInit {
  private readonly products = inject(ProductService);
  private readonly auth = inject(AuthService);
  readonly isAdmin = this.auth.role() === 'ADMIN';
  data: ProductPage | null = null;
  page = 0;
  loading = false;
  error = '';

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.error = '';
    this.products.list(this.page).subscribe({
      next: data => { this.data = data; this.loading = false; },
      error: () => { this.data = null; this.error = 'Unable to load the catalog. Please try again.'; this.loading = false; }
    });
  }

  nextPage(): void {
    if (this.data && this.page + 1 < this.data.totalPages) { this.page++; this.load(); }
  }

  previousPage(): void {
    if (this.page > 0) { this.page--; this.load(); }
  }
}
