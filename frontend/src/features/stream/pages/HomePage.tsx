import { useState } from "react";
import { Video, Play, TrendingUp } from "lucide-react";
import { CreateStreamModal } from "../components/CreateStreamModal";

export function HomePage() {
  const [isModalOpen, setIsModalOpen] = useState(false);

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900">
      <div className="container mx-auto px-4 py-16">
        {/* Header */}
        <header className="text-center mb-16">
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
              Veja o número de espectadores atualizando ao vivo via WebSocket.
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
