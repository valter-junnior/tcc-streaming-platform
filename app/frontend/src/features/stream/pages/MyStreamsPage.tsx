import { useState } from "react";
import {
  Video,
  Play,
  Plus,
  Edit2,
  Trash2,
  Loader2,
  AlertCircle,
  Eye,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useMyStreams } from "../../../app/hooks/useMyStreams";
import { useUserId } from "../../../shared/hooks/useUserId";
import { routes } from "../../../app/routes";
import { CreateStreamModal } from "../components/CreateStreamModal";
import { EditStreamModal } from "../components/EditStreamModal";
import { DeleteStreamModal } from "../components/DeleteStreamModal";
import type { Stream } from "../../../app/types/stream";

export function MyStreamsPage() {
  const navigate = useNavigate();
  const userId = useUserId();
  const { data: streams, isLoading, error } = useMyStreams();
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [editingStream, setEditingStream] = useState<Stream | null>(null);
  const [deletingStream, setDeletingStream] = useState<Stream | null>(null);

  if (isLoading || !userId) {
    return (
      <div className="min-h-screen bg-[#0b0d10] flex items-center justify-center">
        <Loader2 className="w-12 h-12 text-purple-500 animate-spin" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-[#0b0d10] flex items-center justify-center">
        <div className="bg-red-500/10 border border-red-500 rounded-lg p-6 max-w-md">
          <div className="flex items-center gap-3 text-red-500">
            <AlertCircle className="w-6 h-6" />
            <p>Erro ao carregar suas streams</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#0b0d10]">
      <div className="container mx-auto max-w-7xl px-5 py-10">
        {/* Header */}
        <div className="mb-10 flex flex-col gap-4 border-b border-white/10 pb-8 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <div className="mb-3 flex items-center gap-2 text-violet-400">
              <Video className="h-5 w-5" />
              <span className="text-sm font-medium uppercase tracking-[0.16em]">Studio</span>
            </div>
            <h1 className="text-3xl font-semibold tracking-tight text-white">Minhas transmissões</h1>
            <p className="mt-2 text-sm text-slate-500">Acesse o painel ou atualize os dados da live.</p>
          </div>
          <button
            onClick={() => setIsCreateModalOpen(true)}
            className="inline-flex w-fit items-center gap-2 rounded-md bg-violet-600 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-violet-500"
          >
            <Plus className="h-4 w-4" />
            Nova transmissão
          </button>
        </div>

        {/* Streams List */}
        {!streams || streams.length === 0 ? (
          <div className="mx-auto max-w-2xl py-16 text-center">
            <div className="rounded-xl border border-dashed border-white/15 bg-white/[0.02] p-12">
              <Video className="w-16 h-16 text-slate-600 mx-auto mb-4" />
              <h2 className="text-2xl font-semibold text-white mb-2">
                Nenhuma transmissão criada
              </h2>
              <p className="text-slate-400 mb-6">
                Crie uma transmissão para começar.
              </p>
              <button
                onClick={() => setIsCreateModalOpen(true)}
                className="inline-flex items-center gap-2 bg-purple-600 hover:bg-purple-700 text-white font-semibold px-6 py-3 rounded-lg transition-colors"
              >
                <Play className="w-5 h-5" />
                Criar transmissão
              </button>
            </div>
          </div>
        ) : (
          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
            {streams.map((stream) => (
              <div
                key={stream.id}
                className="bg-slate-800/70 backdrop-blur rounded-lg border border-slate-700 overflow-hidden hover:border-purple-500 transition-all"
              >
                {/* Thumbnail */}
                <div
                  onClick={() => navigate(routes.dashboard(stream.id))}
                  className="aspect-video bg-gradient-to-br from-purple-900/50 to-slate-900 flex items-center justify-center relative cursor-pointer hover:opacity-90 transition-opacity"
                >
                  <Play className="w-16 h-16 text-white/80" />
                  {stream.status === "LIVE" && (
                    <div className="absolute top-2 left-2 bg-red-600 text-white text-xs font-bold px-2 py-1 rounded flex items-center gap-1">
                      <span className="w-2 h-2 bg-white rounded-full animate-pulse"></span>
                      AO VIVO
                    </div>
                  )}
                  {stream.status === "WAITING" && (
                    <div className="absolute top-2 left-2 bg-yellow-600 text-white text-xs font-bold px-2 py-1 rounded">
                      AGUARDANDO
                    </div>
                  )}
                  {stream.status === "ENDED" && (
                    <div className="absolute top-2 left-2 bg-slate-600 text-white text-xs font-bold px-2 py-1 rounded">
                      ENCERRADA
                    </div>
                  )}
                </div>

                {/* Stream Info */}
                <div className="p-4">
                  <h3 className="text-lg font-semibold text-white mb-2 truncate">
                    {stream.title}
                  </h3>
                  {stream.description && (
                    <p className="text-sm text-slate-400 mb-3 line-clamp-2">
                      {stream.description}
                    </p>
                  )}

                  {/* Stats */}
                  <div className="flex items-center gap-4 text-sm text-slate-300 mb-4">
                    <div className="flex items-center gap-1">
                      <Eye className="w-4 h-4" />
                      <span>{stream.currentViewers}</span>
                    </div>
                    <div className="text-slate-500">
                      Pico: {stream.viewersPeak}
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="flex gap-2">
                    <button
                      onClick={() => setEditingStream(stream)}
                      className="flex-1 flex items-center justify-center gap-2 bg-slate-700 hover:bg-slate-600 text-white px-4 py-2 rounded-lg transition-colors text-sm"
                    >
                      <Edit2 className="w-4 h-4" />
                      Editar
                    </button>
                    <button
                      onClick={() => setDeletingStream(stream)}
                      className="flex-1 flex items-center justify-center gap-2 bg-red-600/20 hover:bg-red-600/30 text-red-400 px-4 py-2 rounded-lg transition-colors text-sm border border-red-600/30"
                    >
                      <Trash2 className="w-4 h-4" />
                      Excluir
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Modals */}
      <CreateStreamModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
      />
      {editingStream && (
        <EditStreamModal
          stream={editingStream}
          isOpen={true}
          onClose={() => setEditingStream(null)}
        />
      )}
      {deletingStream && (
        <DeleteStreamModal
          stream={deletingStream}
          isOpen={true}
          onClose={() => setDeletingStream(null)}
        />
      )}
    </div>
  );
}
