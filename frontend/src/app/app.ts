import { Component, inject, OnInit, signal } from '@angular/core';
import { AuthService } from './services/auth.service';
import { AuthComponent } from './auth/auth';
import { DashboardComponent } from './dashboard/dashboard';

@Component({
  selector: 'app-root',
  imports: [AuthComponent, DashboardComponent],
  template: `
    @if (loggedIn()) {
      <app-dashboard />
    } @else {
      <app-auth (loggedIn)="loggedIn.set(true)" />
    }
  `
})
export class App implements OnInit {
  private auth = inject(AuthService);
  loggedIn = signal(false);

  ngOnInit() {
    this.loggedIn.set(this.auth.isLoggedIn);
  }
}
