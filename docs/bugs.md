pedi que fosse verificado a feature que mostra os viewers em tempo real no frontend mas ele ta chamando     

const pollInterval = setInterval(() => {
      if (streamId) {
        loadStream();
      }
    }, 1000); 
    
que faz que a pagina atualize por completo e bugando tudo implemente da forma mais correta essa atualizacao de viewers e alis mesmo assim a quantidade de viewers tem continuado 0 sendo que eu abrir a pagina em dois navegadorees diferente e no guia anonima veifique toda essa feature por completo