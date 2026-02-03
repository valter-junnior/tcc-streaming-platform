import { useState } from "react";
import {
  Video,
  Play,
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
      <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 flex items-center justify-center">
        <Loader2 className="w-12 h-12 text-purple-500 animate-spin" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 flex items-center justify-center">
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
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900">
      <div className="container mx-auto px-4 py-16">
        {/* Header */}
        <div className="mb-12">
          <div className="flex items-center gap-3 mb-4">
            <Video className="w-10 h-10 text-purple-400" />
            <h1 className="text-4xl font-bold text-white">Minhas Lives</h1>
          </div>
          <p className="text-slate-300">
            Gerencie suas transmissões, edite informações e acompanhe
            estatísticas
          </p>
        </div>

        {/* Streams List */}
        {!streams || streams.length === 0 ? (
          <div className="max-w-2xl mx-auto text-center py-16">
            <div className="bg-slate-800/50 backdrop-blur rounded-lg p-12 border border-slate-700">
              <Video className="w-16 h-16 text-slate-600 mx-auto mb-4" />
              <h2 className="text-2xl font-semibold text-white mb-2">
                Nenhuma stream criada ainda
              </h2>
              <p className="text-slate-400 mb-6">
                Crie sua primeira transmissão para começar
              </p>
              <button
                onClick={() => setIsCreateModalOpen(true)}
                className="inline-flex items-center gap-2 bg-purple-600 hover:bg-purple-700 text-white font-semibold px-6 py-3 rounded-lg transition-colors"
              >
                <Play className="w-5 h-5" />
                Criar Stream
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
