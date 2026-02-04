import { useState } from "react";
import { Video, Play, TrendingUp, Eye, Loader2, ListVideo } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { CreateStreamModal } from "../components/CreateStreamModal";
import { useLiveStreams } from "../../../app/hooks/useLiveStreams";
import { routes } from "../../../app/routes";

export function HomePage() {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const navigate = useNavigate();
  const { data: liveStreams, isLoading } = useLiveStreams();

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900">
      <div className="container mx-auto px-4 py-16">
        {/* Header */}
        <header className="text-center mb-16 relative">
          <div className="absolute top-0 right-4">
            <button
              onClick={() => navigate(routes.myStreams())}
              className="inline-flex items-center gap-2 bg-slate-800/70 hover:bg-slate-700/70 border border-slate-700 text-white px-4 py-2 rounded-lg transition-colors"
            >
              <ListVideo className="w-5 h-5" />
              Minhas Lives
            </button>
          </div>
          <div className="flex items-center justify-center gap-3 mb-4">
            <Video className="w-12 h-12 text-purple-400" />
            <h1 className="text-5xl font-bold text-white">StreamLab</h1>
          </div>
          <p className="text-xl text-slate-300">
            Plataforma de Streaming com Análise Comparativa
          </p>
        </header>

        {/* Hero Section */}
        <div className="max-w-4xl mx-auto text-center mb-16">
          <h2 className="text-4xl font-bold text-white mb-6">
            Transmita ao vivo em segundos
          </h2>
          <p className="text-lg text-slate-300 mb-8">
            Crie sua transmissão, configure o OBS e comece a streamar. Sem
            cadastro, sem complicação.
          </p>

          <button
            onClick={() => setIsModalOpen(true)}
            className="inline-flex items-center gap-2 bg-purple-600 hover:bg-purple-700 text-white font-semibold px-8 py-4 rounded-lg text-lg transition-colors shadow-lg hover:shadow-xl"
          >
            <Play className="w-6 h-6" />
            Iniciar Streaming
          </button>
        </div>

        {/* Live Streams Section */}
        {liveStreams && liveStreams.length > 0 && (
          <div className="max-w-6xl mx-auto mb-16">
            <h3 className="text-3xl font-bold text-white mb-6 flex items-center gap-2">
              <Play className="w-8 h-8 text-red-500 animate-pulse" />
              Ao Vivo Agora
            </h3>

            {isLoading ? (
              <div className="flex items-center justify-center py-12">
                <Loader2 className="w-8 h-8 text-purple-500 animate-spin" />
              </div>
            ) : (
              <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
                {liveStreams?.map((stream) => (
                  <div
                    key={stream.id}
                    onClick={() => navigate(routes.watch(stream.id))}
                    className="bg-slate-800/70 backdrop-blur rounded-lg border border-slate-700 overflow-hidden cursor-pointer hover:border-purple-500 transition-all hover:scale-105"
                  >
                    {/* Thumbnail Placeholder */}
                    <div className="aspect-video bg-gradient-to-br from-purple-900/50 to-slate-900 flex items-center justify-center relative">
                      <Play className="w-16 h-16 text-white/80" />
                      <div className="absolute top-2 left-2 bg-red-600 text-white text-xs font-bold px-2 py-1 rounded flex items-center gap-1">
                        <span className="w-2 h-2 bg-white rounded-full animate-pulse"></span>
                        AO VIVO
                      </div>
                    </div>

                    {/* Stream Info */}
                    <div className="p-4">
                      <h4 className="text-lg font-semibold text-white mb-2 truncate">
                        {stream.title}
                      </h4>
                      {stream.description && (
                        <p className="text-sm text-slate-400 mb-3 line-clamp-2">
                          {stream.description}
                        </p>
                      )}
                      <div className="flex items-center gap-2 text-sm text-slate-300">
                        <Eye className="w-4 h-4" />
                        <span>{stream.currentViewers} assistindo</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Features */}
        <div className="max-w-5xl mx-auto grid md:grid-cols-3 gap-8 mt-20">
          <div className="bg-slate-800/50 backdrop-blur p-6 rounded-lg border border-slate-700">
            <div className="w-12 h-12 bg-purple-500/20 rounded-lg flex items-center justify-center mb-4">
              <Video className="w-6 h-6 text-purple-400" />
            </div>
            <h3 className="text-xl font-semibold text-white mb-2">
              Sem Cadastro
            </h3>
            <p className="text-slate-400">
              Clique, configure e transmita. Acesso instantâneo sem burocracia.
            </p>
          </div>

          <div className="bg-slate-800/50 backdrop-blur p-6 rounded-lg border border-slate-700">
            <div className="w-12 h-12 bg-purple-500/20 rounded-lg flex items-center justify-center mb-4">
              <TrendingUp className="w-6 h-6 text-purple-400" />
            </div>
            <h3 className="text-xl font-semibold text-white mb-2">
              Tempo Real
            </h3>
            <p className="text-slate-400">
              Veja o número de espectadores atualizando ao vivo via SSE.
            </p>
          </div>

          <div className="bg-slate-800/50 backdrop-blur p-6 rounded-lg border border-slate-700">
            <div className="w-12 h-12 bg-purple-500/20 rounded-lg flex items-center justify-center mb-4">
              <Play className="w-6 h-6 text-purple-400" />
            </div>
            <h3 className="text-xl font-semibold text-white mb-2">
              Múltiplas Qualidades
            </h3>
            <p className="text-slate-400">
              Transcodificação automática em 360p, 480p, 720p e 1080p.
            </p>
          </div>
        </div>

        {/* About Project */}
        <div className="max-w-3xl mx-auto mt-20 bg-slate-800/30 backdrop-blur p-8 rounded-lg border border-slate-700">
          <h3 className="text-2xl font-bold text-white mb-4">
            Sobre o Projeto
          </h3>
          <p className="text-slate-300 leading-relaxed mb-4">
            Este é um <strong>Trabalho de Conclusão de Curso (TCC)</strong> que
            desenvolve uma plataforma de streaming de vídeo ao vivo com foco em{" "}
            <strong>análise comparativa de tecnologias</strong>.
          </p>
          <p className="text-slate-300 leading-relaxed">
            O objetivo é comparar o desempenho de diferentes stacks tecnológicos
            (RabbitMQ vs Redis Streams vs NATS, FFmpeg vs GStreamer, Nginx-RTMP
            vs SRS) através de métricas quantitativas e qualitativas.
          </p>
        </div>
      </div>

      {/* Modal */}
      <CreateStreamModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
      />
    </div>
  );
}
