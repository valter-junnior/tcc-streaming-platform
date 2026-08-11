import { useState } from "react";
import { Eye, Loader2, Play, Plus, Radio, Video } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { CreateStreamModal } from "../components/CreateStreamModal";
import { useLiveStreams } from "../../../app/hooks/useLiveStreams";
import { routes } from "../../../app/routes";

export function HomePage() {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const navigate = useNavigate();
  const { data: liveStreams, isLoading } = useLiveStreams();

  return (
    <div className="min-h-screen bg-[#0b0d10] text-white">
      <header className="border-b border-white/10 bg-[#0b0d10]/95">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-5 py-4">
          <button
            onClick={() => navigate(routes.home())}
            className="flex items-center gap-2 text-lg font-semibold tracking-tight"
          >
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-violet-600">
              <Radio className="h-4 w-4" />
            </span>
            StreamLab
          </button>

          <button
            onClick={() => navigate(routes.myStreams())}
            className="rounded-md px-3 py-2 text-sm text-slate-300 transition hover:bg-white/10 hover:text-white"
          >
            Minhas transmissões
          </button>
        </div>
      </header>

      <main className="mx-auto max-w-7xl px-5 pb-16">
        <section className="flex flex-col gap-6 border-b border-white/10 py-16 md:flex-row md:items-end md:justify-between">
          <div className="max-w-2xl">
            <p className="mb-3 text-sm font-medium uppercase tracking-[0.18em] text-violet-400">
              Streaming ao vivo
            </p>
            <h1 className="text-4xl font-semibold tracking-tight text-white md:text-5xl">
              Transmita. Assista. Ao vivo.
            </h1>
            <p className="mt-4 max-w-xl text-base leading-7 text-slate-400">
              Publique sua live via OBS e acompanhe o conteúdo direto do navegador.
            </p>
          </div>

          <button
            onClick={() => setIsModalOpen(true)}
            className="inline-flex w-fit items-center gap-2 rounded-md bg-violet-600 px-4 py-3 text-sm font-semibold transition hover:bg-violet-500"
          >
            <Plus className="h-4 w-4" />
            Nova transmissão
          </button>
        </section>

        <section className="pt-10">
          <div className="mb-5 flex items-center justify-between">
            <div>
              <h2 className="text-xl font-semibold">Ao vivo agora</h2>
              <p className="mt-1 text-sm text-slate-500">
                {liveStreams?.length ?? 0} transmissões disponíveis
              </p>
            </div>
          </div>

          {isLoading ? (
            <div className="flex min-h-48 items-center justify-center rounded-xl border border-white/10 bg-white/[0.03]">
              <Loader2 className="h-6 w-6 animate-spin text-violet-400" />
            </div>
          ) : liveStreams && liveStreams.length > 0 ? (
            <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
              {liveStreams.map((stream) => (
                <button
                  key={stream.id}
                  onClick={() => navigate(routes.watch(stream.id))}
                  className="group overflow-hidden rounded-xl border border-white/10 bg-white/[0.04] text-left transition hover:-translate-y-0.5 hover:border-violet-500/70 hover:bg-white/[0.07]"
                >
                  <div className="relative flex aspect-video items-center justify-center overflow-hidden bg-gradient-to-br from-violet-950 via-slate-900 to-[#11151b]">
                    <Video className="h-10 w-10 text-white/30 transition group-hover:scale-110 group-hover:text-violet-300" />
                    <span className="absolute left-3 top-3 inline-flex items-center gap-1.5 rounded bg-red-500 px-2 py-1 text-[10px] font-bold tracking-wide">
                      <span className="h-1.5 w-1.5 rounded-full bg-white" />
                      AO VIVO
                    </span>
                  </div>
                  <div className="p-4">
                    <h3 className="truncate font-medium text-white">{stream.title}</h3>
                    <div className="mt-2 flex items-center gap-1.5 text-sm text-slate-400">
                      <Eye className="h-4 w-4" />
                      {stream.currentViewers} assistindo
                    </div>
                  </div>
                </button>
              ))}
            </div>
          ) : (
            <div className="rounded-xl border border-dashed border-white/15 bg-white/[0.02] px-6 py-14 text-center">
              <Play className="mx-auto h-8 w-8 text-slate-600" />
              <p className="mt-3 text-sm text-slate-400">Nenhuma transmissão ao vivo no momento.</p>
              <button
                onClick={() => setIsModalOpen(true)}
                className="mt-5 text-sm font-medium text-violet-400 hover:text-violet-300"
              >
                Começar uma transmissão
              </button>
            </div>
          )}
        </section>
      </main>

      <CreateStreamModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
      />
    </div>
  );
}
