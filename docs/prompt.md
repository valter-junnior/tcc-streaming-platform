corrija os erros na parte do rtmp com base no seguinte:

`on_publish`: Chamado quando um publicador (por exemplo, OBS, FFmpeg) tenta iniciar uma transmissão.
Finalidade: É usado principalmente para autenticação (verificação de uma chave de transmissão) e autorização. Seu serviço de backend pode responder com um código de status HTTP 2xx para permitir a transmissão, um 3xx para redirecioná-la para outro local ou qualquer outro código para negar a conexão.

`on_publish_done`: Chamado quando um publicador para de transmitir.
Finalidade: Usado para executar tarefas de limpeza ou atualizar o status da transmissão em seu banco de dados (por exemplo, marcar uma transmissão como "offline").

`on_play`: Acionado quando um espectador tenta iniciar a reprodução de uma transmissão.

Finalidade: Pode ser usado para autenticação e autorização do espectador.

`on_play_done`: Chamado quando um espectador se desconecta de uma transmissão.

Finalidade: Útil para rastrear a contagem de espectadores ou outras análises.

`on_update`: Alterna o modo estrito para callbacks `on_update`, que estão relacionados a atualizações e retransmissão de transmissões.
on_record_done: Chamado após a conclusão de uma gravação de fluxo.

corrija controllers e os arquivos do ngxin.conf dockerfile e transcode.sh