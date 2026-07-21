/**
 * FILE: auth.service.ts
 * PURPOSE: Manages JWT tokens — stores them in localStorage, exposes the current
 *          token as an Observable, and provides login/register/logout methods.
 *
 * TOKEN STORAGE IN localStorage:
 *   localStorage is simple but has trade-offs:
 *   - Accessible to any JS on the page (XSS risk).
 *   - Alternative: HttpOnly cookie (immune to XSS but needs CSRF protection).
 *   For this learning project, localStorage is fine. Production apps that handle
 *   sensitive data should use HttpOnly cookies.
 *
 * BehaviorSubject for token$:
 *   BehaviorSubject always emits the LAST value to new subscribers.
 *   Components subscribing to token$ immediately get the current login state
 *   without waiting for the next event. Perfect for "am I logged in?" checks.
 *
 * userId from JWT claims:
 *   We decode the JWT payload (base64url middle segment) client-side to extract
 *   the userId ("sub" claim). We do NOT verify the signature here — the gateway
 *   already verified it. We just need the claim value.
 */
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, tap } from 'rxjs';

const API = 'http://localhost:8080/api/auth';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);

  private _token = new BehaviorSubject<string | null>(localStorage.getItem('access_token'));
  token$ = this._token.asObservable();

  get token() { return this._token.value; }
  get userId(): string | null {
    const t = this.token;
    if (!t) return null;
    try {
      return JSON.parse(atob(t.split('.')[1])).sub;
    } catch { return null; }
  }
  get isLoggedIn() { return !!this._token.value; }

  register(email: string, password: string, firstName: string, lastName: string) {
    return this.http.post<any>(`${API}/register`, { email, password, firstName, lastName })
      .pipe(tap(r => this.storeTokens(r.data)));
  }

  login(email: string, password: string) {
    return this.http.post<any>(`${API}/login`, { email, password })
      .pipe(tap(r => this.storeTokens(r.data)));
  }

  logout() {
    const token = this.token;
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    this._token.next(null);
    if (token) {
      this.http.post(`${API}/logout`, {}, {
        headers: { Authorization: `Bearer ${token}` }
      }).subscribe({ error: () => {} });
    }
  }

  private storeTokens(data: any) {
    localStorage.setItem('access_token', data.accessToken);
    localStorage.setItem('refresh_token', data.refreshToken);
    this._token.next(data.accessToken);
  }
}
