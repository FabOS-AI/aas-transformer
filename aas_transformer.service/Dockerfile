FROM openjdk:18.0.2.1-jdk-slim-buster

ENV DATABASE_HOST="aas-transformer-database" \
    DATABASE_PORT=3306 \
    AAS_BROKER_HOST="aas-broker" \
    AAS_BROKER_PORT=1884 \
    AAS_AAS_REGISTRY_HOST="aas-registry" \
    AAS_AAS_REGISTRY_PORT=8080 \
    AAS_AAS_REPOSITORY_HOST="aas-env" \
    AAS_AAS_REPOSITORY_Port=8081 \
    AAS_SUBMODEL_REGISTRY_HOST="sm-registry" \
    AAS_SUBMODEL_REGISTRY_PORT=8080 \
    AAS_SUBMODEL_REPOSITORY_HOST="aas-env" \
    AAS_SUBMODEL_REPOSITORY_Port=8081

RUN apt update && \
    apt install -y curl jq

COPY target/*-exec.jar /app/app.jar

WORKDIR /app

CMD ["java", "--add-opens=java.base/java.lang=ALL-UNNAMED", "-jar", "-Djava.security.egd=file:/dev/./urandom", "/app/app.jar"]
