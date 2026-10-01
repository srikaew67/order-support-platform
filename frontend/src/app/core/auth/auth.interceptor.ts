import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '../../../environments/environment';
export const authTokenInterceptor: HttpInterceptorFn = (request, next) => {
  const token = localStorage.getItem('accessToken');
  const apiOrigins = [environment.api.order, environment.api.support, environment.api.notification];
  const isApi = request.url.startsWith('/api/') || apiOrigins.some(origin => request.url.startsWith(`${origin}/api/`));
  return token && isApi ? next(request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })) : next(request);
};
