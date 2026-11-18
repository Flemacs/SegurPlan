# ---------- Etapa 1: compilar ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# -Pprod activa el perfil de producción (Swagger apagado por defecto)
RUN mvn -B -q package -DskipTests -Pprod

# ---------- Etapa 2: ejecutar ----------
FROM eclipse-temurin:21-jre
# postgresql-client aporta pg_dump y psql para los respaldos desde la aplicación
# curl se usa en el HEALTHCHECK
RUN apt-get update && apt-get install -y --no-install-recommends postgresql-client curl \
    && rm -rf /var/lib/apt/lists/*
# Usuario de servicio sin privilegios
RUN useradd -r -s /usr/sbin/nologin appuser
WORKDIR /app
COPY --from=build --chown=appuser:appuser /app/target/*.jar app.jar
RUN mkdir -p /app/uploads /app/backups /app/logs && chown -R appuser:appuser /app
ENV PG_BIN_DIR="" \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -Xlog:gc*:file=/app/logs/gc.log:time,uptime:filecount=5,filesize=10m"
USER appuser
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fs http://localhost:${PORT:-8080}/api/csrf || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
