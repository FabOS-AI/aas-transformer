FROM openjdk:18.0.2.1-jdk-slim-buster

ENV AAS_TRANSFORMER_SERVICE_MANAGEMENT_SCHEMA="http" \
    AAS_TRANSFORMER_SERVICE_MANAGEMENT_HOST="aas-transformer-management" \
    AAS_TRANSFORMER_SERVICE_MANAGEMENT_PORT=4010 \
    AAS_BROKER_HOST="aas-broker" \
    AAS_BROKER_PORT=1883 \
    AAS_AAS_REGISTRY_HOST="aas-registry" \
    AAS_AAS_REGISTRY_PORT=8080 \
    AAS_AAS_REGISTRY_PATH="" \
    AAS_AAS_REPOSITORY_HOST="aas-env" \
    AAS_AAS_REPOSITORY_PORT=8081 \
    AAS_AAS_REPOSITORY_PATH="" \
    AAS_SUBMODEL_REGISTRY_HOST="sm-registry" \
    AAS_SUBMODEL_REGISTRY_PORT=8080 \
    AAS_SUBMODEL_REGISTRY_PATH="" \
    AAS_SUBMODEL_REPOSITORY_HOST="aas-env" \
    AAS_SUBMODEL_REPOSITORY_PORT=8081 \
    AAS_SUBMODEL_REPOSITORY_PATH=""

RUN apt update && \
    apt install -y curl jq

COPY target/*-exec.jar /app/app.jar

WORKDIR /app

CMD ["java", "--add-opens=java.base/java.lang=ALL-UNNAMED", "-jar", "-Djava.security.egd=file:/dev/./urandom", "/app/app.jar"]
