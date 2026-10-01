import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from './core/auth/auth.service';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink],
  template: `<section><h2>Dashboard</h2><p>You are signed in.</p><a routerLink="/products">Browse products</a> <button type="button" (click)="logout()">Sign out</button></section>`
})
export class Dashboard {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  logout(): void {
    this.auth.clearSession();
    void this.router.navigateByUrl('/login');
  }
}
