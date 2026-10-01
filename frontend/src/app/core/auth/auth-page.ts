import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-auth-page',
  imports: [FormsModule, RouterLink],
  template: `
    <section class="auth-card">
      <h2>{{ isRegister ? 'Create account' : 'Sign in' }}</h2>
      <form (ngSubmit)="submit()" #authForm="ngForm">
        @if (isRegister) {
          <label>Display name <input name="displayName" [(ngModel)]="displayName" required maxlength="120" autocomplete="name" /></label>
        }
        <label>Email <input name="email" [(ngModel)]="email" type="email" required email autocomplete="email" /></label>
        <label>Password <input name="password" [(ngModel)]="password" type="password" required [minlength]="isRegister ? 8 : 1" [attr.autocomplete]="isRegister ? 'new-password' : 'current-password'" /></label>
        @if (error) { <p role="alert">{{ error }}</p> }
        <button type="submit" [disabled]="authForm.invalid || loading">{{ loading ? 'Please wait…' : isRegister ? 'Create account' : 'Sign in' }}</button>
      </form>
      @if (isRegister) { <p>Already have an account? <a routerLink="/login">Sign in</a></p> }
      @else { <p>New here? <a routerLink="/register">Create an account</a></p> }
    </section>
  `,
  styles: [`
    .auth-card { max-width: 26rem; margin: 3rem auto; padding: 2rem; background: white; border: 1px solid #ddd; border-radius: .75rem; }
    form, label { display: grid; gap: .5rem; }
    form { gap: 1rem; }
    input { padding: .7rem; border: 1px solid #aaa; border-radius: .3rem; font: inherit; }
    button { padding: .75rem; font: inherit; cursor: pointer; }
    [role=alert] { color: #a42121; }
  `]
})
export class AuthPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  readonly isRegister = this.route.snapshot.data['mode'] === 'register';
  email = '';
  password = '';
  displayName = '';
  loading = false;
  error = '';

  submit(): void {
    if (this.loading) return;
    this.loading = true;
    this.error = '';
    const request = this.isRegister
      ? this.auth.register(this.email, this.password, this.displayName)
      : this.auth.login(this.email, this.password);
    request.subscribe({
      next: () => void this.router.navigateByUrl('/dashboard'),
      error: (failure: HttpErrorResponse) => {
        this.error = failure.error?.message || 'Unable to complete your request. Please try again.';
        this.loading = false;
      }
    });
  }
}
