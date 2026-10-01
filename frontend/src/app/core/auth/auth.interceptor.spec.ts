import { HttpRequest, HttpResponse } from '@angular/common/http';
import { of } from 'rxjs';
import { authTokenInterceptor } from './auth.interceptor';
import { environment } from '../../../environments/environment';

describe('authTokenInterceptor', () => {
  it('adds the stored bearer token to API requests', (done) => {
    localStorage.setItem('accessToken', 'token-123');
    authTokenInterceptor(new HttpRequest('GET', `${environment.api.order}/api/v1/orders`), (request) => {
      expect(request.headers.get('Authorization')).toBe('Bearer token-123');
      return of(new HttpResponse({ status: 200 }));
    }).subscribe(() => done());
  });
  it('leaves unrelated external requests without the token', (done) => {
    localStorage.setItem('accessToken', 'token-123');
    authTokenInterceptor(new HttpRequest('GET', 'https://example.com/data'), (request) => {
      expect(request.headers.has('Authorization')).toBeFalse();
      return of(new HttpResponse({ status: 200 }));
    }).subscribe(() => done());
  });
});
