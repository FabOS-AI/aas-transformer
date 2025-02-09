FROM ubuntu

ENV TRANSFORMER_MGMT_URL="http://host.docker.internal:4010"

RUN apt update && apt install -y wget
RUN mkdir /opt/swagger

WORKDIR /opt/swagger

CMD wget -O swagger.yml ${TRANSFORMER_MGMT_URL}/v3/api-docs