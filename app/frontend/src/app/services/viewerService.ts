import { apiService } from "./apiService";
import { logger } from "../../shared/lib/logger";

export interface ViewerResponse {
  currentViewers: number;
  viewersPeak: number;
  status: string;
}

/**
 * Service para gerenciar join/leave de viewers nas streams
 * Usa REST API (axios) ao invés de SSE para manipular dados
 */
class ViewerService {
  /**
   * Entra em uma stream (incrementa viewers)
   * Chamado quando o usuário acessa a página
   */
  async joinStream(
    streamId: string,
    viewerId: string,
    countAsViewer: boolean = true,
  ): Promise<ViewerResponse> {
    try {
      logger.info("[Viewer] Joining stream", {
        streamId,
        viewerId,
        countAsViewer,
      });

      const response = await apiService
        .getAxiosInstance()
        .post<ViewerResponse>(`/streams/${streamId}/join`, null, {
          params: {
            viewerId,
            countAsViewer,
          },
        });

      logger.info("[Viewer] Joined stream successfully", response.data);
      return response.data;
    } catch (error) {
      logger.error("[Viewer] Error joining stream", error);
      throw error;
    }
  }

  /**
   * Sai de uma stream (decrementa viewers)
   * Chamado quando o usuário sai da página
   */
  async leaveStream(
    streamId: string,
    viewerId: string,
    countAsViewer: boolean = true,
  ): Promise<ViewerResponse> {
    try {
      logger.info("[Viewer] Leaving stream", {
        streamId,
        viewerId,
        countAsViewer,
      });

      const response = await apiService
        .getAxiosInstance()
        .post<ViewerResponse>(`/streams/${streamId}/leave`, null, {
          params: {
            viewerId,
            countAsViewer,
          },
        });

      logger.info("[Viewer] Left stream successfully", response.data);
      return response.data;
    } catch (error) {
      logger.error("[Viewer] Error leaving stream", error);
      throw error;
    }
  }
}

export const viewerService = new ViewerService();
