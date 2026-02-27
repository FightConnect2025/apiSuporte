# --------- STAGE 1: Build ---------
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app

# Copia o projeto
COPY pom.xml .
COPY src ./src

# Build do jar (sem testes)
RUN mvn clean package -DskipTests

# --------- STAGE 2: Runtime ---------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copia o jar gerado (pega o único jar do target)
COPY --from=build /app/target/*.jar app.jar

# Porta da API suporte
EXPOSE 8086

# Sobe a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]
