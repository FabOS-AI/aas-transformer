FROM openjdk:18.0.2.1-jdk-slim-buster

ENV DATABASE_HOST="aas-transformer-database" \
    DATABASE_PORT=3306 \
    AAS_BROKER_HOST="aas-broker" \
    AAS_BROKER_PORT=1883 \
    AAS_REGISTRY_HOST="aas-registry" \
    AAS_REGISTRY_PORT=4000 \
    AAS_REGISTRY_CONTEXT="registry" \
    AAS_SERVER_HOST="aasServer" \
    AAS_SERVER_PORT=4001 \
    AAS_SERVER_CONTEXT="aasServer"

RUN apt update && \
    apt install -y curl jq

COPY target/*-exec.jar /app/app.jar

WORKDIR /app

RUN export JDK_JAVA_OPTIONS="--add-opens java.base/java.lang=ALL-UNNAMED"

ENTRYPOINT ["java", "-jar", "-Djava.security.egd=file:/dev/./urandom", "/app/app.jar"]
