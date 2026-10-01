import { HttpInterceptorFn } from '@angular/common/http';
export const authTokenInterceptor: HttpInterceptorFn = (request, next) => {
  const token = localStorage.getItem('accessToken');
  return token && request.url.startsWith('/api/') ? next(request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })) : next(request);
};
