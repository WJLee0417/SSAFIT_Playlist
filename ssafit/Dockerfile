FROM eclipse-temurin:25-jdk-noble AS build

WORKDIR /workspace/ssafit

COPY ssafit/.mvn .mvn
COPY ssafit/mvnw ssafit/mvnw.cmd ssafit/pom.xml ./

RUN chmod +x ./mvnw \
    && ./mvnw -B dependency:go-offline

COPY ssafit/src ./src

RUN ./mvnw -B package -DskipTests

FROM eclipse-temurin:25-jre-noble AS runtime

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --home-dir /app app

COPY --from=build --chown=app:app /workspace/ssafit/target/ssafit-0.0.1-SNAPSHOT.war /app/app.war

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.war"]
