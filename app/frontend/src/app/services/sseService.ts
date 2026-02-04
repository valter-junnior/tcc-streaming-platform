import { API_BASE_URL } from "../config/env";
import { logger } from "../../shared/lib/logger";

type StreamStatusCallback = (data: StreamStatusMessage) => void;
type ViewersUpdateCallback = (data: ViewersUpdateMessage) => void;

interface StreamStatusMessage {
  type: string;
  status: string;
}

interface ViewersUpdateMessage {
  currentViewers: number;
  viewersPeak: number;
}

interface Subscription {
  streamId: string;
  viewerId: string;
  eventSource: EventSource;
  statusCallbacks: StreamStatusCallback[];
  viewersCallbacks: ViewersUpdateCallback[];
  reconnectAttempts: number;
}

/**
 * Service para gerenciar conexões SSE (Server-Sent Events)
 * Substitui o WebSocket para notificações em tempo real
 */
class SseService {
  private subscriptions: Map<string, Subscription> = new Map();

  /**
   * Conecta a uma stream para receber notificações em tempo real
   * NÃO incrementa viewers - isso é feito via viewerService.joinStream()
   */
  subscribe(
    streamId: string,
    viewerId: string,
    onStatusUpdate: StreamStatusCallback,
    onViewersUpdate: ViewersUpdateCallback,
    countAsViewer: boolean = true,
  ): () => void {
    // Se já existe uma subscrição para essa stream, desconectar a antiga primeiro
    const existing = this.subscriptions.get(streamId);
    if (existing) {
      logger.warn(
        "[SSE] Closing existing subscription before creating new one",
        {
          streamId,
          oldViewerId: existing.viewerId,
          newViewerId: viewerId,
        },
      );
      existing.eventSource.close();
      this.subscriptions.delete(streamId);
    }

    // Criar nova conexão SSE
    const url = `${API_BASE_URL}/api/sse/stream/${streamId}/subscribe?viewerId=${viewerId}&countAsViewer=${countAsViewer}`;
    logger.info("[SSE] Subscribing to stream", {
      streamId,
      viewerId,
      countAsViewer,
      url,
    });

    const eventSource = new EventSource(url);

    const subscription: Subscription = {
      streamId,
      viewerId,
      eventSource,
      statusCallbacks: [onStatusUpdate],
      viewersCallbacks: [onViewersUpdate],
      reconnectAttempts: 0,
    };

    this.subscriptions.set(streamId, subscription);

    // Listener para eventos de status da stream
    eventSource.addEventListener("stream_status", (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data) as StreamStatusMessage;
        logger.info("[SSE] ✅ Received stream_status", data);
        subscription.statusCallbacks.forEach((cb) => cb(data));
      } catch (error) {
        logger.error("[SSE] Error parsing stream_status", error);
      }
    });

    // Listener para eventos de atualização de viewers
    eventSource.addEventListener("viewers_update", (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data) as ViewersUpdateMessage;
        logger.info("[SSE] ✅ Received viewers_update", data);
        subscription.viewersCallbacks.forEach((cb) => cb(data));
      } catch (error) {
        logger.error("[SSE] Error parsing viewers_update", error);
      }
    });

    // Listener para conexão aberta
    eventSource.onopen = () => {
      logger.info("[SSE] Connection established", { streamId, viewerId });
    };

    // Listener para erros
    eventSource.onerror = (error) => {
      const currentSubscription = this.subscriptions.get(streamId);
      if (!currentSubscription) return;

      currentSubscription.reconnectAttempts++;

      logger.error("[SSE] Connection error", {
        streamId,
        viewerId,
        error,
        readyState: eventSource.readyState,
        reconnectAttempts: currentSubscription.reconnectAttempts,
      });

      // Se exceder 10 tentativas de reconexão, fechar permanentemente
      if (currentSubscription.reconnectAttempts > 10) {
        logger.error(
          "[SSE] Too many reconnection attempts, closing connection",
          {
            streamId,
            viewerId,
          },
        );
        this.unsubscribe(streamId);
        return;
      }

      // Se a conexão foi fechada pelo servidor (pode ser stream inexistente)
      if (eventSource.readyState === EventSource.CLOSED) {
        logger.warn(
          "[SSE] Connection closed by server (stream may not exist)",
          {
            streamId,
            viewerId,
          },
        );

        // Usar backoff exponencial antes de tentar reconectar
        const backoffMs = Math.min(
          1000 * Math.pow(2, currentSubscription.reconnectAttempts - 1),
          30000,
        );
        logger.info(`[SSE] Will retry after ${backoffMs}ms backoff`);

        setTimeout(() => {
          // Verificar se ainda deve reconectar
          if (this.subscriptions.has(streamId)) {
            logger.info("[SSE] Attempting to reconnect...", {
              streamId,
              viewerId,
            });
            // Reconectar criando nova subscription
            this.subscribe(
              streamId,
              viewerId,
              currentSubscription.statusCallbacks[0],
              currentSubscription.viewersCallbacks[0],
              countAsViewer,
            );
          }
        }, backoffMs);

        this.unsubscribe(streamId);
      }
      // EventSource.CONNECTING (0) significa que está tentando reconectar automaticamente
      // Deixamos o EventSource fazer o retry automático
    };

    // Retorna função para cancelar subscrição
    return () => {
      this.unsubscribe(streamId);
    };
  }

  /**
   * Cancela subscrição de uma stream
   * NÃO decrementa viewers automaticamente - isso é feito via viewerService.leaveStream()
   * ou pelo callback de desconexão do SSE no backend
   */
  unsubscribe(streamId: string): void {
    const subscription = this.subscriptions.get(streamId);
    if (subscription) {
      logger.info("[SSE] Unsubscribing from stream", {
        streamId,
        viewerId: subscription.viewerId,
      });

      subscription.eventSource.close();
      this.subscriptions.delete(streamId);
    }
  }

  /**
   * Cancela todas as subscrições ativas
   */
  unsubscribeAll(): void {
    logger.info("[SSE] Unsubscribing from all streams");
    this.subscriptions.forEach((_, streamId) => {
      this.unsubscribe(streamId);
    });
  }

  /**
   * Verifica se está subscrito a uma stream
   */
  isSubscribed(streamId: string): boolean {
    return this.subscriptions.has(streamId);
  }
}

export const sseService = new SseService();
