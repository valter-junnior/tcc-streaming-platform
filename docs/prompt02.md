Agora faça o seguinte roder vários testes no seguinte fluxo:

1. Modifique o .env para usar o Nginx + FFMpeg
2. Faça um rebuild do docker compose para executar os container 
3. teste o rtmp e os enpoints das lives: 
    3.1: Crie uma live
    3.2: inicie uma transmissao fake
    3.3: acesse o hls para ver se a saída está como esperado
4. Se tudo estiver ok, siga para próxima combinação Nginx + Gstreamer, e assim por diante com o srs + ffmepg e depois o srs + gstremer
5. Se der erro nos testes analise os logs do rtmp, verifique e corrija.

6. analise tambem o backend e frontend para ver se tem funcionaldiades e classes que fazem sentido, caso a funcionalidade não esteja relamente sendo usuado apenas remova

Objetivo: 
Verificar se toda a plataforma está funcionando corretamente para as combinaçãos e também corrigir possiveis erros e falhas,

Erros já conhecidos: 
1. Quando fazemos o build dos containers e inicimos uma live no nginx independente da combinação está demorando mais de minutos para começar o hls gerar alguma coisa e conseguir assistir no frontend, analise por que inicialmente funcionava corretamente

2. O srs está buildando o container mas ao conectar o OBS ao rtmp ele fica apenas tentando reconectar várias vezes e a live não inico

Obs: Qualquer coisa adicione logs para durante o processo para saber o que está acontecendo
Obs: Evite mudar regras de negocio aa questão aqui é deixar o mais simples possivel e conseguirmos implementar as funcoes para o objetivo final que é um projeto pronto para ser testado e usado no tcc, com sobrecarrga de usuarios vendo a live

Por fim gere apenas um .md dizendo o que foi aplicado e quais os bugs acontecia.

