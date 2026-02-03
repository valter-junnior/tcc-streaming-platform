import { useEffect, useRef } from "react";
// @ts-ignore - Plyr doesn't have proper TypeScript exports
import Plyr from "plyr";
import Hls from "hls.js";
import "plyr/dist/plyr.css";
import { logger } from "../../../shared/lib/logger";

interface VideoPlayerProps {
  hlsUrl: string;
  autoPlay?: boolean;
}

export function VideoPlayerPlyr({
  hlsUrl,
  autoPlay = false,
}: VideoPlayerProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const playerRef = useRef<Plyr | null>(null);
  const hlsRef = useRef<Hls | null>(null);

  // Função para adicionar botão LIVE customizado
  const addLiveButton = (player: Plyr) => {
    const controlsContainer =
      player.elements?.container?.querySelector(".plyr__controls");
    if (controlsContainer) {
      const liveButton = document.createElement("button");
      liveButton.className = "plyr__control plyr__control--live";
      liveButton.innerHTML = "● AO VIVO";
      liveButton.setAttribute("aria-label", "Voltar ao vivo");
      liveButton.onclick = (e) => {
        e.preventDefault();
        e.stopPropagation();

        if (hlsRef.current && player.media) {
          // Método mais sutil - apenas ajusta para a posição mais atual
          const seekableLength = player.media.seekable.length;
          if (seekableLength > 0) {
            // Vai para o final do buffer disponível (posição mais próxima do vivo)
            const livePosition = player.media.seekable.end(seekableLength - 1);
            player.media.currentTime = livePosition;
          } else {
            // Fallback: vai para a duração atual
            if (player.duration && player.duration > 0) {
              player.media.currentTime = player.duration;
            }
          }
        } else if (player.media) {
          // Para Safari nativo - método mais direto
          const seekableLength = player.media.seekable.length;
          if (seekableLength > 0) {
            const livePosition = player.media.seekable.end(seekableLength - 1);
            player.media.currentTime = livePosition;
          } else {
            player.media.currentTime = player.duration || 0;
          }
        }
      };

      // Inserir antes do botão de fullscreen
      const fullscreenBtn = controlsContainer.querySelector(
        '[data-plyr="fullscreen"]',
      );
      if (fullscreenBtn) {
        controlsContainer.insertBefore(liveButton, fullscreenBtn);
      } else {
        controlsContainer.appendChild(liveButton);
      }
    }
  };

  useEffect(() => {
    if (!videoRef.current) return;

    const video = videoRef.current;

    // Then initialize HLS.js BEFORE Plyr
    if (Hls.isSupported()) {
      const hls = new Hls({
        enableWorker: false,
        debug: false,
        lowLatencyMode: false,
        backBufferLength: 90,
        maxBufferLength: 30,
        maxMaxBufferLength: 60,
        xhrSetup: function (xhr: XMLHttpRequest) {
          xhr.withCredentials = false;
        },
      });

      hlsRef.current = hls;

      hls.on(Hls.Events.ERROR, (_event, data) => {
        logger.error("HLS error", data);
        if (data.fatal) {
          switch (data.type) {
            case Hls.ErrorTypes.NETWORK_ERROR:
              logger.warn("Network error, trying to recover...");
              hls.startLoad();
              break;
            case Hls.ErrorTypes.MEDIA_ERROR:
              logger.warn("Media error, trying to recover...");
              hls.recoverMediaError();
              break;
            default:
              logger.error("Cannot recover from error, destroying HLS");
              hls.destroy();
              break;
          }
        }
      });

      hls.loadSource(hlsUrl);
      hls.attachMedia(video);

      hls.on(Hls.Events.MANIFEST_PARSED, () => {
        logger.info("HLS manifest parsed successfully");

        // Initialize Plyr AFTER HLS is ready
        if (!playerRef.current) {
          const player = new Plyr(video, {
            controls: [
              "play-large",
              "play",
              "mute",
              "volume",
              "settings",
              "fullscreen",
            ],
            settings: ["quality"],
            autoplay: false,
            muted: false,
            clickToPlay: true,
            keyboard: { focused: true, global: false },
            tooltips: { controls: true, seek: false },
            seekTime: 0,
            displayDuration: false,
            invertTime: false,
          });

          playerRef.current = player;

          // Adicionar botão LIVE após inicializar o player
          setTimeout(() => addLiveButton(player), 100);
        }
      });
    } else if (video.canPlayType("application/vnd.apple.mpegurl")) {
      // Native HLS support (Safari)
      video.src = hlsUrl;

      // Initialize Plyr for Safari
      const player = new Plyr(video, {
        controls: [
          "play-large",
          "play",
          "mute",
          "volume",
          "settings",
          "fullscreen",
        ],
        settings: ["quality"],
        autoplay: false,
        muted: false,
        clickToPlay: true,
        keyboard: { focused: true, global: false },
        tooltips: { controls: true, seek: false },
        seekTime: 0,
        displayDuration: false,
        invertTime: false,
      });

      playerRef.current = player;

      // Adicionar botão LIVE após inicializar o player
      setTimeout(() => addLiveButton(player), 100);
    } else {
      logger.error("HLS not supported in this browser");
    }

    return () => {
      if (playerRef.current) {
        playerRef.current.destroy();
        playerRef.current = null;
      }
      if (hlsRef.current) {
        hlsRef.current.destroy();
        hlsRef.current = null;
      }
    };
  }, [hlsUrl, autoPlay]);

  return (
    <div
      className="plyr-container rounded-sm overflow-hidden"
      style={{ aspectRatio: "16/9" }}
    >
      <video
        ref={videoRef}
        className="plyr-video"
        playsInline
        controls
        crossOrigin="anonymous"
      />
      <style>{`
        .plyr-container {
          --plyr-color-main: #8B5CF6; /* projeto accent */
          --plyr-video-background: #000;
          --plyr-video-control-color: #ffffff;
          --plyr-video-control-color-hover: #ffffff;
          --plyr-video-control-background-hover: var(--plyr-color-main);

          background: var(--plyr-video-background);
        }

        .plyr-video {
          width: 100%;
          height: 100%;
        }

        /* Big play button */
        .plyr__control--overlaid {
          background: rgba(17, 24, 39, 0.75);
          backdrop-filter: blur(12px) saturate(180%);
          border: 3px solid rgba(255, 255, 255, 0.9);
          transition: all 0.25s ease;
          box-shadow: 0 0 0 0 rgba(139, 92, 246, 0),
                      0 8px 32px rgba(0, 0, 0, 0.6);
        }

        .plyr__control--overlaid:hover {
          background: rgba(139, 92, 246, 0.25);
          border-color: rgba(139, 92, 246, 1);
        }

        /* Controls bar */
        .plyr__controls {
          background: linear-gradient(to top, rgba(0, 0, 0, 0.8), transparent);
        }

        /* Control buttons hover and active colors */
        .plyr__control:hover {
          color: var(--plyr-color-main);
          background: rgba(139, 92, 246, 0.06);
        }

        .plyr__control[aria-pressed="true"] {
          color: var(--plyr-color-main);
        }

        /* Progress bar thumb */
        .plyr__progress input[type="range"]::-webkit-slider-thumb {
          background: var(--plyr-color-main);
          box-shadow: 0 1px 1px rgba(0, 0, 0, 0.15),
                      0 0 0 1px rgba(139, 92, 246, 0.2);
        }

        .plyr__progress input[type="range"]::-moz-range-thumb {
          background: var(--plyr-color-main);
          box-shadow: 0 1px 1px rgba(0, 0, 0, 0.15),
                      0 0 0 1px rgba(139, 92, 246, 0.2);
        }

        /* Volume thumb */
        .plyr__volume input[type="range"]::-webkit-slider-thumb {
          background: var(--plyr-color-main);
        }

        .plyr__volume input[type="range"]::-moz-range-thumb {
          background: var(--plyr-color-main);
        }

        /* Loading spinner */
        .plyr--loading::after {
          border-color: rgba(139, 92, 246, 0.3);
          border-top-color: var(--plyr-color-main);
        }

        /* Menu dropdown */
        .plyr__menu__container {
          background: rgba(17, 24, 39, 0.95);
          backdrop-filter: blur(12px);
          border: 1px solid rgba(139, 92, 246, 0.2);
          border-radius: 0.5rem;
        }

        .plyr__menu__container [role="menuitemradio"][aria-checked="true"]::before {
          background: var(--plyr-color-main);
        }

        /* Botão LIVE customizado */
        .plyr__control--live {
          font-weight: bold;
          color: white !important;
          background-color: rgb(220, 38, 38) !important;
          padding: 0 12px !important;
          border-radius: 4px !important;
          margin-left: 8px !important;
          font-size: 0.8em !important;
          transition: all 0.2s ease !important;
        }

        .plyr__control--live:hover {
          background-color: rgb(185, 28, 28) !important;
          transform: scale(1.05) !important;
        }

        .plyr__control--live::before {
          content: none !important;
        }
      `}</style>
    </div>
  );
}
