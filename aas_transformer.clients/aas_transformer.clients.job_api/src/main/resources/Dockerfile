FROM ubuntu

ENV TRANSFORMER_JOB_API_URL="http://host.docker.internal:4040"

RUN apt update && apt install -y wget
RUN mkdir /opt/swagger

WORKDIR /opt/swagger

CMD wget -O swagger.yml ${TRANSFORMER_JOB_API_URL}/v3/api-docs