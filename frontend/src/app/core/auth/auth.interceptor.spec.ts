import { HttpRequest, HttpResponse } from '@angular/common/http';
import { of } from 'rxjs';
import { authTokenInterceptor } from './auth.interceptor';

describe('authTokenInterceptor', () => {
  it('adds the stored bearer token to API requests', (done) => {
    localStorage.setItem('accessToken', 'token-123');
    authTokenInterceptor(new HttpRequest('GET', '/api/v1/orders'), (request) => {
      expect(request.headers.get('Authorization')).toBe('Bearer token-123');
      return of(new HttpResponse({ status: 200 }));
    }).subscribe(() => done());
  });
});
