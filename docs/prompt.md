leia o utils.md antes de realizar as tarefas.

tarefas:

[ ] Quando iniciamos uma stream a página do frontend para quem assiste a stream nao atualiza automaticamente. Arquivos: NginxCallbackController.java, WatchPage.tsx. 

[ ] Quando uma stream termina(status = ENDED), ela pode ser reiniciada sem problema não precisa criar uma nova stream, mude isso no backend e no frontend. Mas nesse caso precisamos de um modo que os dados de viewers e pico de viewers da ultima stream nao se mistura com a nova, ou até mesmo fazer essa parte de pico de viewers apenas na dashboard e que ela seja por um range de tempo um grafico de linha de 00:00 até 23:59 mostrando os picos de viewers de 1 em 1hr