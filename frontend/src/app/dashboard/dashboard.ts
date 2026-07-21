import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SlicePipe } from '@angular/common';
import { AuthService } from '../services/auth.service';
import { JobService } from '../services/job.service';
import { WsService } from '../services/ws.service';
import { Subscription } from 'rxjs';

type JobStatus = 'PENDING' | 'QUEUED' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

interface Job {
  id: string;
  originalFileName: string;
  operationType: string;
  status: JobStatus;
  outputFilePath: string | null;
  processingTimeMs: number | null;
  errorMessage: string | null;
  createdAt: string;
}

@Component({
  selector: 'app-dashboard',
  imports: [FormsModule, SlicePipe],
  template: `
    <div class="layout">
      <!-- Sidebar -->
      <aside>
        <div class="logo">Pixlry</div>

        <div class="stats">
          <div class="stat">
            <span class="stat-val">{{ stats().totalJobs ?? '—' }}</span>
            <span class="stat-label">Total</span>
          </div>
          <div class="stat">
            <span class="stat-val green">{{ stats().completedJobs ?? '—' }}</span>
            <span class="stat-label">Done</span>
          </div>
          <div class="stat">
            <span class="stat-val red">{{ stats().failedJobs ?? '—' }}</span>
            <span class="stat-label">Failed</span>
          </div>
          <div class="stat">
            <span class="stat-val">{{ stats().avgProcessingTimeMs ? stats().avgProcessingTimeMs + 'ms' : '—' }}</span>
            <span class="stat-label">Avg time</span>
          </div>
        </div>

        @if (unreadCount() > 0) {
          <div class="notif-badge">{{ unreadCount() }} new notification{{ unreadCount() > 1 ? 's' : '' }}</div>
        }

        <button class="logout-btn" (click)="logout()">Logout</button>
      </aside>

      <!-- Main -->
      <main>
        <!-- Upload Card -->
        <div class="card upload-card">
          <h2>Upload Image</h2>

          <div class="drop-zone"
               [class.has-file]="selectedFile()"
               (click)="fileInput.click()"
               (dragover)="$event.preventDefault()"
               (drop)="onDrop($event)">
            @if (selectedFile()) {
              <span>📎 {{ selectedFile()!.name }}</span>
            } @else {
              <span>Click or drag an image here</span>
            }
          </div>
          <input #fileInput type="file" accept="image/*" hidden (change)="onFileChange($event)" />

          <div class="row2">
            <div class="field">
              <label>Operation</label>
              <select [(ngModel)]="operation">
                <option value="RESIZE">Resize</option>
              </select>
            </div>
            <div class="field">
              <label>Width px</label>
              <input type="number" [(ngModel)]="width" placeholder="800" />
            </div>
            <div class="field">
              <label>Height px</label>
              <input type="number" [(ngModel)]="height" placeholder="600" />
            </div>
          </div>

          @if (uploadError()) {
            <div class="err">{{ uploadError() }}</div>
          }

          <button class="btn-primary" (click)="upload()" [disabled]="!selectedFile() || uploading()">
            {{ uploading() ? 'Uploading…' : 'Process image' }}
          </button>
        </div>

        <!-- Job List -->
        <div class="card">
          <div class="jobs-header">
            <h2>Jobs</h2>
            <button class="refresh-btn" (click)="loadJobs()">↻ Refresh</button>
          </div>

          @if (jobs().length === 0) {
            <p class="empty">No jobs yet. Upload an image to get started.</p>
          }

          <div class="job-list">
            @for (job of jobs(); track job.id) {
              <div class="job-row" [class]="'status-' + job.status.toLowerCase()">
                <div class="job-info">
                  <span class="job-name">{{ job.originalFileName }}</span>
                  <span class="job-meta">{{ job.operationType }} · {{ job.createdAt | slice:0:10 }}</span>
                </div>
                <div class="job-right">
                  @if (job.processingTimeMs) {
                    <span class="job-time">{{ job.processingTimeMs }}ms</span>
                  }
                  <span class="badge badge-{{ job.status.toLowerCase() }}">{{ job.status }}</span>
                </div>
              </div>
            }
          </div>
        </div>
      </main>
    </div>
  `,
  styles: [`
    :host { display: block; min-height: 100vh; background: #0f0f0f; color: #fff; font-family: -apple-system, sans-serif; }
    .layout { display: flex; min-height: 100vh; }

    aside { width: 220px; background: #1a1a1a; border-right: 1px solid #2a2a2a; padding: 28px 20px; display: flex; flex-direction: column; gap: 24px; flex-shrink: 0; }
    .logo { font-size: 22px; font-weight: 700; letter-spacing: -0.5px; }

    .stats { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
    .stat { background: #252525; border-radius: 8px; padding: 10px; display: flex; flex-direction: column; gap: 2px; }
    .stat-val { font-size: 20px; font-weight: 700; }
    .stat-label { font-size: 11px; color: #666; text-transform: uppercase; letter-spacing: 0.5px; }
    .green { color: #4ade80; }
    .red { color: #f87171; }

    .notif-badge { background: #854d0e; color: #fde68a; font-size: 12px; padding: 6px 10px; border-radius: 6px; }
    .logout-btn { margin-top: auto; background: transparent; border: 1px solid #333; color: #888; padding: 8px; border-radius: 6px; cursor: pointer; font-size: 13px; }
    .logout-btn:hover { color: #fff; border-color: #555; }

    main { flex: 1; padding: 32px; display: flex; flex-direction: column; gap: 24px; overflow-y: auto; }

    .card { background: #1a1a1a; border: 1px solid #2a2a2a; border-radius: 12px; padding: 24px; }
    .card h2 { margin: 0 0 20px; font-size: 16px; font-weight: 600; color: #ccc; }

    .drop-zone { border: 2px dashed #333; border-radius: 8px; padding: 32px; text-align: center; cursor: pointer; color: #666; font-size: 14px; transition: all .15s; }
    .drop-zone:hover, .drop-zone.has-file { border-color: #555; color: #aaa; background: #252525; }

    .row2 { display: flex; gap: 12px; margin-top: 14px; flex-wrap: wrap; }
    .field { display: flex; flex-direction: column; gap: 5px; flex: 1; min-width: 100px; }
    label { font-size: 12px; color: #666; }
    select, input[type=number] { background: #252525; border: 1px solid #333; border-radius: 6px; color: #fff; padding: 8px 10px; font-size: 14px; }
    select:focus, input[type=number]:focus { outline: none; border-color: #555; }

    .err { color: #f87171; font-size: 13px; margin-top: 8px; }
    .btn-primary { margin-top: 16px; padding: 10px 20px; background: #fff; color: #000; border: none; border-radius: 6px; font-weight: 600; font-size: 14px; cursor: pointer; }
    .btn-primary:disabled { opacity: 0.4; cursor: not-allowed; }

    .jobs-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .jobs-header h2 { margin: 0; }
    .refresh-btn { background: transparent; border: 1px solid #333; color: #888; padding: 5px 10px; border-radius: 6px; cursor: pointer; font-size: 13px; }
    .refresh-btn:hover { color: #fff; }

    .empty { color: #555; font-size: 14px; text-align: center; padding: 32px 0; }
    .job-list { display: flex; flex-direction: column; gap: 8px; }
    .job-row { display: flex; justify-content: space-between; align-items: center; padding: 12px 14px; background: #252525; border-radius: 8px; border-left: 3px solid #333; }
    .job-row.status-completed { border-left-color: #4ade80; }
    .job-row.status-failed { border-left-color: #f87171; }
    .job-row.status-processing { border-left-color: #60a5fa; }
    .job-row.status-queued { border-left-color: #facc15; }

    .job-info { display: flex; flex-direction: column; gap: 3px; }
    .job-name { font-size: 14px; font-weight: 500; }
    .job-meta { font-size: 12px; color: #666; }
    .job-right { display: flex; align-items: center; gap: 10px; }
    .job-time { font-size: 12px; color: #666; }

    .badge { font-size: 11px; padding: 3px 8px; border-radius: 4px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }
    .badge-completed { background: #14532d; color: #4ade80; }
    .badge-failed { background: #450a0a; color: #f87171; }
    .badge-processing { background: #1e3a5f; color: #60a5fa; }
    .badge-queued { background: #422006; color: #facc15; }
    .badge-pending { background: #1c1917; color: #a8a29e; }
  `]
})
export class DashboardComponent implements OnInit, OnDestroy {
  private auth = inject(AuthService);
  private jobSvc = inject(JobService);
  private ws = inject(WsService);
  private wsSub?: Subscription;

  jobs = signal<Job[]>([]);
  stats = signal<any>({});
  unreadCount = signal(0);

  selectedFile = signal<File | null>(null);
  operation = 'RESIZE';
  width = 800;
  height = 600;
  uploading = signal(false);
  uploadError = signal('');

  ngOnInit() {
    this.loadJobs();
    this.loadStats();
    this.loadNotifications();

    const userId = this.auth.userId;
    if (userId) {
      this.ws.connect(userId);
      this.wsSub = this.ws.jobUpdate$.subscribe(event => {
        this.onWsUpdate(event);
      });
    }
  }

  ngOnDestroy() {
    this.ws.disconnect();
    this.wsSub?.unsubscribe();
  }

  onFileChange(e: Event) {
    const f = (e.target as HTMLInputElement).files?.[0];
    if (f) this.selectedFile.set(f);
  }

  onDrop(e: DragEvent) {
    e.preventDefault();
    const f = e.dataTransfer?.files?.[0];
    if (f) this.selectedFile.set(f);
  }

  upload() {
    if (!this.selectedFile()) return;
    this.uploading.set(true);
    this.uploadError.set('');

    this.jobSvc.uploadJob(this.selectedFile()!, this.operation, {
      width: String(this.width),
      height: String(this.height)
    }).subscribe({
      next: (r) => {
        this.uploading.set(false);
        this.selectedFile.set(null);
        this.jobs.update(jobs => [r.data, ...jobs]);
      },
      error: (e) => {
        this.uploading.set(false);
        this.uploadError.set(e.error?.error || 'Upload failed');
      }
    });
  }

  loadJobs() {
    this.jobSvc.listJobs().subscribe({
      next: (r) => this.jobs.set(r.data?.content ?? []),
      error: () => {}
    });
  }

  loadStats() {
    this.jobSvc.getAnalytics().subscribe({
      next: (r) => this.stats.set(r.data ?? {}),
      error: () => {}
    });
  }

  loadNotifications() {
    this.jobSvc.getNotifications().subscribe({
      next: (r) => {
        const unread = (r.data ?? []).filter((n: any) => !n.read).length;
        this.unreadCount.set(unread);
      },
      error: () => {}
    });
  }

  onWsUpdate(event: any) {
    this.jobs.update(jobs =>
      jobs.map(j => j.id === event.jobId
        ? { ...j, status: event.status, outputFilePath: event.outputFilePath, processingTimeMs: event.processingTimeMs }
        : j
      )
    );
    if (event.status === 'COMPLETED' || event.status === 'FAILED') {
      this.unreadCount.update(n => n + 1);
      this.loadStats();
    }
  }

  logout() {
    this.ws.disconnect();
    this.auth.logout();
  }
}
