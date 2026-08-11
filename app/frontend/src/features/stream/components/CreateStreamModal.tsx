import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { X, Loader2 } from "lucide-react";
import { apiService } from "../../../app/services/apiService";
import { routes } from "../../../app/routes";
import type { CreateStreamRequest } from "../../../app/types/stream";
import { useUserId } from "../../../shared/hooks/useUserId";
import { logger } from "../../../shared/lib/logger";
import { getErrorMessage } from "../../../shared/utils/errorHandler";

interface CreateStreamModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function CreateStreamModal({ isOpen, onClose }: CreateStreamModalProps) {
  const navigate = useNavigate();
  const userId = useUserId();
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [isLoading, setIsLoading] = useState(false);
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
      setError("Aguarde, carregando identificação...");
      return;
    }

    setIsLoading(true);

    try {
      const request: CreateStreamRequest = {
        title: title.trim(),
        description: description.trim(),
        ownerId: userId,
      };

      const response = await apiService.createStream(request);

      // Redirect to dashboard
      navigate(routes.dashboard(response.id));
    } catch (error) {
      logger.error("Error creating stream", error);
      setError(getErrorMessage(error));
    } finally {
      setIsLoading(false);
    }
  };

  const handleClose = () => {
    if (!isLoading) {
      setTitle("");
      setDescription("");
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
          <h2 className="text-xl font-semibold text-white">Nova transmissão</h2>
          <button
            onClick={handleClose}
            disabled={isLoading}
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
              disabled={isLoading}
              className="w-full rounded-md border border-white/10 bg-white/[0.06] px-3 py-2 text-white placeholder-slate-500 focus:border-violet-500 focus:outline-none focus:ring-1 focus:ring-violet-500 disabled:opacity-50"
            />
            <p className="mt-1 text-xs text-slate-500">
              {title.length}/100 caracteres
            </p>
          </div>

          {/* Description */}
          <div>
            <label
              htmlFor="description"
              className="mb-2 block text-xs font-medium uppercase tracking-wide text-slate-400"
            >
              Descrição (opcional)
            </label>
            <textarea
              id="description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Descreva sua transmissão..."
              rows={3}
              maxLength={500}
              disabled={isLoading}
              className="w-full resize-none rounded-md border border-white/10 bg-white/[0.06] px-3 py-2 text-white placeholder-slate-500 focus:border-violet-500 focus:outline-none focus:ring-1 focus:ring-violet-500 disabled:opacity-50"
            />
            <p className="mt-1 text-xs text-slate-500">
              {description.length}/500 caracteres
            </p>
          </div>

          {/* Error Message */}
          {error && (
            <div className="bg-red-500/10 border border-red-500/50 rounded-lg p-3">
              <p className="text-red-400 text-sm">{error}</p>
            </div>
          )}

          {/* Actions */}
          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={handleClose}
              disabled={isLoading}
              className="flex-1 rounded-md border border-white/10 bg-white/[0.06] px-4 py-2 text-white transition-colors hover:bg-white/10 disabled:opacity-50"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={isLoading || !title.trim()}
              className="flex-1 rounded-md bg-violet-600 px-4 py-2 text-white transition-colors hover:bg-violet-500 disabled:opacity-50 flex items-center justify-center gap-2"
            >
              {isLoading ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Criando...
                </>
              ) : (
                "Criar Stream"
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
