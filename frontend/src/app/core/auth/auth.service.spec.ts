import { AuthService } from './auth.service';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('stores the access token after a successful session', () => {
    service.storeSession('token-123');
    expect(localStorage.getItem('accessToken')).toBe('token-123');
  });

  it('logs in through the order API and stores its token', () => {
    let role = '';
    service.login('customer@example.com', 'password').subscribe(session => role = session.role);
    const request = http.expectOne(`${environment.api.order}/api/v1/auth/login`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ email: 'customer@example.com', password: 'password' });
    request.flush({ accessToken: 'jwt-123', role: 'CUSTOMER', displayName: 'Customer' });
    expect(role).toBe('CUSTOMER');
    expect(localStorage.getItem('accessToken')).toBe('jwt-123');
  });

  it('registers through the order API and stores its token', () => {
    service.register('new@example.com', 'password', 'New User').subscribe();
    const request = http.expectOne(`${environment.api.order}/api/v1/auth/register`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ email: 'new@example.com', password: 'password', displayName: 'New User' });
    request.flush({ accessToken: 'jwt-456', role: 'CUSTOMER', displayName: 'New User' });
    expect(localStorage.getItem('accessToken')).toBe('jwt-456');
  });
});
