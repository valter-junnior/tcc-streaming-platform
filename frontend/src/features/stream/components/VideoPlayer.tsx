import { useEffect, useRef, useState } from "react";
import Hls from "hls.js";
import { Play, AlertCircle, Loader2 } from "lucide-react";

interface VideoPlayerProps {
  hlsUrl: string;
  autoPlay?: boolean;
}

export function VideoPlayer({ hlsUrl, autoPlay = true }: VideoPlayerProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const hlsRef = useRef<Hls | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentQuality, setCurrentQuality] = useState<string>("auto");

  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    setIsLoading(true);
    setError(null);

    // Check HLS support
    if (Hls.isSupported()) {
      const hls = new Hls({
        enableWorker: true,
        lowLatencyMode: true,
        backBufferLength: 90,
      });

      hlsRef.current = hls;

      hls.loadSource(hlsUrl);
      hls.attachMedia(video);

      hls.on(Hls.Events.MANIFEST_PARSED, () => {
        console.log("HLS manifest loaded");
        setIsLoading(false);

        if (autoPlay) {
          video.play().catch((err) => {
            console.error("Autoplay failed:", err);
            setError("Clique em play para iniciar");
            setIsLoading(false);
          });
        }
      });

      hls.on(Hls.Events.ERROR, (_event, data) => {
        console.error("HLS error:", data);

        if (data.fatal) {
          switch (data.type) {
            case Hls.ErrorTypes.NETWORK_ERROR:
              setError("Erro de rede. Tentando reconectar...");
              setTimeout(() => hls.startLoad(), 1000);
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
        if (autoPlay) {
          video.play().catch((err) => {
            console.error("Autoplay failed:", err);
            setError("Clique em play para iniciar");
          });
        }
      });

      video.addEventListener("error", () => {
        setError("Erro ao carregar stream");
        setIsLoading(false);
      });
    } else {
      setError("Navegador não suporta HLS");
      setIsLoading(false);
    }

    return () => {
      if (hlsRef.current) {
        hlsRef.current.destroy();
        hlsRef.current = null;
      }
    };
  }, [hlsUrl, autoPlay]);

  return (
    <div
      className="relative w-full bg-black rounded-lg overflow-hidden"
      style={{ aspectRatio: "16/9" }}
    >
      <video ref={videoRef} controls className="w-full h-full" playsInline />

      {/* Loading Overlay */}
      {isLoading && (
        <div className="absolute inset-0 flex items-center justify-center bg-black/80">
          <div className="text-center">
            <Loader2 className="w-12 h-12 text-purple-500 animate-spin mx-auto mb-2" />
            <p className="text-white">Carregando stream...</p>
          </div>
        </div>
      )}

      {/* Error Overlay */}
      {error && !isLoading && (
        <div className="absolute inset-0 flex items-center justify-center bg-black/80">
          <div className="text-center p-4">
            <AlertCircle className="w-12 h-12 text-red-500 mx-auto mb-2" />
            <p className="text-white mb-2">{error}</p>
            {error.includes("Clique") && (
              <button
                onClick={() => videoRef.current?.play()}
                className="px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-lg transition-colors inline-flex items-center gap-2"
              >
                <Play className="w-4 h-4" />
                Reproduzir
              </button>
            )}
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
