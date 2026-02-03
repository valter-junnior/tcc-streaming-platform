import { useEffect, useRef, useState } from "react";
import Hls from "hls.js";
import { Play, AlertCircle } from "lucide-react";

interface VideoPlayerProps {
  hlsUrl: string;
  autoPlay?: boolean;
  onReady?: () => void;
}

export function VideoPlayer({
  hlsUrl,
  autoPlay = true,
  onReady,
}: VideoPlayerProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const hlsRef = useRef<Hls | null>(null);
  const retryTimeoutRef = useRef<number | null>(null);
  const retryCountRef = useRef(0);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [needsInteraction, setNeedsInteraction] = useState(false);
  const [currentQuality, setCurrentQuality] = useState<string>("auto");
  const [isReady, setIsReady] = useState(false);

  const MAX_RETRIES = 10;
  const RETRY_DELAY = 5000; // 5 seconds

  const loadHlsStream = () => {
    const video = videoRef.current;
    if (!video) return;

    setError(null);

    // Check HLS support
    if (Hls.isSupported()) {
      // Destroy previous instance if exists
      if (hlsRef.current) {
        hlsRef.current.destroy();
      }

      const hls = new Hls({
        enableWorker: true,
        lowLatencyMode: true,
        backBufferLength: 90,
        manifestLoadingTimeOut: 10000,
        manifestLoadingMaxRetry: 0,
        manifestLoadingRetryDelay: 0,
        levelLoadingTimeOut: 10000,
        levelLoadingMaxRetry: 0,
        fragLoadingTimeOut: 20000,
        fragLoadingMaxRetry: 0,
      });

      hlsRef.current = hls;

      hls.loadSource(hlsUrl);
      hls.attachMedia(video);

      hls.on(Hls.Events.MANIFEST_PARSED, () => {
        console.log("HLS manifest loaded successfully");
        setIsReady(true);
        setIsLoading(false);
        retryCountRef.current = 0;
        onReady?.();

        if (autoPlay) {
          video.play().catch((err) => {
            console.error("Autoplay failed:", err);
            setNeedsInteraction(true);
            setIsLoading(false);
          });
        }
      });

      hls.on(Hls.Events.ERROR, (_event, data) => {
        console.log("HLS error:", data);

        if (data.fatal) {
          switch (data.type) {
            case Hls.ErrorTypes.NETWORK_ERROR:
              if (data.response?.code === 404) {
                // HLS files not ready yet - retry with delay
                if (retryCountRef.current < MAX_RETRIES) {
                  retryCountRef.current++;
                  console.log(
                    `HLS 404 - Retry ${retryCountRef.current}/${MAX_RETRIES} in ${RETRY_DELAY}ms`,
                  );

                  retryTimeoutRef.current = setTimeout(() => {
                    loadHlsStream();
                  }, RETRY_DELAY);
                } else {
                  setError(
                    "Stream não disponível. A transmissão pode não ter iniciado ainda.",
                  );
                  setIsLoading(false);
                }
              } else {
                setError("Erro de rede. Tentando reconectar...");
                setTimeout(() => hls.startLoad(), 1000);
              }
              break;
            case Hls.ErrorTypes.MEDIA_ERROR:
              setError("Erro de mídia. Tentando recuperar...");
              hls.recoverMediaError();
              break;
            default:
              setError("Erro ao carregar stream");
              setIsLoading(false);
              break;
          }
        }
      });

      hls.on(Hls.Events.LEVEL_SWITCHED, (_event, data) => {
        const level = hls.levels[data.level];
        if (level) {
          setCurrentQuality(`${level.height}p`);
        }
      });
    } else if (video.canPlayType("application/vnd.apple.mpegurl")) {
      // Native HLS support (Safari)
      video.src = hlsUrl;

      video.addEventListener("loadedmetadata", () => {
        setIsLoading(false);
        retryCountRef.current = 0;
        if (autoPlay) {
          video.play().catch((err) => {
            console.error("Autoplay failed:", err);
            setNeedsInteraction(true);
          });
        }
      });

      video.addEventListener("error", () => {
        if (retryCountRef.current < MAX_RETRIES) {
          retryCountRef.current++;
          console.log(
            `Native HLS error - Retry ${retryCountRef.current}/${MAX_RETRIES} in ${RETRY_DELAY}ms`,
          );

          retryTimeoutRef.current = setTimeout(() => {
            loadHlsStream();
          }, RETRY_DELAY);
        } else {
          setError("Erro ao carregar stream");
          setIsLoading(false);
        }
      });
    } else {
      setError("Navegador não suporta HLS");
      setIsLoading(false);
    }
  };

  useEffect(() => {
    retryCountRef.current = 0;
    setIsReady(false);

    loadHlsStream();

    return () => {
      if (retryTimeoutRef.current) {
        clearTimeout(retryTimeoutRef.current);
      }
      if (hlsRef.current) {
        hlsRef.current.destroy();
        hlsRef.current = null;
      }
    };
  }, [hlsUrl, autoPlay]);

  const handlePlayClick = () => {
    const video = videoRef.current;
    if (video) {
      video
        .play()
        .then(() => {
          setNeedsInteraction(false);
        })
        .catch((err) => {
          console.error("Failed to play:", err);
          setError("Não foi possível reproduzir o vídeo");
        });
    }
  };

  const handleRetry = () => {
    retryCountRef.current = 0;
    setError(null);
    setIsReady(false);
    loadHlsStream();
  };

  return (
    <div
      className="relative w-full bg-black rounded-lg overflow-hidden"
      style={{ aspectRatio: "16/9" }}
    >
      <video
        ref={videoRef}
        controls
        className={`w-full h-full ${!isReady ? "hidden" : ""}`}
        playsInline
      />

      {/* Loading Overlay - removed, handled by WatchPage */}

      {/* Needs Interaction Overlay */}
      {needsInteraction && !isLoading && !error && (
        <div
          className="absolute inset-0 flex items-center justify-center bg-black/90 cursor-pointer group"
          onClick={handlePlayClick}
        >
          <div className="text-center p-8">
            <div className="w-20 h-20 bg-purple-600 group-hover:bg-purple-700 rounded-full flex items-center justify-center mx-auto mb-4 transition-all transform group-hover:scale-110">
              <Play className="w-10 h-10 text-white ml-1" />
            </div>
            <p className="text-white text-lg font-semibold mb-2">
              Clique para Reproduzir
            </p>
            <p className="text-slate-400 text-sm">
              Seu navegador requer interação para iniciar o vídeo
            </p>
          </div>
        </div>
      )}

      {/* Error Overlay */}
      {error && !isLoading && (
        <div className="absolute inset-0 flex items-center justify-center bg-black/80">
          <div className="text-center p-4">
            <AlertCircle className="w-12 h-12 text-red-500 mx-auto mb-2" />
            <p className="text-white mb-2">{error}</p>
            <button
              onClick={handleRetry}
              className="px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-lg transition-colors inline-flex items-center gap-2"
            >
              <Play className="w-4 h-4" />
              Tentar Novamente
            </button>
          </div>
        </div>
      )}

      {/* Quality Badge */}
      {currentQuality !== "auto" && !isLoading && !error && (
        <div className="absolute top-4 right-4 px-3 py-1 bg-black/70 backdrop-blur rounded text-white text-sm font-semibold">
          {currentQuality}
        </div>
      )}
    </div>
  );
}
