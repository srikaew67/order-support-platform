import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { AuthPage } from './core/auth/auth-page';

export const routes: Routes = [
  { path: 'login', component: AuthPage, data: { mode: 'login' } },
  { path: 'register', component: AuthPage, data: { mode: 'register' } },
  { path: 'dashboard', canActivate: [authGuard], loadComponent: () => import('./dashboard').then(m => m.Dashboard) },
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: '**', redirectTo: 'login' }
];
