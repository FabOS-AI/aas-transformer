FROM ubuntu

ENV TRANSFORMER_HOST=aas-transformer \
    TRANSFORMER_PORT=4010 \
    TRANSFORMER_PATH=""

COPY ./src/main/resources /aas-transformer-initializer

RUN apt update && apt install -y curl jq
RUN chmod +x /aas-transformer-initializer/startup.sh

WORKDIR /aas-transformer-initializer
CMD /aas-transformer-initializer/startup.sh
