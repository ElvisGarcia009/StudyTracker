# ---------- Etapa 1: compilar con Maven (no hace falta tener Maven instalado) ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Primero solo el pom para aprovechar la caché de dependencias de Docker
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Los tests unitarios corren aquí; si fallan, la imagen no se construye
RUN mvn -B package

# ---------- Etapa 2: imagen liviana solo con el JRE ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /build/target/quarkus-app/lib/ ./lib/
COPY --from=build /build/target/quarkus-app/*.jar ./
COPY --from=build /build/target/quarkus-app/app/ ./app/
COPY --from=build /build/target/quarkus-app/quarkus/ ./quarkus/

EXPOSE 8080
ENV JAVA_OPTS="-Djava.util.logging.manager=org.jboss.logmanager.LogManager -XX:MaxRAMPercentage=75"

CMD ["sh", "-c", "java $JAVA_OPTS -jar quarkus-run.jar"]
