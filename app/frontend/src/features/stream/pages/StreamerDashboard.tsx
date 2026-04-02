import { useEffect, useState, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  Copy,
  Eye,
  Video,
  Clock,
  AlertCircle,
  CheckCircle,
  Loader2,
  Share2,
  XCircle,
  Play,
} from "lucide-react";
import { apiService } from "../../../app/services/apiService";
import { sseService } from "../../../app/services/sseService";
import { RTMP_URL, APP_URL } from "../../../app/config/env";
import { routes } from "../../../app/routes";
import { useUserId } from "../../../shared/hooks/useUserId";
import { useViewerJoinLeave } from "../../../app/hooks/useViewerJoinLeave";
import { logger } from "../../../shared/lib/logger";
import { getErrorMessage } from "../../../shared/utils/errorHandler";
import { StreamingTime } from "../components/StreamingTime";
import type { Stream, StreamStatus } from "../../../app/types/stream";
import { logger } from "../../../shared/lib/logger";

export function StreamerDashboard() {
  const { streamId } = useParams<{ streamId: string }>();
  const navigate = useNavigate();
  const userId = useUserId();

  const [stream, setStream] = useState<Stream | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isRestarting, setIsRestarting] = useState(false);
  const [copiedField, setCopiedField] = useState<string | null>(null);
  const unsubscribeRef = useRef<(() => void) | null>(null);
  const hasConnectedRef = useRef(false);

  // Streamer não deve contar nos viewers (countAsViewer=false)
  useViewerJoinLeave(streamId, userId, false, (response) => {
    // Atualizar viewers mesmo que streamer não conte
    logger.info("[StreamerDashboard] Received join response", response);
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
      hasConnectedRef.current = false; // ✅ Resetar para permitir reconexão no remount
      if (unsubscribeRef.current) {
        unsubscribeRef.current();
        unsubscribeRef.current = null;
      }
    };
  }, [streamId]);

  const loadStream = async () => {
    try {
      setIsLoading(true);
      const data = await apiService.getStream(streamId!);
      setStream(data);
    } catch (err: unknown) {
      logger.error("Error loading stream", err);
      setError(getErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  };

  const connectSse = () => {
    logger.info("[StreamerDashboard] Connecting SSE", { streamId });

    // Usar um viewerId fixo para o streamer (baseado no streamId)
    const streamerViewerId = `streamer-${streamId}`;

    // Passar countAsViewer=false para não contar o streamer como viewer
    unsubscribeRef.current = sseService.subscribe(
      streamId!,
      streamerViewerId,
      (statusMessage) => {
        logger.info(
          "[StreamerDashboard] ✅ SSE Callback: Received status update",
          statusMessage,
        );

        if (statusMessage.type === "STREAM_STARTED") {
          logger.info(
            "[StreamerDashboard] 🎥 Stream started! Updating status to LIVE",
          );
          setStream((prev) =>
            prev
              ? {
                  ...prev,
                  status: "LIVE" as StreamStatus,
                  startedAt: new Date().toISOString(),
                }
              : null,
          );
        } else if (statusMessage.type === "STREAM_ENDED") {
          logger.info(
            "[StreamerDashboard] ⏹️  Stream ended! Updating status to ENDED",
          );
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
          "[StreamerDashboard] ✅ SSE Callback: Received viewers update",
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
      false, // countAsViewer = false para não contar streamer como viewer
    );
  };

  const handleCopy = (text: string, field: string) => {
    navigator.clipboard.writeText(text);
    setCopiedField(field);
    setTimeout(() => setCopiedField(null), 2000);
  };

  const handleRestartStream = async () => {
    if (
      !confirm(
        "Deseja reiniciar esta transmissão? As métricas de viewers serão resetadas.",
      )
    ) {
      return;
    }

    setIsRestarting(true);
    try {
      const data = await apiService.restartStream(streamId!);
      setStream(data);
    } catch (err: any) {
      logger.error("Error restarting stream", err);
      alert("Erro ao reiniciar transmissão");
    } finally {
      setIsRestarting(false);
    }
  };

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
          <h2 className="text-2xl font-bold text-white mb-2">Erro</h2>
          <p className="text-slate-400 mb-6">
            {error || "Stream não encontrada"}
          </p>
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

  const rtmpUrl = RTMP_URL;
  const streamKey = stream.streamKey;
  const watchUrl = `${APP_URL}${routes.watch(stream.id)}`;

  const getStatusColor = (status: StreamStatus) => {
    switch (status) {
      case "WAITING":
        return "text-yellow-500";
      case "LIVE":
        return "text-green-500";
      case "ENDED":
        return "text-red-500";
      default:
        return "text-slate-500";
    }
  };

  const getStatusIcon = (status: StreamStatus) => {
    switch (status) {
      case "WAITING":
        return <Clock className="w-5 h-5" />;
      case "LIVE":
        return <Video className="w-5 h-5" />;
      case "ENDED":
        return <XCircle className="w-5 h-5" />;
      default:
        return null;
    }
  };

  const getStatusText = (status: StreamStatus) => {
    switch (status) {
      case "WAITING":
        return "Aguardando Conexão";
      case "LIVE":
        return "Ao Vivo";
      case "ENDED":
        return "Encerrada";
      default:
        return status;
    }
  };

  return (
    <div className="min-h-screen bg-slate-900">
      <div className="container mx-auto px-4 py-8 max-w-4xl">
        {/* Header */}
        <div className="mb-8">
          <button
            onClick={() => navigate(routes.home())}
            className="text-purple-400 hover:text-purple-300 mb-4 inline-flex items-center gap-2"
          >
            ← Voltar ao Início
          </button>
          <h1 className="text-3xl font-bold text-white mb-2">{stream.title}</h1>
          {stream.description && (
            <p className="text-slate-400">{stream.description}</p>
          )}
        </div>

        {/* Status Badge */}
        <div
          className={`inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-slate-800 border border-slate-700 mb-6 ${getStatusColor(stream.status)}`}
        >
          {getStatusIcon(stream.status)}
          <span className="font-semibold">{getStatusText(stream.status)}</span>
          {(stream.status === "LIVE" || stream.status === "ENDED") && (
            <span className="ml-2">
              <StreamingTime
                startedAt={stream.startedAt}
                endedAt={stream.endedAt}
                status={stream.status}
                size="small"
                showIcon={false}
              />
            </span>
          )}
        </div>

        {/* Stats */}
        <div className="grid grid-cols-3 gap-4 mb-8">
          <div className="bg-slate-800 rounded-lg p-6 border border-slate-700">
            <div className="flex items-center gap-3 mb-2">
              <Eye className="w-5 h-5 text-purple-400" />
              <span className="text-slate-400 text-sm">
                Espectadores Atuais
              </span>
            </div>
            <p className="text-3xl font-bold text-white">
              {stream.currentViewers}
            </p>
          </div>
          <div className="bg-slate-800 rounded-lg p-6 border border-slate-700">
            <div className="flex items-center gap-3 mb-2">
              <Eye className="w-5 h-5 text-purple-400" />
              <span className="text-slate-400 text-sm">
                Pico de Espectadores
              </span>
            </div>
            <p className="text-3xl font-bold text-white">
              {stream.viewersPeak}
            </p>
          </div>
          <div className="bg-slate-800 rounded-lg p-6 border border-slate-700">
            <div className="flex items-center gap-3 mb-2">
              <Clock className="w-5 h-5 text-purple-400" />
              <span className="text-slate-400 text-sm">
                {stream.status === "LIVE"
                  ? "Tempo de Transmissão"
                  : stream.status === "ENDED"
                    ? "Duração Total"
                    : "Tempo de Transmissão"}
              </span>
            </div>
            <div className="text-3xl font-bold text-white">
              {stream.status === "LIVE" || stream.status === "ENDED" ? (
                <StreamingTime
                  startedAt={stream.startedAt}
                  endedAt={stream.endedAt}
                  status={stream.status}
                  size="large"
                  showIcon={false}
                />
              ) : (
                "--:--:--"
              )}
            </div>
          </div>
        </div>

        {/* Instructions for OBS */}
        {stream.status !== "ENDED" && (
          <div className="bg-slate-800 rounded-lg p-6 border border-slate-700 mb-6">
            <h2 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
              <Video className="w-6 h-6 text-purple-400" />
              Configuração do OBS Studio
            </h2>

            <div className="space-y-4">
              {/* RTMP URL */}
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  URL do Servidor
                </label>
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={rtmpUrl}
                    readOnly
                    className="flex-1 px-4 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white font-mono text-sm"
                  />
                  <button
                    onClick={() => handleCopy(rtmpUrl, "rtmp")}
                    className="px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-lg transition-colors flex items-center gap-2"
                  >
                    {copiedField === "rtmp" ? (
                      <CheckCircle className="w-4 h-4" />
                    ) : (
                      <Copy className="w-4 h-4" />
                    )}
                  </button>
                </div>
              </div>

              {/* Stream Key */}
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  Chave de Transmissão
                </label>
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={streamKey}
                    readOnly
                    className="flex-1 px-4 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white font-mono text-sm"
                  />
                  <button
                    onClick={() => handleCopy(streamKey, "key")}
                    className="px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-lg transition-colors flex items-center gap-2"
                  >
                    {copiedField === "key" ? (
                      <CheckCircle className="w-4 h-4" />
                    ) : (
                      <Copy className="w-4 h-4" />
                    )}
                  </button>
                </div>
              </div>

              {/* Watch URL */}
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  Link para Compartilhar
                </label>
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={watchUrl}
                    readOnly
                    className="flex-1 px-4 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white font-mono text-sm"
                  />
                  <button className="px-4 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white flex items-center justify-center">
                    <a
                      href={watchUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      <Eye className="w-4 h-4 text-purple-400" />
                    </a>
                  </button>
                  <button
                    onClick={() => handleCopy(watchUrl, "watch")}
                    className="px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-lg transition-colors flex items-center gap-2"
                  >
                    {copiedField === "watch" ? (
                      <CheckCircle className="w-4 h-4" />
                    ) : (
                      <Share2 className="w-4 h-4" />
                    )}
                  </button>
                </div>
              </div>
            </div>

            {/* OBS Instructions */}
            <div className="mt-6 bg-slate-700/50 rounded-lg p-4">
              <h3 className="font-semibold text-white mb-2">
                Como configurar:
              </h3>
              <ol className="text-sm text-slate-300 space-y-1 list-decimal list-inside">
                <li>Abra o OBS Studio</li>
                <li>Vá em Configurações → Transmissão</li>
                <li>Cole a URL do Servidor e a Chave de Transmissão</li>
                <li>Clique em "Iniciar Transmissão"</li>
              </ol>
            </div>
          </div>
        )}

        {/* End Stream Button */}
        {/*stream.status !== "ENDED" && (
          <button
            onClick={handleEndStream}
            disabled={isEnding}
            className="w-full px-6 py-3 bg-red-600 hover:bg-red-700 text-white rounded-lg transition-colors disabled:opacity-50 flex items-center justify-center gap-2 font-semibold"
          >
            {isEnding ? (
              <>
                <Loader2 className="w-5 h-5 animate-spin" />
                Encerrando...
              </>
            ) : (
              <>
                <XCircle className="w-5 h-5" />
                Encerrar Transmissão
              </>
            )}
          </button>
        ) */}

        {/* Stream Ended Message */}
        {stream.status === "ENDED" && (
          <div className="bg-slate-800 rounded-lg p-8 border border-slate-700 text-center">
            <XCircle className="w-16 h-16 text-red-500 mx-auto mb-4" />
            <h3 className="text-2xl font-bold text-white mb-2">
              Transmissão Encerrada
            </h3>
            <p className="text-slate-400 mb-6">
              Esta transmissão foi finalizada. Você pode reiniciá-la usando a
              mesma stream key ou criar uma nova.
            </p>
            <div className="flex gap-4 justify-center">
              <button
                onClick={handleRestartStream}
                disabled={isRestarting}
                className="px-6 py-2 bg-green-600 hover:bg-green-700 text-white rounded-lg transition-colors disabled:opacity-50 flex items-center gap-2"
              >
                {isRestarting ? (
                  <>
                    <Loader2 className="w-5 h-5 animate-spin" />
                    Reiniciando...
                  </>
                ) : (
                  <>
                    <Play className="w-5 h-5" />
                    Reiniciar Stream
                  </>
                )}
              </button>
              <button
                onClick={() => navigate(routes.home())}
                className="px-6 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-lg transition-colors"
              >
                Criar Nova Stream
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
