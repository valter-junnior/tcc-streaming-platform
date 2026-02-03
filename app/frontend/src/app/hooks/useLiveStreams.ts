import { useQuery } from "@tanstack/react-query";
import { apiService } from "../services/apiService";

export function useLiveStreams() {
  return useQuery({
    queryKey: ["liveStreams"],
    queryFn: () => apiService.getLiveStreams(),
    refetchInterval: 30000, // Refetch every 30 seconds
    staleTime: 30000, // Consider data stale after 30 seconds
  });
}
