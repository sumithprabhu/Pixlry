import { Component, inject, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-auth',
  imports: [FormsModule],
  template: `
    <div class="auth-wrap">
      <div class="auth-card">
        <h1 class="logo">Pixlry</h1>
        <p class="tagline">Image processing platform</p>

        <div class="tab-row">
          <button [class.active]="mode() === 'login'" (click)="mode.set('login')">Login</button>
          <button [class.active]="mode() === 'register'" (click)="mode.set('register')">Register</button>
        </div>

        @if (mode() === 'register') {
          <div class="row2">
            <input [(ngModel)]="firstName" placeholder="First name" />
            <input [(ngModel)]="lastName" placeholder="Last name" />
          </div>
        }
        <input [(ngModel)]="email" type="email" placeholder="Email" />
        <input [(ngModel)]="password" type="password" placeholder="Password" />

        @if (error()) {
          <div class="err">{{ error() }}</div>
        }

        <button class="btn-primary" (click)="submit()" [disabled]="loading()">
          {{ loading() ? 'Please wait…' : (mode() === 'login' ? 'Login' : 'Create account') }}
        </button>
      </div>
    </div>
  `,
  styles: [`
    .auth-wrap { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #0f0f0f; }
    .auth-card { background: #1a1a1a; border: 1px solid #2a2a2a; border-radius: 12px; padding: 40px; width: 360px; display: flex; flex-direction: column; gap: 14px; }
    .logo { margin: 0; font-size: 28px; font-weight: 700; color: #fff; letter-spacing: -1px; }
    .tagline { margin: 0; color: #666; font-size: 13px; }
    .tab-row { display: flex; gap: 8px; }
    .tab-row button { flex: 1; padding: 8px; background: #2a2a2a; border: 1px solid #333; border-radius: 6px; color: #888; cursor: pointer; font-size: 14px; }
    .tab-row button.active { background: #fff; color: #000; border-color: #fff; font-weight: 600; }
    .row2 { display: flex; gap: 8px; }
    .row2 input { flex: 1; }
    input { padding: 10px 12px; background: #252525; border: 1px solid #333; border-radius: 6px; color: #fff; font-size: 14px; width: 100%; box-sizing: border-box; }
    input:focus { outline: none; border-color: #555; }
    .err { color: #f87171; font-size: 13px; }
    .btn-primary { padding: 11px; background: #fff; color: #000; border: none; border-radius: 6px; font-weight: 600; font-size: 15px; cursor: pointer; }
    .btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
  `]
})
export class AuthComponent {
  loggedIn = output();
  private authSvc = inject(AuthService);

  mode = signal<'login' | 'register'>('login');
  email = ''; password = ''; firstName = ''; lastName = '';
  loading = signal(false);
  error = signal('');

  submit() {
    this.error.set('');
    this.loading.set(true);
    const obs = this.mode() === 'login'
      ? this.authSvc.login(this.email, this.password)
      : this.authSvc.register(this.email, this.password, this.firstName, this.lastName);

    obs.subscribe({
      next: () => { this.loading.set(false); this.loggedIn.emit(); },
      error: (e) => {
        this.loading.set(false);
        this.error.set(e.error?.error || e.error?.message || 'Something went wrong');
      }
    });
  }
}
