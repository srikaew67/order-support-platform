import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { authGuard, adminGuard, customerGuard } from './auth.guard';
import { provideHttpClient } from '@angular/common/http';

describe('authGuard', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient()] });
  });

  it('redirects unauthenticated users to login', () => {
    const result = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));
    expect(result).toEqual(TestBed.inject(Router).createUrlTree(['/login']));
  });

  it('allows authenticated users', () => {
    localStorage.setItem('accessToken', 'jwt-123');
    expect(TestBed.runInInjectionContext(() => authGuard({} as never, {} as never))).toBeTrue();
  });

  it('blocks non-admin users from product editing routes', () => {
    localStorage.setItem('accessToken', 'jwt-123');
    localStorage.setItem('role', 'CUSTOMER');
    const result = TestBed.runInInjectionContext(() => adminGuard({} as never, {} as never));
    expect(result).toEqual(TestBed.inject(Router).createUrlTree(['/products']));
  });

  it('blocks support users from the order creation route', () => {
    localStorage.setItem('accessToken', 'jwt-123');
    localStorage.setItem('role', 'SUPPORT');
    const result = TestBed.runInInjectionContext(() => customerGuard({} as never, {} as never));
    expect(result).toEqual(TestBed.inject(Router).createUrlTree(['/orders']));
  });
});
