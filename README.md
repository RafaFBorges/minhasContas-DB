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