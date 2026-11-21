<!-- Título -->
<h1 align="center"> Minhas Contas - Backend </h1>

<h6 align="right">editado pela última vez: 15/04/2026</h6>

*******
### Índice 

* [Descrição do Projeto](#descrição-do-projeto)
* [Status do Projeto](#status-do-projeto)
* [Pessoas Desenvolvedoras](#pessoas-desenvolvedoras)
* [Funcionalidades e Demonstração da Aplicação](#funcionalidades-e-demonstração-da-aplicação)
* [Modo de uso](#modo-de-uso)
* [Tecnologias utilizadas](#tecnologias-utilizadas)
* [Arquitetura](#arquitetura)
*******

<!-- Descrição -->
<h2> :blue_book: Sobre: </h2>

<div id="descrição-do-projeto"/>
<h3> Descrição </h3>

Minhas Contas Backend é uma API REST construída em Java com Spring Boot para gerenciamento de finanças pessoais. A aplicação fornece endpoints de autenticação, criação e gerenciamento de despesas, organização por categorias para apoiar o controle financeiro do usuário.

<div id="status-do-projeto"/>

> Status do Projeto: :construction: Projeto em construção :construction:
  - Criação *v0.0.1*: 15/04/2026

<div id="pessoas-desenvolvedoras"/>
<h3> Desenvolvedores </h3>

| [<img src="https://github.com/RafaFBorges.png" alt="foto do desenvolvedor" width="128px" height="128px"/><br><sub>Equipe Minhas Contas</sub>](https://github.com/RafaFBorges)
| :---: |

<!-- Funcionalidades e Demonstração da Aplicação -->
<div id="funcionalidades-e-demonstração-da-aplicação"/>
<h2> :hammer: Funcionalidades e Demonstração da Aplicação: </h2>

Minhas Contas Web Backend *v0.0.1*.
  - Login com autenticação via endpoints protegidos.
  - CRUD completo de despesas com categorias.
  - CRUD de categorias para organização de despesas.
  - Autenticação e autorização segura.
  - Estrutura modular com Controllers, Services e DTOs.
  - Suporte a Docker para containerização.
  - Testes automatizados com JUnit.

### Releases:

- *v0.0.1*
  * :construction: Em construção :construction:

<!-- Primeiro acesso -->
<div id="modo-de-uso" />

### Modo de uso:

- Passos para instalar as tecnologias necessárias:

  1. Instale Java JDK 17 neste [link](https://www.oracle.com/java/technologies/downloads/#java17)
  2. Instale Maven neste [link](https://maven.apache.org/download.cgi)
  3. Instale Docker neste [link](https://www.docker.com/products/docker-desktop)
  4. Instale PostgreSQL neste [link](https://www.postgresql.org/download/)

- Variáveis de ambiente (arquivo `.env` na raiz, ignorado pelo git, ou as variáveis do serviço na hospedagem):

| Variável | Obrigatória | Para quê |
| --- | --- | --- |
| `DATASOURCE_URL`, `DATASOURCE_USERNAME`, `DATASOURCE_PASSWORD` | sim | Conexão com o PostgreSQL |
| `JWT_SECRET` | sim | Chave que assina os access tokens (JWT). Mínimo 32 bytes e não pode ser repetitiva (menos de 10 caracteres distintos); sem isso a API não sobe. Gere com `openssl rand -base64 48`. Trocá-la invalida só os access tokens em circulação (até 15 min); os refresh tokens ficam no banco e continuam emitindo access tokens novos. Trocá-la também faz falhar (401) um refresh repetido que estava dentro da tolerância de 10 s, porque o sucessor é derivado do segredo; o cliente precisa fazer login de novo. Para encerrar todas as sessões depois de um vazamento, apague as linhas de `refresh_tokens` (`delete from refresh_tokens;`) além de trocar o segredo |
| `ADMIN_USER`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` | não | Primeiro Admin, criado na subida só se ainda não existir nenhum. Defina as três ou nenhuma. A senha segue a política do cadastro (8+ caracteres, maiúscula, minúscula, número e especial). Só valem no primeiro boot: depois que existe um Admin, **remova as três do ambiente**. Se ficarem e a linha do Admin for apagada, o próximo restart recria a conta com a senha que está na variável. Se o Admin for rebaixado e as variáveis ficarem, a API sobe normalmente (só avisa no log) e nada é criado. Mudar a variável não altera a conta existente. Prefira um `ADMIN_USER` que não seja óbvio: o login é limitado por usuário (5 erros a cada 15 min), então um nome previsível pode ser bloqueado por terceiros |
| `SHOW_SQL` | não | `true` mostra o SQL no log (pode conter dados de usuários) |

- Autenticação: `POST /login` devolve um access token (JWT, 15 min) e um refresh token (7 dias, de uso único). As rotas protegidas exigem `Authorization: Bearer <accessToken>`. `POST /refresh` e `POST /logout` recebem `{"refreshToken": "..."}`; o refresh troca o par (guarde sempre o novo) e o logout encerra a sessão. Repetir um refresh token já trocado é aceito por `jwt.refresh-reuse-grace-seconds` (10 s, para respostas perdidas ou pedidos simultâneos) e devolve o mesmo sucessor da primeira troca, sem criar outra sessão (limite assumido: nessa janela quem copiou o token também recebe o sucessor; só vincular o replay a IP/user agent, no proxy do Web, fecharia isso); depois disso é tratado como cópia do token e encerra todas as sessões do usuário. Cada login forma uma cadeia que vale no máximo `jwt.refresh-chain-max-days` (30) no total, mesmo sendo usada todo dia; o logout encerra a cadeia inteira. Cada usuário tem no máximo `jwt.max-sessions-per-user` (10) sessões: um novo login além disso encerra as menos usadas há mais tempo (a sessão renovada todo dia fica, a parada sai). Tokens vencidos (e os já trocados há mais de 1 dia) são apagados por uma tarefa a cada hora. **Mudança de contrato:** o login deixou de devolver `token` (agora `accessToken` e `refreshToken`) e o header `token` não é mais aceito; publique a API junto com o Web que a consome. Papéis: `SYSTEM > ADMIN > SUPPORT > USER`.

- Construindo dependências do projeto (no root):

```bash
mvn clean install
```

- Opções para rodar a aplicação:

  1. Dev: inicia a aplicação em modo de desenvolvimento (IDE ou Maven wrapper).

    ```bash
    ./mvnw spring-boot:run
    ```

  2. Build: gera o build de produção.

    ```bash
    mvn clean package
    ```

  3. Testes: executa os testes unitários.

    ```bash
    mvn test
    ```

  4. Docker: constrói e executa a aplicação em contêiner.

    ```bash
    docker build -t minhascontas-backend .
    docker run -p 8080:8080 minhascontas-backend
    ```

  5. Docker Compose: inicia a aplicação com banco de dados.

    ```bash
    docker-compose up --build
    ```

- A aplicação será acessível em:

```bash
http://localhost:8080
```

### Migrações do banco:

O schema é versionado com Flyway em `src/main/resources/db/migration` (`V1__baseline.sql`, `V2__....sql`, ...).

  - Para alterar uma entidade (nova coluna, tabela...), crie um novo arquivo `V<n>__descricao.sql`. Nunca edite um já aplicado.
  - O Hibernate só valida (`ddl-auto=validate`): se a entidade divergir do banco, a aplicação não sobe.
  - Banco vazio (sandbox, testes): recebe todas as migrações. Banco que já tinha tabelas: é marcado como V1 sem executá-lo.
  - Recriar o sandbox do zero: `docker compose down -v && docker compose up --build`.

<!-- Tecnologias -->
<div id="tecnologias-utilizadas"/>
<h2> Stack de tecnologias: </h2>

- Java 17
- Spring Boot
- Spring Data JPA
- Spring Security
- Maven
- PostgreSQL
- Docker
- JUnit
- Lombok

<!-- Arquitetura -->
<div id="arquitetura"/>
<h2> Arquitetura: </h2>

<div id="arquitetura-servicos"/>
<h3> Estrutura de Serviços </h3>

  - Back-end REST API: API REST para login, autenticação, gerenciamento de despesas, categorias e tags.
  - Banco de dados PostgreSQL: persistência de dados de usuários, despesas e categorias.

<div id="arquitetura-pastas"/>
<h3> Arquitetura de Pastas </h3>

  - root
    - `src/main/java/com/minhascontasdb`
      - `controller` : endpoints REST da aplicação.
      - `service` : lógica de negócios.
      - `persistence` : camada de acesso aos dados.
      - `dto` : objetos de transferência de dados (requisição/resposta).
      - `filter` : filtros de requisição HTTP.
      - `inicialization` : garantia de dados iniciais.
    - `src/main/resources`
      - `application.properties` : configurações da aplicação.
      - `static` : recursos estáticos.
      - `templates` : templates HTML.
    - `src/test` : testes unitários e de integração.

<div id="arquitetura-variaveis"/>
<h3> Variáveis de ambiente </h3>

  Configure as seguintes variáveis em `application.properties`:

  - `spring.datasource.url` : URL de conexão com o banco de dados PostgreSQL.
  - `spring.datasource.username` : usuário do banco de dados.
  - `spring.datasource.password` : senha do banco de dados.
  - `server.port` : porta da aplicação (padrão: 8080).

  - Dashboard do projeto: https://dashboard.render.com/project/prj-d351ucp5pdvs73bc9s20
  - Link do Projeto: https://minhascontas-server.onrender.com/