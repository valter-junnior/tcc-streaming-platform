import { useEffect, useRef } from "react";
import { viewerService, type ViewerResponse } from "../services/viewerService";
import { logger } from "../../shared/lib/logger";
import { API_BASE_URL } from "../config/env";

/**
 * Hook para gerenciar entrada/saída de viewers em uma stream
 * Automaticamente faz join ao montar e leave ao desmontar
 */
export function useViewerJoinLeave(
  streamId: string | undefined,
  viewerId: string | null,
  countAsViewer: boolean = true,
  onJoinSuccess?: (response: ViewerResponse) => void,
) {
  const hasJoined = useRef(false);
  const isJoining = useRef(false);

  useEffect(() => {
    if (!streamId || !viewerId) {
      return;
    }

    // Proteção contra double-call (React Strict Mode)
    if (isJoining.current || hasJoined.current) {
      return;
    }

    // Join quando montar o componente
    const joinStreamAsync = async () => {
      isJoining.current = true;

      try {
        const response = await viewerService.joinStream(
          streamId,
          viewerId,
          countAsViewer,
        );

        // Sempre marcar como joined, independente do status
        // Backend já gerencia se incrementa ou não o contador
        hasJoined.current = true;

        logger.info("[useViewerJoinLeave] Join response received", {
          streamId,
          viewerId,
          response,
        });

        // Callback com a resposta para atualizar UI
        if (onJoinSuccess) {
          onJoinSuccess(response);
        }
      } catch (error) {
        logger.error("[useViewerJoinLeave] Error joining stream", error);
        hasJoined.current = false; // Permitir retry em caso de erro
      } finally {
        isJoining.current = false;
      }
    };

    joinStreamAsync();

    // Garantir leave ao fechar aba/navegador
    const handleBeforeUnload = () => {
      if (hasJoined.current && countAsViewer) {
          const url = `${API_BASE_URL}/api/streams/${streamId}/leave`;
        );

        // Usar fetch com keepalive (executa mesmo após página fechar)
        fetch(`${url}?viewerId=${viewerId}&countAsViewer=${countAsViewer}`, {
          method: "POST",
          keepalive: true,
          headers: {
            "Content-Type": "application/json",
          },
        }).catch((error) => {
          logger.error("[useViewerJoinLeave] Leave request failed", error);
        });
      }
    };

    window.addEventListener("beforeunload", handleBeforeUnload);

    // Leave quando desmontar o componente (navegação SPA)
    return () => {
      window.removeEventListener("beforeunload", handleBeforeUnload);

      // Não resetar hasJoined aqui para evitar recontagem no refresh
      // O leave será chamado pelo beacon ou SSE disconnect
    };
  }, [streamId, viewerId, countAsViewer, onJoinSuccess]);
}
