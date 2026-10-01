import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { inject } from '@angular/core';
import { tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AuthSession {
  accessToken: string;
  role: 'CUSTOMER' | 'SUPPORT' | 'ADMIN';
  displayName: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly authUrl = `${environment.api.order}/api/v1/auth`;

  login(email: string, password: string) {
    return this.http.post<AuthSession>(`${this.authUrl}/login`, { email, password })
      .pipe(tap(session => this.storeSession(session.accessToken)));
  }

  register(email: string, password: string, displayName: string) {
    return this.http.post<AuthSession>(`${this.authUrl}/register`, { email, password, displayName })
      .pipe(tap(session => this.storeSession(session.accessToken)));
  }

  storeSession(accessToken: string): void { localStorage.setItem('accessToken', accessToken); }
  clearSession(): void { localStorage.removeItem('accessToken'); }
  isAuthenticated(): boolean { return Boolean(localStorage.getItem('accessToken')); }
}
