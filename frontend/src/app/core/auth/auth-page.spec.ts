import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { AuthPage } from './auth-page';
import { environment } from '../../../environments/environment';

describe('AuthPage', () => {
  let http: HttpTestingController;

  afterEach(() => http?.verify());

  it('submits a registration and stores the returned session', () => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [AuthPage],
      providers: [
        provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { data: { mode: 'register' } } } }
      ]
    });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(AuthPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Create account');
    const page = fixture.componentInstance;
    page.email = 'new@example.com';
    page.password = 'SecurePass123!';
    page.displayName = 'New User';
    page.submit();
    const request = http.expectOne(`${environment.api.order}/api/v1/auth/register`);
    request.flush({ accessToken: 'registered-token', role: 'CUSTOMER', displayName: 'New User' });
    expect(localStorage.getItem('accessToken')).toBe('registered-token');
  });
});
