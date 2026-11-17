# ---------- Etapa 1: compilar ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Descarga las dependencias (incluye JasperReports y springdoc) y las deja en caché
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- Etapa 2: ejecutar ----------
FROM eclipse-temurin:21-jre
# postgresql-client aporta pg_dump y psql para los respaldos desde la aplicación
RUN apt-get update && apt-get install -y --no-install-recommends postgresql-client \
    && rm -rf /var/lib/apt/lists/*

# La aplicación no corre como root
RUN groupadd --system segurplan && useradd --system --gid segurplan --home /app segurplan
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/uploads /app/backups /app/logs \
    && chown -R segurplan:segurplan /app
USER segurplan

ENV PG_BIN_DIR="" \
    JAVA_OPTS="-Xmx512m" \
    SWAGGER_ENABLED="true"
EXPOSE 8080

# Sin curl en la imagen: se comprueba que el puerto acepte conexiones
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
    CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080' || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
