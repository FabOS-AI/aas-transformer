FROM ubuntu

ENV TRANSFORMER_MGMT_URL="http://host.docker.internal:4010"

RUN apt update && apt install -y wget jq
RUN mkdir /opt/swagger

WORKDIR /opt/swagger

CMD ./startup.sh