import { useState, useEffect } from "react";

/**
 * Hook que retorna um viewerId único e persistente para este browser/device
 * O ID persiste entre refreshes e abas diferentes do mesmo browser
 */
export function useViewerId(): string {
  const [viewerId] = useState(() => {
    // Tentar obter do localStorage
    const stored = localStorage.getItem("viewerId");

    if (stored) {
      return stored;
    }

    // Gerar novo ID único
    const newId = `viewer_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    localStorage.setItem("viewerId", newId);

    return newId;
  });

  // Garantir que o ID está salvo
  useEffect(() => {
    const stored = localStorage.getItem("viewerId");
    if (stored !== viewerId) {
      localStorage.setItem("viewerId", viewerId);
    }
  }, [viewerId]);

  return viewerId;
}
