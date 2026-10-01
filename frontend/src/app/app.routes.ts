import { Routes } from '@angular/router';
import { adminGuard, authGuard, customerGuard } from './core/auth/auth.guard';
import { AuthPage } from './core/auth/auth-page';
import { ProductListPage } from './features/products/product-list-page';
import { ProductDetailPage } from './features/products/product-detail-page';
import { ProductFormPage } from './features/products/product-form-page';
import { OrderListPage } from './features/orders/order-list-page';
import { OrderCreatePage } from './features/orders/order-create-page';
import { OrderDetailPage } from './features/orders/order-detail-page';

export const routes: Routes = [
  { path: 'login', component: AuthPage, data: { mode: 'login' } },
  { path: 'register', component: AuthPage, data: { mode: 'register' } },
  { path: 'dashboard', canActivate: [authGuard], loadComponent: () => import('./dashboard').then(m => m.Dashboard) },
  { path: 'products', component: ProductListPage },
  { path: 'products/new', component: ProductFormPage, canActivate: [adminGuard] },
  { path: 'products/:id/edit', component: ProductFormPage, canActivate: [adminGuard] },
  { path: 'products/:id', component: ProductDetailPage },
  { path: 'orders', component: OrderListPage, canActivate: [authGuard] },
  { path: 'orders/new', component: OrderCreatePage, canActivate: [customerGuard] },
  { path: 'orders/:id', component: OrderDetailPage, canActivate: [authGuard] },
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: '**', redirectTo: 'login' }
];
