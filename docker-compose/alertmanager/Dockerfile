FROM quay.io/prometheus/alertmanager:v0.28.0

ENV GROUP_WAIT=30s
ENV GROUP_INTERVAL=5m
ENV REPEAT_INTERVAL=1h
ENV AAS_TRANSFORMER_MANAGEMENT_HOST=aas-transformer-management
ENV AAS_TRANSFORMER_MANAGEMENT_PORT=4010
ENV AAS_TRANSFORMER_MANAGEMENT_PATH=/scaling/alert

COPY config.yml /alertmanager/config.yml
COPY startup.sh /alertmanager/startup.sh

ENTRYPOINT ["/alertmanager/startup.sh"]
CMD        [ "--config.file=/alertmanager/config.yml" ]
