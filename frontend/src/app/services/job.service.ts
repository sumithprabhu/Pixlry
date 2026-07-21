/**
 * FILE: job.service.ts
 * PURPOSE: REST client for job-service, analytics-service, and notification-service
 *          endpoints — all routed through the API Gateway at port 8080.
 *
 * BEARER TOKEN HEADER:
 *   Every protected request adds "Authorization: Bearer <token>".
 *   The gateway validates this token, extracts userId/role, and forwards
 *   X-User-Id / X-User-Role headers to the downstream service.
 *   Downstream services read X-User-Id from the request header instead of
 *   decoding the JWT themselves — this is the "token relay" pattern.
 *
 * MULTIPART UPLOAD (uploadJob):
 *   FormData is the browser's way of sending multipart/form-data.
 *   We append the File object directly — the browser handles encoding.
 *   The 'parameters' field is a JSON string that the server parses into
 *   Map<String, String> (width, height for resize).
 *   We do NOT set Content-Type manually — the browser adds the boundary
 *   string automatically when it sees FormData.
 */
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
