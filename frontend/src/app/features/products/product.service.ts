import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../environments/environment';

export interface Product {
  id: string;
  sku: string;
  name: string;
  description: string;
  price: number;
  stockQuantity: number;
  version: number;
}

export type ProductInput = Omit<Product, 'id' | 'version'> & { version?: number };

export interface ProductPage {
  content: Product[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.api.order}/api/v1/products`;

  list(page = 0, size = 12) {
    return this.http.get<ProductPage>(this.url, { params: { page, size } });
  }

  get(id: string) {
    return this.http.get<Product>(`${this.url}/${id}`);
  }

  create(input: ProductInput) {
    return this.http.post<Product>(this.url, input);
  }

  update(id: string, input: ProductInput) {
    return this.http.put<Product>(`${this.url}/${id}`, input);
  }

  delete(id: string) {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
