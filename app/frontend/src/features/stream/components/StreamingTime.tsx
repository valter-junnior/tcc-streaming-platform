import { Clock, Circle } from "lucide-react";
import { useStreamingTime } from "../../../shared/hooks/useStreamingTime";

interface StreamingTimeProps {
  startedAt: string | null;
  status: string;
  size?: "small" | "medium" | "large";
  showIcon?: boolean;
}

/**
 * Componente que exibe o tempo de streaming em tempo real
 */
export function StreamingTime({
  startedAt,
  status,
  size = "medium",
  showIcon = true,
}: StreamingTimeProps) {
  const { duration, isLive } = useStreamingTime(startedAt);

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

  // Não mostrar se não está LIVE
  if (status !== "LIVE" || !isLive) {
    return null;
  }

  return (
    <div className={`inline-flex items-center gap-2 ${sizeClasses[size]}`}>
      {showIcon && (
        <>
          <Circle
            className={`${iconSizes[size]} text-red-500 fill-red-500 animate-pulse`}
          />
          <Clock className={`${iconSizes[size]} text-slate-400`} />
        </>
      )}
      <span className="text-white font-mono">{duration}</span>
    </div>
  );
}
