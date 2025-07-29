FROM eclipse-temurin:18

# TODO: Insert Redis properties
#ENV

RUN apt update && \
    apt install -y curl jq

COPY target/*-exec.jar /app/app.jar

WORKDIR /app

CMD ["java", "--add-opens=java.base/java.lang=ALL-UNNAMED", "-jar", "-Djava.security.egd=file:/dev/./urandom", "/app/app.jar"]
