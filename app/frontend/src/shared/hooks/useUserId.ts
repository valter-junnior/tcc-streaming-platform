import { useEffect, useState } from "react";

const USER_ID_KEY = "streaming_user_id";
const EXPIRATION_KEY = "streaming_user_id_expiration";
const SEVEN_DAYS_MS = 7 * 24 * 60 * 60 * 1000;

export function useUserId() {
  const [userId, setUserId] = useState<string | null>(null);

  useEffect(() => {
    const storedUserId = localStorage.getItem(USER_ID_KEY);
    const expiration = localStorage.getItem(EXPIRATION_KEY);

    if (storedUserId && expiration) {
      const expirationTime = parseInt(expiration, 10);
      const now = Date.now();

      if (now < expirationTime) {
        setUserId(storedUserId);
        return;
      }
    }

    const newUserId = generateUserId();
    const newExpiration = Date.now() + SEVEN_DAYS_MS;

    localStorage.setItem(USER_ID_KEY, newUserId);
    localStorage.setItem(EXPIRATION_KEY, newExpiration.toString());
    setUserId(newUserId);
  }, []);

  return userId;
}

function generateUserId(): string {
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}
