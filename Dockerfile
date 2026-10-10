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

# Runtime Java 25; o banco fica fora do container.
FROM eclipse-temurin:25-jre-noble
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates fontconfig fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*
RUN groupadd --system apecan && useradd --system --gid apecan --home-dir /app apecan \
    && install -d -o apecan -g apecan -m 700 /app
WORKDIR /app
COPY --from=build --chown=apecan:apecan /build/app.jar app.jar
ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx256m -XX:+ExitOnOutOfMemoryError -Djava.awt.headless=true"
# Perfis (prod,render), PORT e segredos sao fornecidos pelo ambiente do Render.
USER apecan
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
