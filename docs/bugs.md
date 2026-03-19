---

### 2.4 FFmpeg transcodifica sempre para 4 qualidades mesmo sem viewers

**Arquivo:** `transcode-ffmpeg.sh`

O script sempre gera as 4 variantes (1080p/5Mbps, 720p/2.8Mbps, 480p/1.4Mbps, 360p/0.8Mbps) independentemente de haver viewers. Em hardware limitado isso pode causar lag no encoder e aumentar a latência.

**Melhoria:** usar `-preset veryfast` (em vez de `fast`) para reduzir uso de CPU com qualidade ligeiramente menor, ou começar apenas com 720p e escalar as qualidades conforme demanda (ABR adaptativo via lógica no script). Para um TCC, ao menos trocar `fast` por `veryfast` reduz significativamente a carga de CPU sem impacto perceptível.

---