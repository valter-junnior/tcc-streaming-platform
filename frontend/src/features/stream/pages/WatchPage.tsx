import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  Eye,
  AlertCircle,
  Loader2,
  Clock,
  Video as VideoIcon,
} from "lucide-react";
import { apiService } from "../../../app/services/apiService";
import { websocketService } from "../../../app/services/websocketService";
import { HLS_URL } from "../../../app/config/env";
import { routes } from "../../../app/routes";
import { VideoPlayer } from "../components/VideoPlayer";
import type { Stream, StreamStatus } from "../../../app/types/stream";

export function WatchPage() {
  const { streamId } = useParams<{ streamId: string }>();
  const navigate = useNavigate();

  const [stream, setStream] = useState<Stream | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [viewerId] = useState(
    () => `viewer_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
  );

  useEffect(() => {
    if (!streamId) {
      navigate(routes.home);
      return;
    }

    loadStream();
    connectWebSocket();

    return () => {
      if (stream && websocketService.isConnected()) {
        websocketService.sendViewerLeft(streamId, viewerId);
      }
      websocketService.disconnect();
    };
  }, [streamId]);

  useEffect(() => {
    if (stream && stream.status === "LIVE" && websocketService.isConnected()) {
      websocketService.sendViewerJoined(streamId!, viewerId);
    }
  }, [stream?.status]);

  const loadStream = async () => {
    try {
      setIsLoading(true);
      const data = await apiService.getStream(streamId!);
      setStream(data);
    } catch (err: any) {
      console.error("Error loading stream:", err);
      setError("Stream não encontrada");
    } finally {
      setIsLoading(false);
    }
  };

  const connectWebSocket = async () => {
    try {
      await websocketService.connect();

      websocketService.subscribeToStreamStatus(streamId!, (message) => {
        if (
          message.type === "STATUS_UPDATE" ||
          message.type === "STREAM_STARTED"
        ) {
          setStream((prev) =>
            prev ? { ...prev, status: message.data.status } : null,
          );
        } else if (message.type === "STREAM_ENDED") {
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
      });

      websocketService.subscribeToStreamViewers(streamId!, (message) => {
        if (message.type === "VIEWERS_UPDATE") {
          setStream((prev) =>
            prev
              ? {
                  ...prev,
                  currentViewers: message.data.currentViewers,
                  viewersPeak: message.data.viewersPeak,
                }
              : null,
          );
        }
      });
    } catch (err) {
      console.error("WebSocket connection failed:", err);
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
          <h2 className="text-2xl font-bold text-white mb-2">
            Stream não encontrada
          </h2>
          <p className="text-slate-400 mb-6">{error}</p>
          <button
            onClick={() => navigate(routes.home)}
            className="px-6 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-lg transition-colors"
          >
            Voltar ao Início
          </button>
        </div>
      </div>
    );
  }

  const hlsUrl = `${HLS_URL}/${stream.streamKey}/index.m3u8`;

  return (
    <div className="min-h-screen bg-slate-900">
      <div className="container mx-auto px-4 py-8 max-w-6xl">
        {/* Back Button */}
        <button
          onClick={() => navigate(routes.home)}
          className="text-purple-400 hover:text-purple-300 mb-6 inline-flex items-center gap-2"
        >
          ← Voltar ao Início
        </button>

        <div className="grid lg:grid-cols-3 gap-6">
          {/* Video Player */}
          <div className="lg:col-span-2">
            {stream.status === "LIVE" ? (
              <VideoPlayer hlsUrl={hlsUrl} />
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
                onClick={() => navigate(routes.home)}
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
