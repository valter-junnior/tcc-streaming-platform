import { useEffect, useRef } from "react";
import videojs from "video.js";
import "video.js/dist/video-js.css";
import { logger } from "../../../shared/lib/logger";
import type Player from "video.js/dist/types/player";

interface VideoPlayerProps {
  hlsUrl: string;
  autoPlay?: boolean;
  onReady?: () => void;
  onRetrying?: (retryCount: number, maxRetries: number) => void;
  onError?: (error: any) => void;
}

export function VideoPlayer({
  hlsUrl,
  autoPlay = true,
  onReady,
  onRetrying,
  onError,
}: VideoPlayerProps) {
  const videoRef = useRef<HTMLDivElement>(null);
  const playerRef = useRef<Player | null>(null);
  const retryTimeoutRef = useRef<number | null>(null);
  const retryCountRef = useRef(0);

  const MAX_RETRIES = 10;
  const RETRY_DELAY = 5000; // 5 seconds

  useEffect(() => {
    if (!videoRef.current) return;

    // Cleanup previous player
    if (playerRef.current) {
      playerRef.current.dispose();
      playerRef.current = null;
    }

    retryCountRef.current = 0;

    const initializePlayer = () => {
      if (!videoRef.current) return;

      // Create video element
      const videoElement = document.createElement("video-js");
      videoElement.className = "vjs-big-play-centered";
      videoRef.current.appendChild(videoElement);

      // Initialize video.js player
      const player = videojs(videoElement, {
        controls: true,
        autoplay: autoPlay,
        preload: "auto",
        fluid: true,
        liveui: true, // Enable live UI
        controlBar: {
          progressControl: false, // Remove seekbar for live
          remainingTimeDisplay: false,
          currentTimeDisplay: false,
          timeDivider: false,
          durationDisplay: false,
          seekToLive: false,
          playToggle: true,
          volumePanel: {
            inline: false,
          },
          pictureInPictureToggle: false,
          fullscreenToggle: true,
        },
        html5: {
          vhs: {
            overrideNative: true,
            enableLowInitialPlaylist: true,
            smoothQualityChange: true,
            useBandwidthFromLocalStorage: true,
          },
          nativeAudioTracks: false,
          nativeVideoTracks: false,
        },
      });

      playerRef.current = player;

      // Set source
      player.src({
        src: hlsUrl,
        type: "application/x-mpegURL",
      });

      // Event handlers
      player.on("loadedmetadata", () => {
        logger.debug("Video.js: Manifest loaded successfully");
        onReady?.();
      });

      player.on("error", () => {
        const error = player.error();
        logger.error("Video.js error", error);

        if (error && (error.code === 2 || error.code === 4)) {
          // MEDIA_ERR_NETWORK (2) or MEDIA_ERR_SRC_NOT_SUPPORTED (4)
          if (retryCountRef.current < MAX_RETRIES) {
            retryCountRef.current++;
            logger.warn(
              `Video.js: Retry ${retryCountRef.current}/${MAX_RETRIES} in ${RETRY_DELAY}ms`,
            );

            onRetrying?.(retryCountRef.current, MAX_RETRIES);

            retryTimeoutRef.current = window.setTimeout(() => {
              if (playerRef.current) {
                playerRef.current.src({
                  src: hlsUrl,
                  type: "application/x-mpegURL",
                });
              }
            }, RETRY_DELAY);
          } else {
            onError?.(error);
          }
        } else {
          onError?.(error);
        }
      });

      player.on("playing", () => {
        retryCountRef.current = 0;
      });
    };

    initializePlayer();

    return () => {
      if (retryTimeoutRef.current) {
        clearTimeout(retryTimeoutRef.current);
      }
      if (playerRef.current) {
        playerRef.current.dispose();
        playerRef.current = null;
      }
    };
  }, [hlsUrl, autoPlay, onReady]);

  return (
    <div
      className="relative w-full rounded-lg overflow-hidden"
      style={{ aspectRatio: "16/9" }}
    >
      <div ref={videoRef} className="video-player-wrapper" />
      <style>{`
        .video-player-wrapper {
          width: 100%;
          height: 100%;
        }
        
        .video-js {
          width: 100%;
          height: 100%;
          font-family: inherit;
          background-color: #000;
        }
        
        /* Custom big play button */
        .vjs-big-play-button {
          position: absolute;
          top: 50%;
          left: 50%;
          transform: translate(-50%, -50%);
          width: 96px;
          height: 96px;
          border-radius: 50%;
          border: 3px solid rgba(255, 255, 255, 0.9);
          background: rgba(17, 24, 39, 0.75);
          backdrop-filter: blur(12px) saturate(180%);
          transition: all 0.25s ease;
          box-shadow: 0 0 0 0 rgba(139, 92, 246, 0.6),
                      0 8px 32px rgba(0, 0, 0, 0.6);
        }
        
        .vjs-big-play-button:hover {
          border-color: rgba(139, 92, 246, 1);
          background: rgba(139, 92, 246, 0.25);
          transform: translate(-50%, -50%) scale(1.08);
          box-shadow: 0 0 0 8px rgba(139, 92, 246, 0.15),
                      0 12px 40px rgba(139, 92, 246, 0.4),
                      0 4px 16px rgba(0, 0, 0, 0.5);
        }
        
        .vjs-big-play-button:active {
          transform: translate(-50%, -50%) scale(0.96);
          transition: all 0.1s ease;
        }
        
        .vjs-big-play-button .vjs-icon-placeholder {
          display: flex;
          align-items: center;
          justify-content: center;
          width: 100%;
          height: 100%;
        }
        
        .vjs-big-play-button .vjs-icon-placeholder:before {
          font-size: 3.5em;
          color: #ffffff;
          text-shadow: 0 2px 12px rgba(0, 0, 0, 0.4);
          position: relative;
          left: 3px;
        }
        
        .vjs-big-play-button:hover .vjs-icon-placeholder:before {
          color: #ffffff;
          text-shadow: 0 0 20px rgba(139, 92, 246, 0.8),
                       0 2px 12px rgba(0, 0, 0, 0.5);
        }
        
        /* Control bar styling */
        .video-js .vjs-control-bar {
          background: linear-gradient(to top, rgba(0, 0, 0, 0.8), transparent);
          backdrop-filter: blur(8px);
          height: 4em;
        }
        
        .video-js .vjs-play-control,
        .video-js .vjs-volume-panel,
        .video-js .vjs-fullscreen-control {
          transition: all 0.2s;
        }
        
        .video-js .vjs-play-control:hover,
        .video-js .vjs-volume-panel:hover,
        .video-js .vjs-fullscreen-control:hover {
          color: rgb(139, 92, 246);
        }
        
        /* Live badge */
        .vjs-live-control {
          display: flex;
          align-items: center;
          font-weight: bold;
          color: white;
          background-color: rgb(220, 38, 38);
          padding: 0 12px;
          border-radius: 4px;
          margin-left: 8px;
          cursor: default !important;
          pointer-events: none;
        }
        
        .vjs-live-control:before {
          content: "●";
          margin-right: 6px;
          animation: pulse 2s infinite;
        }
        
        @keyframes pulse {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.5; }
        }
        
        /* Remove seekbar and time controls */
        .vjs-progress-control,
        .vjs-remaining-time,
        .vjs-current-time,
        .vjs-time-divider,
        .vjs-duration,
        .vjs-seek-to-live-control {
          display: none !important;
        }
        
        /* Loading spinner */
        .vjs-loading-spinner {
          border-color: rgba(139, 92, 246, 0.8);
        }
        
        /* Error display */
        .vjs-error .vjs-error-display {
          background: rgba(0, 0, 0, 0.95);
        }
        
        .vjs-error .vjs-error-display:before {
          content: 'Stream não disponível';
          font-size: 1.5em;
          margin-bottom: 1em;
        }
        
        /* Poster image */
        .vjs-poster {
          background-size: cover;
        }
      `}</style>
    </div>
  );
}
