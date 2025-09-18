<!-- Título -->
<h1 align="center"> MinhasContas-DB </h1>

<!-- Badges -->

*******
### Índice 

* [Descrição do Projeto](#descrição-do-projeto)
* [Status do Projeto](#status-do-Projeto)
* [Desenvolvedores do Projeto](#pessoas-desenvolvedoras)
* [Funcionalidades e Demonstração da Aplicação](#funcionalidades-e-demonstração-da-aplicação)
* [Modo de uso](#modo-de-uso)
* [Tecnologias utilizadas](#tecnologias-utilizadas)
*******

<!-- Descrição -->
<h2> :blue_book: Sobre: </h2>

<div id="descrição-do-projeto"/>
<h3> Descrição </h3>

Aplicativo mobile reconhecer as figurinhas faltantes do álbum de figurinhas da copa de 2022.

<div id="status-do-Projeto"/>

> Status do Projeto: :construction: Projeto em construção :construction:

<div id="pessoas-desenvolvedoras"/>
<h3> Desenvolvedores </h3>

| [<img src="https://github.com/RafaFBorges.png" alt="foto Rafael Fernandes Borges"  width="128px" height="128px"/><br><sub>Rafael Fernandes Borges</sub>](https://github.com/RafaFBorges)
| :---: |

<!-- Funcionalidades e Demonstração da Aplicação -->
<div id="funcionalidades-e-demonstração-da-aplicação"/>
<h2> :hammer: Funcionalidades e Demonstração da Aplicação: </h2>

A feature base deste programa é a manipulção de dados para um controle de gastos, ainda em construção na versão *v0.0.1*.

### Releases:

- *v0.0.1*
  * :construction: Em construção :construction:
  * CRUD de despesas
  * Dockerização do projeto da deploy

<!-- Primeiro acesso -->
<div id="modo-de-uso" />

### Modo de uso:

- Passos para começar no projeto:
  *Obs.:* busque os passos atuais para instalação no chatGPT

  1. Baixe e instale o Java JDK 17
  2. Baixe e instale o Maven
  3. Baixe e instale o Docker Desktop
  4. Baixe e instale o Postgress

- Executar o projeto:
  
  - Local
    1. Clique com o botão direito do mouse em uma classe
    2. Clique em *Run Java*
  
  - Docker
    1. Buildar a imagem a partir do root do projeto
```
docker build -t minhascontas .
```

    2. Rodar o docker mapeando uma porta do computador para a porta 8080
```
docker run -p 8080:8080 minhascontas
```

    3. Acessar os endpoints na porta 8080
  
- Execute os teste:
  
  - Local
    1. Execute no terminal o código
```
mvn test
```

  - Docker
    1. Execute no terminal o código
```
docker-compose up --build test
```

- Deploy

  Deploy da aplicação é feito na plataforma Render. O deploy é separado em um server e um banco de dados SQL. Links para acesso são são:

  - Dashboard do projeto: https://dashboard.render.com/project/prj-d351ucp5pdvs73bc9s20
  - Link do Projeto: https://minhascontas-server.onrender.com/ 

<!-- Tecnologias -->
<div id="tecnologias-utilizadas"/>
<h2> Tecnologias: </h2>

- Java
- Maven
- Postgress
- Docker