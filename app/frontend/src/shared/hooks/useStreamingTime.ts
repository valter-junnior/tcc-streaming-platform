import { useEffect, useState, useRef } from "react";
import { configService } from "../../app/services/configService";

interface StreamingTimeResult {
  duration: string;
  isLive: boolean;
  isEnded: boolean;
  startedAt: Date | null;
}

/**
 * Hook para calcular e exibir o tempo de streaming
 * - Se LIVE: calcula em tempo real
 * - Se ENDED: calcula duração total baseado em startedAt e endedAt
 */
export function useStreamingTime(
  startedAt: string | null,
  endedAt: string | null = null,
  status: string = "WAITING",
): StreamingTimeResult {
  const [duration, setDuration] = useState<string>("00:00:00");
  const [isEnded, setIsEnded] = useState<boolean>(false);
  const intervalRef = useRef<number | null>(null);
  const isInitializedRef = useRef(false);

  useEffect(() => {
    if (!startedAt) {
      setDuration("00:00:00");
      setIsEnded(false);
      return;
    }

    // Se stream está ENDED e tem endedAt, calcular duração final
    if (status === "ENDED" && endedAt) {
      // ✅ IMPORTANTE: Limpar interval antes de calcular duração final
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
      isInitializedRef.current = false;

      setIsEnded(true);
      const start = new Date(startedAt).getTime();
      const end = new Date(endedAt).getTime();
      const diff = end - start;

      if (diff > 0) {
        const hours = Math.floor(diff / (1000 * 60 * 60));
        const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
        const seconds = Math.floor((diff % (1000 * 60)) / 1000);

        const formatted = `${hours.toString().padStart(2, "0")}:${minutes.toString().padStart(2, "0")}:${seconds.toString().padStart(2, "0")}`;
        setDuration(formatted);
      } else {
        setDuration("00:00:00");
      }
      return;
    }

    // Se não está LIVE, não fazer nada
    if (status !== "LIVE") {
      // ✅ Limpar interval se não está LIVE
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
      isInitializedRef.current = false;
      setDuration("00:00:00");
      setIsEnded(false);
      return;
    }

    // Evitar múltiplas inicializações simultâneas
    if (isInitializedRef.current) {
      return;
    }
    isInitializedRef.current = true;

    const initializeTimer = async () => {
      try {
        // Buscar configuração do backend para timezone
        const config = await configService.getConfig();

        // Calcular offset entre servidor e cliente no momento da config
        const clientNow = Date.now();
        const serverClientDiff = clientNow - config.serverTimestamp;

        // Parsear data do backend
        let startTime = new Date(startedAt);

        // Se o backend retorna timestamp no futuro, há problema de timezone
        // Ajustar baseado na diferença entre timezones
        const now = Date.now();
        const rawDiff = now - startTime.getTime();

        if (rawDiff < -300000) {
          // Se está mais de 5min no futuro
          // Aplicar offset de timezone (assumindo que backend está em UTC)
          const localOffset = new Date().getTimezoneOffset() * 60 * 1000;
          startTime = new Date(startTime.getTime() - localOffset);
        }

        const updateDuration = () => {
          const clientNow = Date.now();
          // Ajustar tempo do cliente com base no offset do servidor
          const adjustedClientTime = clientNow - serverClientDiff;
          const diff = adjustedClientTime - startTime.getTime();

          if (diff <= 0) {
            setDuration("00:00:00");
            return;
          }

          const hours = Math.floor(diff / (1000 * 60 * 60));
          const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
          const seconds = Math.floor((diff % (1000 * 60)) / 1000);

          const formatted = `${hours.toString().padStart(2, "0")}:${minutes.toString().padStart(2, "0")}:${seconds.toString().padStart(2, "0")}`;
          setDuration(formatted);
        };

        // Atualizar imediatamente
        updateDuration();

        // Atualizar a cada segundo
        intervalRef.current = window.setInterval(updateDuration, 1000);
      } catch (error) {
        console.error("[useStreamingTime] Error initializing timer:", error);
        setDuration("--:--:--");
      }
    };

    initializeTimer();

    return () => {
      isInitializedRef.current = false;
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
      setIsEnded(false);
    };
  }, [startedAt, endedAt, status]);

  return {
    duration,
    isLive: status === "LIVE" && !!startedAt,
    isEnded: status === "ENDED" && !!startedAt && !!endedAt,
    startedAt: startedAt ? new Date(startedAt) : null,
  };
}
