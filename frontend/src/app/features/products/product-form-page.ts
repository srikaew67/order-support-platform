import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProductInput, ProductService } from './product.service';

@Component({
  selector: 'app-product-form-page',
  imports: [FormsModule, RouterLink],
  template: `
    <section class="editor">
      <a routerLink="/products">← Products</a>
      <h2>{{ id ? 'Edit product' : 'Add product' }}</h2>
      @if (loading) { <p>Loading product…</p> }
      @if (error) { <p role="alert">{{ error }}</p> }
      <form (ngSubmit)="submit()" #productForm="ngForm">
        <label>SKU <input name="sku" [(ngModel)]="product.sku" required maxlength="80" /></label>
        <label>Name <input name="name" [(ngModel)]="product.name" required maxlength="255" /></label>
        <label>Description <textarea name="description" [(ngModel)]="product.description"></textarea></label>
        <label>Price <input name="price" type="number" [(ngModel)]="product.price" required min="0.01" step="0.01" /></label>
        <label>Stock quantity <input name="stockQuantity" type="number" [(ngModel)]="product.stockQuantity" required min="0" step="1" /></label>
        <button type="submit" [disabled]="productForm.invalid || saving || loading">{{ saving ? 'Saving…' : 'Save product' }}</button>
      </form>
    </section>
  `,
  styles: [`
    .editor { max-width: 600px; margin: 2rem auto; padding: 0 1.5rem; }
    form, label { display: grid; gap: .5rem; } form { gap: 1rem; }
    input, textarea { padding: .7rem; border: 1px solid #aaa; border-radius: .3rem; font: inherit; }
    button { padding: .75rem; font: inherit; } [role=alert] { color: #a42121; }
  `]
})
export class ProductFormPage implements OnInit {
  private readonly products = inject(ProductService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly id = this.route.snapshot.paramMap.get('id');
  product: ProductInput = { sku: '', name: '', description: '', price: 0, stockQuantity: 0 };
  loading = false;
  saving = false;
  error = '';

  ngOnInit(): void {
    if (!this.id) return;
    this.loading = true;
    this.products.get(this.id).subscribe({
      next: product => { this.product = { sku: product.sku, name: product.name,
        description: product.description, price: product.price, stockQuantity: product.stockQuantity }; this.loading = false; },
      error: () => { this.error = 'Unable to load this product.'; this.loading = false; }
    });
  }

  submit(): void {
    if (this.saving || this.loading) return;
    this.saving = true;
    this.error = '';
    const request = this.id ? this.products.update(this.id, this.product) : this.products.create(this.product);
    request.subscribe({
      next: product => void this.router.navigate(['/products', product.id]),
      error: () => { this.error = 'Unable to save this product. Check its fields and SKU.'; this.saving = false; }
    });
  }
}
