import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiService } from "../services/apiService";
import type { UpdateStreamRequest } from "../types/stream";

export function useUpdateStream() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateStreamRequest }) =>
      apiService.updateStream(id, data),
    onSuccess: (_, variables) => {
      // Invalidate and refetch queries
      queryClient.invalidateQueries({ queryKey: ["my-streams"] });
      queryClient.invalidateQueries({ queryKey: ["stream", variables.id] });
      queryClient.invalidateQueries({ queryKey: ["live-streams"] });
    },
  });
}
