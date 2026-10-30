# syntax=docker/dockerfile:1

# ---- Build ----
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

# Dependências primeiro: o cache da camada só é invalidado quando o pom.xml muda.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

# ---- Runtime ----
FROM eclipse-temurin:25-jre
WORKDIR /app

# O banco vive fora da imagem: /data é um volume, nunca o diretório da aplicação.
# Roda como usuário sem privilégios (uid/gid 1000).
RUN mkdir -p /data && chown 1000:1000 /data

ENV SPRING_DATASOURCE_URL=jdbc:sqlite:/data/estacao.db
ENV SERVER_PORT=8080

COPY --from=build /workspace/target/*.jar /app/app.jar

USER 1000
EXPOSE 8080
VOLUME ["/data"]

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
