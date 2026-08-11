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
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4 backdrop-blur-sm">
      <div className="w-full max-w-md rounded-xl border border-white/10 bg-[#15181d] shadow-2xl">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-white/10 p-5">
          <h2 className="text-xl font-semibold text-white">Editar transmissão</h2>
          <button
            onClick={handleClose}
            disabled={updateStream.isPending}
            className="text-slate-400 hover:text-white transition-colors disabled:opacity-50"
          >
            <X className="w-6 h-6" />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-5 p-5">
          {/* Title */}
          <div>
            <label
              htmlFor="title"
              className="mb-2 block text-xs font-medium uppercase tracking-wide text-slate-400"
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
              className="w-full rounded-md border border-white/10 bg-white/[0.06] px-3 py-2 text-white placeholder-slate-500 focus:border-violet-500 focus:outline-none focus:ring-1 focus:ring-violet-500 disabled:opacity-50"
            />
          </div>

          {/* Description */}
          <div>
            <label
              htmlFor="description"
              className="mb-2 block text-xs font-medium uppercase tracking-wide text-slate-400"
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
              className="w-full resize-none rounded-md border border-white/10 bg-white/[0.06] px-3 py-2 text-white placeholder-slate-500 focus:border-violet-500 focus:outline-none focus:ring-1 focus:ring-violet-500 disabled:opacity-50"
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
              className="flex-1 rounded-md border border-white/10 bg-white/[0.06] px-4 py-2 text-white transition-colors hover:bg-white/10 disabled:opacity-50"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={updateStream.isPending}
              className="flex-1 rounded-md bg-violet-600 px-4 py-2 text-white transition-colors hover:bg-violet-500 disabled:opacity-50 flex items-center justify-center gap-2"
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
