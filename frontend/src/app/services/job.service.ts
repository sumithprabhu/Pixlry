import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from './auth.service';

const API = 'http://localhost:8080/api';

@Injectable({ providedIn: 'root' })
export class JobService {
  private http = inject(HttpClient);
  private auth = inject(AuthService);

  private headers() {
    return new HttpHeaders({ Authorization: `Bearer ${this.auth.token}` });
  }

  uploadJob(file: File, operation: string, params: Record<string, string>) {
    const form = new FormData();
    form.append('file', file);
    form.append('operation', operation);
    form.append('parameters', JSON.stringify(params));
    return this.http.post<any>(`${API}/jobs`, form, { headers: this.headers() });
  }

  listJobs(page = 0, size = 20) {
    return this.http.get<any>(`${API}/jobs?page=${page}&size=${size}`,
      { headers: this.headers() });
  }

  getAnalytics() {
    return this.http.get<any>(`${API}/analytics/summary`,
      { headers: this.headers() });
  }

  getNotifications() {
    return this.http.get<any>(`${API}/notifications`,
      { headers: this.headers() });
  }
}
