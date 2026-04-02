import { useEffect, useState, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  Eye,
  AlertCircle,
  Loader2,
  Clock,
  Video as VideoIcon,
} from "lucide-react";
import { apiService } from "../../../app/services/apiService";
import { sseService } from "../../../app/services/sseService";
import { useViewerId } from "../../../app/hooks/useViewerId";
import { useViewerJoinLeave } from "../../../app/hooks/useViewerJoinLeave";
import { HLS_URL } from "../../../app/config/env";
import { routes } from "../../../app/routes";
import { VideoPlayerPlyr as VideoPlayer } from "../components/VideoPlayerPlyr";
import { StreamingTime } from "../components/StreamingTime";
import type { Stream, StreamStatus } from "../../../app/types/stream";
import { logger } from "../../../shared/lib/logger";
import { getErrorMessage } from "../../../shared/utils/errorHandler";
import axios from "axios";

export function WatchPage() {
  const { streamId } = useParams<{ streamId: string }>();
  const navigate = useNavigate();

  const [stream, setStream] = useState<Stream | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isRetrying, setIsRetrying] = useState(false);
  const [hlsAvailable, setHlsAvailable] = useState(false);
  const viewerId = useViewerId(); // Persistente no localStorage
  const hlsCheckIntervalRef = useRef<number | null>(null);
  const unsubscribeRef = useRef<(() => void) | null>(null);
  const hasConnectedRef = useRef(false);

  useViewerJoinLeave(streamId, viewerId, true, (response) => {
    logger.info("[WatchPage] Updating viewers from join response", response);
    setStream((prev) =>
      prev
        ? {
            ...prev,
            currentViewers: response.currentViewers,
            viewersPeak: response.viewersPeak,
          }
        : null,
    );
  });

  useEffect(() => {
    if (!streamId) {
      navigate(routes.home());
      return;
    }

    // Proteção contra double-call do React Strict Mode
    if (hasConnectedRef.current) {
      return;
    }

    hasConnectedRef.current = true;
    loadStream();
    connectSse();

    return () => {
      logger.info("[WatchPage] Component unmount - Unsubscribing from SSE");
      hasConnectedRef.current = false; // ✅ Resetar para permitir reconexão no remount
      if (unsubscribeRef.current) {
        unsubscribeRef.current();
        unsubscribeRef.current = null;
      }
    };
  }, [streamId, viewerId]);

  const loadStream = async () => {
    try {
      setIsLoading(true);
      const data = await apiService.getStream(streamId!);
      setStream(data);
    } catch (error) {
      logger.error("Error loading stream", error);
      if (axios.isAxiosError(error) && error.response?.status === 400) {
        setError("ID de stream inválido.");
      } else {
        setError(getErrorMessage(error));
      }
    } finally {
      setIsLoading(false);
    }
  };

  const checkHlsAvailability = async (hlsUrl: string): Promise<boolean> => {
    try {
      const response = await fetch(hlsUrl, { method: "HEAD" });
      return response.ok;
    } catch (error) {
      logger.debug("HLS not available yet", error);
      return false;
    }
  };

  const startHlsPolling = (hlsUrl: string) => {
    setHlsAvailable(false);
    setIsRetrying(true);

    const poll = async () => {
      logger.debug("Checking HLS availability...");
      const isAvailable = await checkHlsAvailability(hlsUrl);

      if (isAvailable) {
        logger.info("HLS is available!");
        setHlsAvailable(true);
        setIsRetrying(false);
        if (hlsCheckIntervalRef.current) {
          clearInterval(hlsCheckIntervalRef.current);
          hlsCheckIntervalRef.current = null;
        }
      } else {
        logger.debug("HLS not available, will retry in 5s...");
      }
    };

    poll();

    hlsCheckIntervalRef.current = window.setInterval(poll, 5000);
  };

  const stopHlsPolling = () => {
    if (hlsCheckIntervalRef.current) {
      clearInterval(hlsCheckIntervalRef.current);
      hlsCheckIntervalRef.current = null;
    }
    setIsRetrying(false);
  };

  const connectSse = () => {
    logger.info("[WatchPage] Connecting SSE", { streamId, viewerId });

    unsubscribeRef.current = sseService.subscribe(
      streamId!,
      viewerId,
      (statusMessage) => {
        logger.info(
          "[WatchPage] ✅ SSE Callback: Received status update",
          statusMessage,
        );

        if (statusMessage.type === "STREAM_STARTED") {
          logger.info("[WatchPage] 🎥 Stream started! Updating status to LIVE");
          setStream((prev) =>
            prev
              ? { ...prev, status: statusMessage.status as StreamStatus }
              : null,
          );
        } else if (statusMessage.type === "STREAM_ENDED") {
          logger.info("[WatchPage] ⏹️  Stream ended! Updating status to ENDED");
          setStream((prev) =>
            prev
              ? {
                  ...prev,
                  status: "ENDED" as StreamStatus,
                  endedAt: new Date().toISOString(),
                }
              : null,
          );
        }
      },
      (viewersMessage) => {
        logger.info(
          "[WatchPage] ✅ SSE Callback: Received viewers update",
          viewersMessage,
        );

        setStream((prev) =>
          prev
            ? {
                ...prev,
                currentViewers: viewersMessage.currentViewers,
                viewersPeak: viewersMessage.viewersPeak,
              }
            : null,
        );
      },
    );
  };

  // Monitor stream status and start HLS polling when LIVE
  useEffect(() => {
    if (!stream) return;

    const hlsUrl = `${HLS_URL}/${stream.streamKey}/master.m3u8`;

    if (stream.status === "LIVE") {
      startHlsPolling(hlsUrl);
    } else {
      stopHlsPolling();
      setHlsAvailable(false);
    }

    return () => {
      stopHlsPolling();
    };
  }, [stream?.status, stream?.streamKey]);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-slate-900 flex items-center justify-center">
        <Loader2 className="w-8 h-8 text-purple-500 animate-spin" />
      </div>
    );
  }

  if (error || !stream) {
    return (
      <div className="min-h-screen bg-slate-900 flex items-center justify-center p-4">
        <div className="bg-slate-800 rounded-lg p-8 max-w-md text-center border border-slate-700">
          <AlertCircle className="w-16 h-16 text-red-500 mx-auto mb-4" />
          <h2 className="text-2xl font-bold text-white mb-2">
            Stream não encontrada
          </h2>
          <p className="text-slate-400 mb-6">{error}</p>
          <button
            onClick={() => navigate(routes.home())}
            className="px-6 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-lg transition-colors"
          >
            Voltar ao Início
          </button>
        </div>
      </div>
    );
  }

  // FFmpeg gera master.m3u8 com múltiplas qualidades (1080p, 720p, 480p, 360p)
  const hlsUrl = `${HLS_URL}/${stream.streamKey}/master.m3u8`;

  return (
    <div className="min-h-screen bg-slate-900">
      <div className="container mx-auto px-4 py-8 max-w-6xl">
        {/* Back Button */}
        <button
          onClick={() => navigate(routes.home())}
          className="text-purple-400 hover:text-purple-300 mb-6 inline-flex items-center gap-2"
        >
          ← Voltar ao Início
        </button>

        <div className="grid lg:grid-cols-3 gap-6">
          {/* Video Player */}
          <div className="lg:col-span-2">
            {stream.status === "LIVE" && hlsAvailable ? (
              <VideoPlayer hlsUrl={hlsUrl} />
            ) : stream.status === "LIVE" && !hlsAvailable ? (
              <div
                className="relative w-full bg-slate-800 rounded-lg flex items-center justify-center border border-slate-700"
                style={{ aspectRatio: "16/9" }}
              >
                <div className="text-center p-8">
                  <Clock className="w-16 h-16 text-yellow-500 mx-auto mb-4 animate-pulse" />
                  <h3 className="text-2xl font-bold text-white mb-2">
                    Aguardando Transmissão
                  </h3>
                  <p className="text-slate-400">
                    {isRetrying
                      ? "Conectando ao stream... tentando novamente."
                      : "Processando stream... aguarde alguns segundos."}
                  </p>
                </div>
              </div>
            ) : stream.status === "WAITING" ? (
              <div
                className="relative w-full bg-slate-800 rounded-lg flex items-center justify-center border border-slate-700"
                style={{ aspectRatio: "16/9" }}
              >
                <div className="text-center p-8">
                  <Clock className="w-16 h-16 text-yellow-500 mx-auto mb-4" />
                  <h3 className="text-2xl font-bold text-white mb-2">
                    Aguardando Transmissão
                  </h3>
                  <p className="text-slate-400">
                    O streamer ainda não iniciou a transmissão. Aguarde...
                  </p>
                </div>
              </div>
            ) : (
              <div
                className="relative w-full bg-slate-800 rounded-lg flex items-center justify-center border border-slate-700"
                style={{ aspectRatio: "16/9" }}
              >
                <div className="text-center p-8">
                  <AlertCircle className="w-16 h-16 text-red-500 mx-auto mb-4" />
                  <h3 className="text-2xl font-bold text-white mb-2">
                    Transmissão Encerrada
                  </h3>
                  <p className="text-slate-400">
                    Esta transmissão foi finalizada.
                  </p>
                </div>
              </div>
            )}

            {/* Stream Info */}
            <div className="mt-4">
              <h1 className="text-2xl font-bold text-white mb-2">
                {stream.title}
              </h1>
              {stream.description && (
                <p className="text-slate-400">{stream.description}</p>
              )}
            </div>
          </div>

          {/* Sidebar */}
          <div className="space-y-4">
            {/* Status Card */}
            <div className="bg-slate-800 rounded-lg p-6 border border-slate-700">
              <div className="flex items-center gap-2 mb-4">
                {stream.status === "LIVE" ? (
                  <>
                    <div className="w-3 h-3 bg-red-500 rounded-full animate-pulse" />
                    <span className="text-white font-semibold">AO VIVO</span>
                    <StreamingTime
                      startedAt={stream.startedAt}
                      endedAt={stream.endedAt}
                      status={stream.status}
                      size="small"
                      showIcon={false}
                    />
                  </>
                ) : stream.status === "WAITING" ? (
                  <>
                    <Clock className="w-5 h-5 text-yellow-500" />
                    <span className="text-yellow-500 font-semibold">
                      AGUARDANDO
                    </span>
                  </>
                ) : (
                  <>
                    <AlertCircle className="w-5 h-5 text-red-500" />
                    <span className="text-red-500 font-semibold">
                      ENCERRADA
                    </span>
                  </>
                )}
              </div>

              <div className="space-y-3">
                <div className="flex items-center gap-3">
                  <Eye className="w-5 h-5 text-purple-400" />
                  <div>
                    <p className="text-sm text-slate-400">Espectadores</p>
                    <p className="text-xl font-bold text-white">
                      {stream.currentViewers}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <Eye className="w-5 h-5 text-purple-400" />
                  <div>
                    <p className="text-sm text-slate-400">Pico de Viewers</p>
                    <p className="text-xl font-bold text-white">
                      {stream.viewersPeak}
                    </p>
                  </div>
                </div>

                {(stream.status === "LIVE" || stream.status === "ENDED") && (
                  <div className="flex items-center gap-3">
                    <Clock className="w-5 h-5 text-purple-400" />
                    <div>
                      <p className="text-sm text-slate-400">
                        {stream.status === "LIVE"
                          ? "Tempo de Transmissão"
                          : "Duração Total"}
                      </p>
                      <div className="text-xl font-bold text-white">
                        <StreamingTime
                          startedAt={stream.startedAt}
                          endedAt={stream.endedAt}
                          status={stream.status}
                          size="medium"
                          showIcon={false}
                        />
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* About Streaming */}
            <div className="bg-slate-800 rounded-lg p-6 border border-slate-700">
              <div className="flex items-center gap-2 mb-3">
                <VideoIcon className="w-5 h-5 text-purple-400" />
                <h3 className="font-semibold text-white">Sobre o Streaming</h3>
              </div>
              <div className="text-sm text-slate-400 space-y-2">
                <p>
                  Esta plataforma utiliza{" "}
                  <strong className="text-white">
                    HLS (HTTP Live Streaming)
                  </strong>{" "}
                  para entregar vídeo adaptativo.
                </p>
                <p>
                  O player seleciona automaticamente a melhor qualidade baseado
                  na sua conexão.
                </p>
              </div>
            </div>

            {/* Create Your Own */}
            <div className="bg-gradient-to-br from-purple-900/50 to-purple-800/50 rounded-lg p-6 border border-purple-700/50">
              <h3 className="font-semibold text-white mb-2">
                Quer transmitir também?
              </h3>
              <p className="text-sm text-slate-300 mb-4">
                Crie sua própria transmissão em segundos!
              </p>
              <button
                onClick={() => navigate(routes.home())}
                className="w-full px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-lg transition-colors font-semibold"
              >
                Iniciar Minha Stream
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
