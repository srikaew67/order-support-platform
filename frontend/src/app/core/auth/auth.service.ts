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
      .pipe(tap(session => this.storeSession(session.accessToken, session.role)));
  }

  register(email: string, password: string, displayName: string) {
    return this.http.post<AuthSession>(`${this.authUrl}/register`, { email, password, displayName })
      .pipe(tap(session => this.storeSession(session.accessToken, session.role)));
  }

  storeSession(accessToken: string, role?: AuthSession['role']): void {
    localStorage.setItem('accessToken', accessToken);
    if (role) localStorage.setItem('role', role);
  }
  clearSession(): void { localStorage.removeItem('accessToken'); localStorage.removeItem('role'); }
  isAuthenticated(): boolean { return Boolean(localStorage.getItem('accessToken')); }
  role(): AuthSession['role'] | null {
    const role = localStorage.getItem('role');
    return role === 'CUSTOMER' || role === 'SUPPORT' || role === 'ADMIN' ? role : null;
  }
}
