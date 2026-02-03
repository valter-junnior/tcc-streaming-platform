import { useEffect, useRef } from "react";
import videojs from "video.js";
import "video.js/dist/video-js.css";
import type Player from "video.js/dist/types/player";

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
        console.log("Video.js: Manifest loaded successfully");
        onReady?.();
      });

      player.on("error", () => {
        const error = player.error();
        console.log("Video.js error:", error);

        if (error && error.code === 4) {
          // MEDIA_ERR_SRC_NOT_SUPPORTED (404 or similar)
          if (retryCountRef.current < MAX_RETRIES) {
            retryCountRef.current++;
            console.log(
              `Video.js: Retry ${retryCountRef.current}/${MAX_RETRIES} in ${RETRY_DELAY}ms`,
            );

            retryTimeoutRef.current = window.setTimeout(() => {
              if (playerRef.current) {
                playerRef.current.src({
                  src: hlsUrl,
                  type: "application/x-mpegURL",
                });
              }
            }, RETRY_DELAY);
          }
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
          border: none;
          background-color: rgba(139, 92, 246, 0.9);
          width: 80px;
          height: 80px;
          border-radius: 50%;
          transition: all 0.3s;
        }
        
        .vjs-big-play-button:hover {
          background-color: rgba(139, 92, 246, 1);
          transform: scale(1.1);
        }
        
        .vjs-big-play-button .vjs-icon-placeholder:before {
          font-size: 3em;
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
