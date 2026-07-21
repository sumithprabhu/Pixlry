/**
 * FILE: ws.service.ts
 * PURPOSE: Manages the STOMP-over-WebSocket connection to notification-service.
 *
 * TRANSPORT STACK:
 *   Browser → SockJS → notification-service:8085/ws → Spring STOMP broker
 *
 *   SockJS is a compatibility layer — it tries WebSocket first, then falls back
 *   to long-polling or XHR-streaming if WebSocket is blocked. This makes it work
 *   behind corporate proxies that strip WebSocket upgrade headers.
 *
 *   STOMP is a simple messaging protocol on top of WebSocket. It adds:
 *   - SUBSCRIBE / SEND framing
 *   - Destination-based routing (/queue/job-updates, /topic/global)
 *   - Per-user private channels (/user/{id}/queue/...)
 *
 * SUBSCRIPTION DESTINATION:
 *   We subscribe to /user/{userId}/queue/job-updates.
 *   notification-service sends via convertAndSendToUser(userId, "/queue/job-updates", event).
 *   Spring maps this to the per-user destination automatically.
 *   Only the browser session authenticated as userId receives the message.
 *
 * jobUpdate$ Subject:
 *   RxJS Subject is a multicast event emitter. Dashboard subscribes to it and
 *   updates the job row in-place when a WebSocket event arrives.
 *
 * require() vs import:
 *   sockjs-client is a CommonJS module. Using require() inside connect() (lazy)
 *   avoids Angular's tree-shaker warning about non-ESM modules at load time.
 */
import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';

@Injectable({ providedIn: 'root' })
export class WsService {
  private client: Client | null = null;
  jobUpdate$ = new Subject<any>();

  connect(userId: string) {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const SockJS = require('sockjs-client');
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
