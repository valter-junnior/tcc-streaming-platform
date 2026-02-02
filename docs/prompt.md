Vamos iniciar a fase 2 do todo.md (faremos por completo):

nesta fase iniciamos algumas implentações do projeto, mas antes queria deifinir que usaremos a arquitetura limpa no projeto com e domain ou seja common, stream, consumer e metrics cada um é um domain e devem ter sua organização da seguinte forma, utilizarei o consumer como exemplo para os outros.

src/main/java/com/tcc/streaming/
├── consumer/      # Consumer Service
    ├── core
        ├── entities
            ├── User.java
        ├── exceptions
        ├── repositories (interfaces)
            ├── UserRepository.java
        ├── useCases (interfaces)
            ├── UserUseCase.java
        ├── Dtos (crie separado por entidade)
            ├── User
                ├── CreateUserDTO.java
    ├── application
        ├── Services 
            ├── UserService.java (impl do use case)
    ├── infrastructure
        ├── config
        ├── database
            ├── jpa (ou outro nome, escolha a melhor organização para esta parte)
                ├── repositories
                ├── entities (ou data)
                ├── mappers
        ├── middlewares (caso necessario)
        ├── http
            ├── controllers
            ├── requests (usam validation)
            ├── presenters
            ├── handlers (exceptions)
        ├── ws
            ├── controllers
├── common/        # Código compartilhado
├── stream/        # Stream Service
└── metrics/       # Metrics Service
├── StreamingPlatformApplication.java

Caso tenha faltado alguma camada ou tenha alguma duvida ou susgestao crie um questions.md no /docs para eu responder (adicione isto no utils sempre bom ter esse texto de duvida vou utilizar recorrente nos prompts)
