import { useQuery } from "@tanstack/react-query";
import { apiService } from "../services/apiService";
import { useUserId } from "../../shared/hooks/useUserId";

export function useMyStreams() {
  const userId = useUserId();

  return useQuery({
    queryKey: ["my-streams", userId],
    queryFn: () => apiService.getMyStreams(userId!),
    enabled: !!userId,
    staleTime: 1000 * 30, // 30 seconds
    refetchInterval: 1000 * 60, // Refetch every 60 seconds
  });
}
