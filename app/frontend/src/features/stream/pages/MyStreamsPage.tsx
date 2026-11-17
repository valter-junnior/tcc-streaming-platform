import { useState } from "react";
import { ArrowLeft, ArrowUpRight, Edit2, Eye, Loader2, Plus, Radio, Trash2 } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useMyStreams } from "../../../app/hooks/useMyStreams";
import { useUserId } from "../../../shared/hooks/useUserId";
import { routes } from "../../../app/routes";
import { CreateStreamModal } from "../components/CreateStreamModal";
import { EditStreamModal } from "../components/EditStreamModal";
import { DeleteStreamModal } from "../components/DeleteStreamModal";
import type { Stream } from "../../../app/types/stream";

const statusLabel = { LIVE: "Ao vivo", WAITING: "Aguardando", ENDED: "Encerrada" } as const;
export function MyStreamsPage() {
  const navigate = useNavigate();
  const userId = useUserId();
  const { data: streams, isLoading, error } = useMyStreams();
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [editingStream, setEditingStream] = useState<Stream | null>(null);
  const [deletingStream, setDeletingStream] = useState<Stream | null>(null);
  return <div className="studio-shell">
    <header className="site-header"><div className="site-header__inner"><button className="brand" onClick={()=>navigate(routes.home())} aria-label="StreamLab, início"><span className="brand__mark"><Radio size={20}/></span>streamlab<span className="accent">.</span></button><button className="studio-back" onClick={()=>navigate(routes.home())}><ArrowLeft size={16}/> Explorar transmissões</button></div></header>
    <main className="studio-main"><div className="studio-heading"><div><p>Seu espaço de transmissão</p><h1>Meu estúdio<span className="accent">.</span></h1><span>Gerencie suas transmissões e acompanhe o que está no ar.</span></div><button className="action-primary" onClick={()=>setIsCreateModalOpen(true)}><Plus size={18}/> Nova transmissão</button></div>
    {isLoading || !userId ? <div className="studio-message"><Loader2 className="animate-spin"/> Carregando transmissões...</div> : error ? <div className="studio-message">Não foi possível carregar suas transmissões. Verifique a conexão e tente novamente.</div> : !streams?.length ? <div className="studio-empty"><div className="studio-empty__icon"><Radio size={31}/></div><div><h2>Sua próxima transmissão começa aqui.</h2><p>Crie uma transmissão, configure seu programa no OBS e entre no ar.</p><button className="text-action" onClick={()=>setIsCreateModalOpen(true)}>Criar transmissão <ArrowUpRight size={17}/></button></div></div> : <div className="studio-list">{streams.map(stream=><article className="studio-row" key={stream.id}><button className="studio-row__open" onClick={()=>navigate(routes.dashboard(stream.id))} aria-label={`Abrir painel de ${stream.title}`}><div className="studio-row__visual"><Radio size={24}/></div><div className="studio-row__copy"><span className={`studio-status studio-status--${stream.status.toLowerCase()}`}>{statusLabel[stream.status]}</span><h2>{stream.title}</h2>{stream.description && <p>{stream.description}</p>}<span className="studio-row__viewers"><Eye size={15}/> {stream.currentViewers} assistindo <span>· Pico: {stream.viewersPeak}</span></span></div><ArrowUpRight className="studio-row__arrow" size={22}/></button><div className="studio-row__actions"><button onClick={()=>setEditingStream(stream)}><Edit2 size={15}/> Editar</button><button onClick={()=>setDeletingStream(stream)}><Trash2 size={15}/> Excluir</button></div></article>)}</div>}
    </main><CreateStreamModal isOpen={isCreateModalOpen} onClose={()=>setIsCreateModalOpen(false)}/>{editingStream && <EditStreamModal stream={editingStream} isOpen onClose={()=>setEditingStream(null)}/>} {deletingStream && <DeleteStreamModal stream={deletingStream} isOpen onClose={()=>setDeletingStream(null)}/>}
  </div>;
}
