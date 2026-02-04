import { Clock, Circle } from "lucide-react";
import { useStreamingTime } from "../../../shared/hooks/useStreamingTime";

interface StreamingTimeProps {
  startedAt: string | null;
  endedAt?: string | null;
  status: string;
  size?: "small" | "medium" | "large";
  showIcon?: boolean;
}

/**
 * Componente que exibe o tempo de streaming
 * - Se LIVE: mostra tempo em tempo real
 * - Se ENDED: mostra duração total baseado em startedAt e endedAt
 */
export function StreamingTime({
  startedAt,
  endedAt,
  status,
  size = "medium",
  showIcon = true,
}: StreamingTimeProps) {
  const { duration, isLive, isEnded } = useStreamingTime(
    startedAt,
    endedAt,
    status,
  );

  const sizeClasses = {
    small: "text-sm",
    medium: "text-base",
    large: "text-xl font-bold",
  };

  const iconSizes = {
    small: "w-3 h-3",
    medium: "w-4 h-4",
    large: "w-5 h-5",
  };

  // Não mostrar se não tem dados
  if (!isLive && !isEnded) {
    return null;
  }

  return (
    <div className={`inline-flex items-center gap-2 ${sizeClasses[size]}`}>
      {showIcon && (
        <>
          {isLive && (
            <Circle
              className={`${iconSizes[size]} text-red-500 fill-red-500 animate-pulse`}
            />
          )}
          <Clock className={`${iconSizes[size]} text-slate-400`} />
        </>
      )}
      <span className="text-white font-mono">{duration}</span>
      {isEnded && (
        <span className="text-slate-400 text-xs ml-1">(finalizado)</span>
      )}
    </div>
  );
}
