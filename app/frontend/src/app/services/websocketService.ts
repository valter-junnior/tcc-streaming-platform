import { Client, type IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { WS_URL } from "../config/env";
import type { WebSocketMessage } from "../types/stream";

type MessageCallback = (message: WebSocketMessage) => void;

class WebSocketService {
  private client: Client | null = null;
  private subscribers: Map<string, MessageCallback[]> = new Map();
  private connected = false;
  private connectionRefCount = 0;

  connect(): Promise<void> {
    this.connectionRefCount++;

    // Se já está conectado, retornar promise resolvida
    if (this.client?.connected) {
      return Promise.resolve();
    }

    // Se já há conexão em andamento, aguardar
    if (this.connected) {
      return Promise.resolve();
    }

    return new Promise((resolve, reject) => {
      this.client = new Client({
        webSocketFactory: () => new SockJS(WS_URL),
        debug: (str) => {
          if (import.meta.env.DEV) {
            console.log("[WebSocket]", str);
          }
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          console.log("WebSocket connected");
          this.connected = true;
          resolve();
        },
        onStompError: (frame) => {
          console.error("WebSocket error:", frame);
          reject(new Error(frame.headers["message"]));
        },
        onWebSocketClose: () => {
          console.log("WebSocket disconnected");
          this.connected = false;
        },
      });

      this.client.activate();
    });
  }

  disconnect(): void {
    this.connectionRefCount--;

    // Só desconectar quando não houver mais referências
    if (this.connectionRefCount <= 0) {
      this.connectionRefCount = 0;

      if (this.client) {
        console.log("WebSocket disconnecting (ref count reached 0)");
        this.client.deactivate();
        this.connected = false;
        this.subscribers.clear();
      }
    } else {
      console.log(
        `WebSocket kept alive (ref count: ${this.connectionRefCount})`,
      );
    }
  }

  subscribeToStreamStatus(
    streamId: string,
    callback: MessageCallback,
  ): () => void {
    if (!this.client || !this.connected) {
      console.warn("WebSocket not connected. Call connect() first.");
      return () => {};
    }

    const topic = `/topic/stream/${streamId}/status`;
    const subscription = this.client.subscribe(topic, (message: IMessage) => {
      try {
        const data = JSON.parse(message.body);
        callback(data);
      } catch (error) {
        console.error("Error parsing WebSocket message:", error);
      }
    });

    // Store callback for cleanup
    if (!this.subscribers.has(topic)) {
      this.subscribers.set(topic, []);
    }
    this.subscribers.get(topic)!.push(callback);

    // Return unsubscribe function
    return () => {
      subscription.unsubscribe();
      const callbacks = this.subscribers.get(topic) || [];
      const index = callbacks.indexOf(callback);
      if (index > -1) {
        callbacks.splice(index, 1);
      }
    };
  }

  subscribeToStreamViewers(
    streamId: string,
    callback: MessageCallback,
  ): () => void {
    if (!this.client || !this.connected) {
      console.warn("WebSocket not connected. Call connect() first.");
      return () => {};
    }

    const topic = `/topic/stream/${streamId}/viewers`;
    const subscription = this.client.subscribe(topic, (message: IMessage) => {
      try {
        const data = JSON.parse(message.body);
        callback(data);
      } catch (error) {
        console.error("Error parsing WebSocket message:", error);
      }
    });

    // Store callback for cleanup
    if (!this.subscribers.has(topic)) {
      this.subscribers.set(topic, []);
    }
    this.subscribers.get(topic)!.push(callback);

    // Return unsubscribe function
    return () => {
      subscription.unsubscribe();
      const callbacks = this.subscribers.get(topic) || [];
      const index = callbacks.indexOf(callback);
      if (index > -1) {
        callbacks.splice(index, 1);
      }
    };
  }

  sendViewerJoined(streamId: string, viewerId: string): void {
    if (!this.client || !this.connected) {
      console.warn("WebSocket not connected");
      return;
    }

    this.client.publish({
      destination: `/app/stream/${streamId}/join`,
      body: JSON.stringify({ viewerId }),
    });
  }

  sendViewerLeft(streamId: string, viewerId: string): void {
    if (!this.client || !this.connected) {
      console.warn("WebSocket not connected");
      return;
    }

    this.client.publish({
      destination: `/app/stream/${streamId}/leave`,
      body: JSON.stringify({ viewerId }),
    });
  }

  isConnected(): boolean {
    return this.connected;
  }
}

export const websocketService = new WebSocketService();
