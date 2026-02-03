import { useState } from "react";
import { X, Loader2, AlertTriangle, AlertCircle } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useDeleteStream } from "../../../app/hooks/useDeleteStream";
import { useUserId } from "../../../shared/hooks/useUserId";
import { routes } from "../../../app/routes";
import type { Stream } from "../../../app/types/stream";

interface DeleteStreamModalProps {
  stream: Stream;
  isOpen: boolean;
  onClose: () => void;
}

export function DeleteStreamModal({
  stream,
  isOpen,
  onClose,
}: DeleteStreamModalProps) {
  const navigate = useNavigate();
  const userId = useUserId();
  const deleteStream = useDeleteStream();
  const [error, setError] = useState<string | null>(null);

  const isLive = stream.status === "LIVE";

  const handleDelete = async () => {
    setError(null);

    if (!userId) {
      setError("Erro de autenticação");
      return;
    }

    if (isLive) {
      setError("Não é possível excluir uma stream que está ao vivo");
      return;
    }

    try {
      await deleteStream.mutateAsync({
        id: stream.id,
        ownerId: userId,
      });

      // Close modal and redirect to my streams
      onClose();
      navigate(routes.myStreams());
    } catch (err: any) {
      console.error("Error deleting stream:", err);

      if (err.response?.status === 403) {
        setError("Você não tem permissão para excluir esta stream");
      } else if (err.response?.status === 400) {
        setError("Não é possível excluir uma stream que está ao vivo");
      } else {
        setError(
          err.response?.data?.message ||
            "Erro ao excluir stream. Tente novamente.",
        );
      }
    }
  };

  const handleClose = () => {
    if (!deleteStream.isPending) {
      setError(null);
      onClose();
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4 z-50">
      <div className="bg-slate-800 rounded-lg shadow-xl max-w-md w-full border border-slate-700">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-slate-700">
          <h2 className="text-2xl font-bold text-white flex items-center gap-2">
            <AlertTriangle className="w-6 h-6 text-red-500" />
            Excluir Stream
          </h2>
          <button
            onClick={handleClose}
            disabled={deleteStream.isPending}
            className="text-slate-400 hover:text-white transition-colors disabled:opacity-50"
          >
            <X className="w-6 h-6" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 space-y-4">
          {/* Warning for LIVE streams */}
          {isLive ? (
            <div className="bg-red-500/10 border border-red-500 rounded-lg p-4">
              <div className="flex items-start gap-3">
                <AlertCircle className="w-5 h-5 text-red-500 flex-shrink-0 mt-0.5" />
                <div>
                  <p className="text-red-400 font-semibold mb-1">
                    Stream está ao vivo
                  </p>
                  <p className="text-red-300 text-sm">
                    Não é possível excluir uma stream que está em transmissão.
                    Encerre a transmissão primeiro.
                  </p>
                </div>
              </div>
            </div>
          ) : (
            <>
              <p className="text-slate-300">
                Tem certeza que deseja excluir a stream{" "}
                <span className="font-semibold text-white">
                  "{stream.title}"
                </span>
                ?
              </p>
              <p className="text-slate-400 text-sm">
                Esta ação não pode ser desfeita. Todos os dados da stream serão
                permanentemente removidos.
              </p>
            </>
          )}

          {/* Error Message */}
          {error && (
            <div className="flex items-center gap-2 p-3 bg-red-500/10 border border-red-500 rounded-lg text-red-400 text-sm">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {/* Actions */}
          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={handleClose}
              disabled={deleteStream.isPending}
              className="flex-1 px-4 py-2 bg-slate-700 hover:bg-slate-600 text-white rounded-lg transition-colors disabled:opacity-50"
            >
              Cancelar
            </button>
            <button
              type="button"
              onClick={handleDelete}
              disabled={deleteStream.isPending || isLive}
              className="flex-1 px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
            >
              {deleteStream.isPending ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Excluindo...
                </>
              ) : (
                "Excluir"
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
