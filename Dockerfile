# Compilacao isolada: Maven e fontes nao ficam na imagem final.
FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
COPY src/ src/
# O CI executa verify antes do build; Testcontainers precisa de Docker externo.
RUN ./mvnw --batch-mode --no-transfer-progress -DskipTests package \
    && cp target/*.jar /build/app.jar

# Runtime Java 25 com ferramentas de backup; o banco fica fora do container.
FROM eclipse-temurin:25-jre-noble
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates curl gnupg fontconfig fonts-dejavu-core \
    && install -d /usr/share/postgresql-common/pgdg \
    && curl --fail --silent --show-error https://www.postgresql.org/media/keys/ACCC4CF8.asc -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc \
    && echo "deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.asc] https://apt.postgresql.org/pub/repos/apt noble-pgdg main" > /etc/apt/sources.list.d/pgdg.list \
    && apt-get update && apt-get install -y --no-install-recommends postgresql-client-18 \
    && apt-get purge -y curl gnupg && apt-get autoremove -y && rm -rf /var/lib/apt/lists/* \
    && /usr/lib/postgresql/18/bin/pg_dump --version \
    && /usr/lib/postgresql/18/bin/pg_restore --version
RUN groupadd --system apecan && useradd --system --gid apecan --home-dir /app apecan \
    && install -d -o apecan -g apecan -m 700 /app /tmp/apecan-backup
WORKDIR /app
COPY --from=build --chown=apecan:apecan /build/app.jar app.jar
ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx256m -XX:+ExitOnOutOfMemoryError -Djava.awt.headless=true"
# Perfis (prod,render), PORT e segredos sao fornecidos pelo ambiente do Render.
USER apecan
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
