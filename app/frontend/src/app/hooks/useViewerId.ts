import { useState, useEffect } from "react";

export function useViewerId(): string {
  const [viewerId] = useState(() => {
    const stored = localStorage.getItem("viewerId");

    if (stored) {
      return stored;
    }

    const newId = `viewer_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    localStorage.setItem("viewerId", newId);

    return newId;
  });

  useEffect(() => {
    const stored = localStorage.getItem("viewerId");
    if (stored !== viewerId) {
      localStorage.setItem("viewerId", viewerId);
    }
  }, [viewerId]);

  return viewerId;
}
