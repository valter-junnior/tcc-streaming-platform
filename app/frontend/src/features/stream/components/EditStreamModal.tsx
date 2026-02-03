import { useState, type FormEvent } from "react";
import { X, Loader2, CheckCircle, AlertCircle } from "lucide-react";
import { useUpdateStream } from "../../../app/hooks/useUpdateStream";
import { useUserId } from "../../../shared/hooks/useUserId";
import type { Stream } from "../../../app/types/stream";
import { logger } from "../../../shared/lib/logger";

interface EditStreamModalProps {
  stream: Stream;
  isOpen: boolean;
  onClose: () => void;
}

export function EditStreamModal({
  stream,
  isOpen,
  onClose,
}: EditStreamModalProps) {
  const userId = useUserId();
  const updateStream = useUpdateStream();
  const [title, setTitle] = useState(stream.title);
  const [description, setDescription] = useState(stream.description);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!title.trim()) {
      setError("Título é obrigatório");
      return;
    }

    if (title.length < 3) {
      setError("Título deve ter pelo menos 3 caracteres");
      return;
    }

    if (!userId) {
      setError("Erro de autenticação");
      return;
    }

    try {
      await updateStream.mutateAsync({
        id: stream.id,
        data: {
          title: title.trim(),
          description: description.trim(),
          ownerId: userId,
        },
      });

      // Close modal on success
      handleClose();
    } catch (err: any) {
      logger.error("Error updating stream", err);

      if (err.response?.status === 403) {
        setError("Você não tem permissão para editar esta stream");
      } else {
        setError(
          err.response?.data?.message ||
            "Erro ao atualizar stream. Tente novamente.",
        );
      }
    }
  };

  const handleClose = () => {
    if (!updateStream.isPending) {
      setTitle(stream.title);
      setDescription(stream.description);
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
          <h2 className="text-2xl font-bold text-white">Editar Stream</h2>
          <button
            onClick={handleClose}
            disabled={updateStream.isPending}
            className="text-slate-400 hover:text-white transition-colors disabled:opacity-50"
          >
            <X className="w-6 h-6" />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {/* Title */}
          <div>
            <label
              htmlFor="title"
              className="block text-sm font-medium text-slate-300 mb-2"
            >
              Título da Stream *
            </label>
            <input
              id="title"
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Ex: Jogando Minecraft"
              maxLength={100}
              disabled={updateStream.isPending}
              className="w-full px-4 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent disabled:opacity-50"
            />
          </div>

          {/* Description */}
          <div>
            <label
              htmlFor="description"
              className="block text-sm font-medium text-slate-300 mb-2"
            >
              Descrição
            </label>
            <textarea
              id="description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Descreva sua transmissão (opcional)"
              maxLength={500}
              rows={4}
              disabled={updateStream.isPending}
              className="w-full px-4 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent disabled:opacity-50 resize-none"
            />
          </div>

          {/* Error Message */}
          {error && (
            <div className="flex items-center gap-2 p-3 bg-red-500/10 border border-red-500 rounded-lg text-red-400 text-sm">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {/* Success Message */}
          {updateStream.isSuccess && (
            <div className="flex items-center gap-2 p-3 bg-green-500/10 border border-green-500 rounded-lg text-green-400 text-sm">
              <CheckCircle className="w-4 h-4 flex-shrink-0" />
              <span>Stream atualizada com sucesso!</span>
            </div>
          )}

          {/* Actions */}
          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={handleClose}
              disabled={updateStream.isPending}
              className="flex-1 px-4 py-2 bg-slate-700 hover:bg-slate-600 text-white rounded-lg transition-colors disabled:opacity-50"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={updateStream.isPending}
              className="flex-1 px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-lg transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
            >
              {updateStream.isPending ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Salvando...
                </>
              ) : (
                "Salvar"
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
