import { useState } from "react";
import { ArrowUpRight, Eye, Loader2, Plus, Radio, Video } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { CreateStreamModal } from "../components/CreateStreamModal";
import { useLiveStreams } from "../../../app/hooks/useLiveStreams";
import { routes } from "../../../app/routes";

export function HomePage() {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const navigate = useNavigate();
  const { data: liveStreams, isLoading } = useLiveStreams();
  return <div className="site-shell">
    <header className="site-header"><div className="site-header__inner">
      <button className="brand" onClick={() => navigate(routes.home())} aria-label="StreamLab, início"><span className="brand__mark"><Radio size={20} /></span>streamlab<span className="accent">.</span></button>
      <nav className="site-nav" aria-label="Navegação principal"><span>Explorar</span><button onClick={() => navigate(routes.myStreams())}>Meu estúdio <ArrowUpRight size={16}/></button></nav>
    </div></header>
    <main><section className="hero"><div className="hero__inner"><div className="hero__copy">
      <div className="hero__status"><i className="signal-dot"/> Transmissões em tempo real</div>
      <h1>O momento acontece <em>agora.</em></h1>
      <p>Entre em uma transmissão ao vivo ou coloque a sua no ar. Um espaço direto para quem transmite e para quem assiste.</p>
      <button className="action-primary" onClick={() => setIsModalOpen(true)}><Plus size={19}/> Criar transmissão</button>
    </div><div className="hero__art" aria-hidden="true"><div className="hero__art-top"><span><i className="signal-dot"/> ON AIR</span><span>● ● ●</span></div><div className="hero__frequency">{Array.from({length:19},(_,i)=><i key={i}/>)}</div><div className="hero__art-bottom"><span>STREAMLAB / LIVE</span><span>00:00:00</span></div></div></div></section>
    <section className="broadcasts" aria-labelledby="broadcasts-title"><div className="section-heading"><div><h2 id="broadcasts-title">No ar agora<span className="accent">.</span></h2><p>Escolha uma transmissão para assistir.</p></div><span className="broadcast-count">{liveStreams?.length ?? 0} no ar</span></div>
      {isLoading ? <div className="empty-state"><Loader2 className="animate-spin" size={28}/><p>Procurando transmissões...</p></div> : liveStreams?.length ? <div className="broadcast-grid">{liveStreams.map((stream,index)=><button className="broadcast-card" key={stream.id} onClick={()=>navigate(routes.watch(stream.id))}><div className="broadcast-card__visual"><span className="broadcast-card__index">{String(index+1).padStart(2,"0")}</span><Video size={42} strokeWidth={1.2}/><span className="live-pill"><i className="signal-dot"/> Ao vivo</span></div><div className="broadcast-card__info"><div><h3>{stream.title}</h3><span><Eye size={15}/> {stream.currentViewers} assistindo</span></div><ArrowUpRight size={21}/></div></button>)}</div> : <div className="empty-state empty-state--editorial"><div className="empty-state__icon"><Radio size={32}/></div><div><h3>O palco está pronto.</h3><p>Ainda não há transmissões ao vivo. Seja a primeira pessoa a entrar no ar.</p><button className="text-action" onClick={()=>setIsModalOpen(true)}>Criar transmissão <ArrowUpRight size={17}/></button></div></div>}
    </section></main><footer className="site-footer"><strong>streamlab.</strong><span>Feito para acontecer ao vivo.</span></footer>
    <CreateStreamModal isOpen={isModalOpen} onClose={()=>setIsModalOpen(false)}/>
  </div>;
}
