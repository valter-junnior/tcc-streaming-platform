estou tentando corrigir este erro no watch page:

chunk-GPLGTWJB.js?v=fd0aed79:10836 VIDEOJS: ERROR: (CODE:2 MEDIA_ERR_NETWORK) HLS playlist request error at URL: 

o fluxo é o seguinte:

quando o streamer inicia a live usamos o websocket para atualizar o status da pagina dizendo que ja está em live, mas como existe um delay para o hsl ficar disponivel pode dar erro 404 entao o certo é tentar de 5 em 5 segundos para ver se a live já está disponivel além disso se eu fizer um refresh na página o player nao carrega fica apenas aguardando mensagem mas esta dizendo que ta ao vivo, ou ponto é que quando o streamer estiver em live e o arquivo no hls ainda nao estiver disponivel quero que a mesma tela de waiting fique aparecendo até o player ficar disponivel com a live por que acho meio ruim ficar um ngc no estilo "carregando player"