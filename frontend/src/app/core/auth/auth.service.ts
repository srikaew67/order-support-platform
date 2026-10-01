import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AuthService {
  storeSession(accessToken: string): void { localStorage.setItem('accessToken', accessToken); }
  clearSession(): void { localStorage.removeItem('accessToken'); }
  isAuthenticated(): boolean { return Boolean(localStorage.getItem('accessToken')); }
}
