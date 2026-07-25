# Etapa 1: Build da aplicação
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 1. Copia apenas o arquivo de dependências e baixa os pacotes
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 2. Copia o resto do código e compila
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Imagem leve para execução
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
