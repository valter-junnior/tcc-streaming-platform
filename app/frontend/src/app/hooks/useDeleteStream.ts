import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiService } from "../services/apiService";

export function useDeleteStream() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, ownerId }: { id: string; ownerId: string }) =>
      apiService.deleteStream(id, ownerId),
    onSuccess: () => {
      // Invalidate and refetch queries
      queryClient.invalidateQueries({ queryKey: ["my-streams"] });
      queryClient.invalidateQueries({ queryKey: ["live-streams"] });
    },
  });
}
