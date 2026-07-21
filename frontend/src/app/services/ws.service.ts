import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

@Injectable({ providedIn: 'root' })
export class WsService {
  private client: Client | null = null;
  jobUpdate$ = new Subject<any>();

  connect(userId: string) {
    this.client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8085/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        this.client!.subscribe(`/user/${userId}/queue/job-updates`, (msg: IMessage) => {
          try { this.jobUpdate$.next(JSON.parse(msg.body)); } catch {}
        });
      }
    });
    this.client.activate();
  }

  disconnect() {
    this.client?.deactivate();
    this.client = null;
  }
}
