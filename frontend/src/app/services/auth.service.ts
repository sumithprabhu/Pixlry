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
