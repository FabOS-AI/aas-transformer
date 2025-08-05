FROM prom/prometheus:v2.55.1

# Config envs
ENV SCRAPE_INTERVAL=200ms
ENV EVALUATION_INTERVAL=200ms
ENV ALERTMANAGER_HOST=alertmanager
ENV ALERTMANAGER_PORT=9093
ENV REDIS_EXPORTER_HOST=redis-exporter
ENV REDIS_EXPORTER_PORT=9121
ENV MOSQUITTO_EXPORTER_HOST=mosquitto-exporter
ENV MOSQUITTO_EXPORTER_PORT=9234
ENV AAS_TRANSFORMER_MANAGEMENT_HOST=aas-transformer-management
ENV AAS_TRANSFORMER_MANAGEMENT_PORT=4010
ENV AAS_TRANSFORMER_MANAGEMENT_PATH=/actuator/prometheus
ENV CADVISOR_HOST=cadvisor
ENV CADVISOR_PORT=8080
ENV CADVISOR_SCRAPE_INTERVAL=3s
ENV CADVISOR_SCRAPE_TIMEOUT=3s

# Rule envs
ENV HIGH_JOB_COUNT_EXPR='(sum(redis_key_size{key="jobs"}) > scalar(max_queue_job_count_total)) and (count(container_last_seen{image=~".*executor.*"} > (time() - 60)) < scalar(max_replica_executor_total))'
ENV HIGH_JOB_COUNT_FOR=1m
ENV HIGH_JOB_COUNT_KEEP_FIRING_FOR=0s
ENV HIGH_JOB_COUNT_ACTION_NAME=scale-up

ENV NO_RUNNING_TRANSFORMATIONS_EXPR='(sum(redis_keys_count{key="proc_jobs*"}) == 0) and (sum(redis_key_size{key="jobs"} == 0)) and (count(container_last_seen{image=~".*executor.*"} > (time() - 60)) > 1)'
ENV NO_RUNNING_TRANSFORMATIONS_FOR=30m
ENV NO_RUNNING_TRANSFORMATIONS_KEEP_FIRING_FOR=0s
ENV NO_RUNNING_TRANSFORMATIONS_ACTION_NAME=scale-down

ENV EXECUTOR_COUNTER_GREATER_INPROCESS_JOBS_EXPR='(count(container_last_seen{image=~".*executor.*"}) > sum(redis_keys_count{key="proc_jobs*"})) and (sum(redis_keys_count{key="proc_jobs*"}) != 0) and (count(container_last_seen{image=~".*executor.*"} > (time() - 60)) > 1)'
ENV EXECUTOR_COUNTER_GREATER_INPROCESS_JOBS_FOR=1m
ENV EXECUTOR_COUNTER_GREATER_INPROCESS_JOBS_KEEP_FIRING_FOR=0s
ENV EXECUTOR_COUNTER_GREATER_INPROCESS_JOBS_ACTION_NAME=scale-down

ENV HIGH_OPEN_SUBMODEL_COUNT_EXPR='((sum(aas_submodel_change_count_total)-sum(aas_processed_submodel_change_count_total)) > 5)'
ENV HIGH_OPEN_SUBMODEL_COUNT_FOR=10s
ENV HIGH_OPEN_SUBMODEL_COUNT_KEEP_FIRING_FOR=0s
ENV HIGH_OPEN_SUBMODEL_COUNT_ACTION_NAME=scale-up

ENV LOW_MQTT_MESSAGE_QUEUE_EXPR='(avg(delta(aas_submodel_change_count_total[1s])) > 0) and (avg(redis_key_size{db="db0",key="mqtt_message_queue"}) < 5)'
ENV LOW_MQTT_MESSAGE_QUEUE_FOR=10s
ENV LOW_MQTT_MESSAGE_QUEUE_KEEP_FIRING_FOR=0s
ENV LOW_MQTT_MESSAGE_QUEUE_ACTION_NAME=scale-down

COPY prometheus.yml /etc/prometheus/prometheus.yml
COPY transformer.yml /etc/prometheus/rules/transformer.yml
COPY startup.sh /etc/prometheus/startup.sh

USER root
RUN chmod +x /etc/prometheus/startup.sh
RUN chown nobody:nobody /etc/prometheus/startup.sh
RUN chown -R nobody:nobody /etc/prometheus/rules
USER nobody

ENTRYPOINT  ["/etc/prometheus/startup.sh"]
CMD         [ "--config.file=/etc/prometheus/prometheus.yml", \
              "--storage.tsdb.path=/prometheus", \
              "--storage.tsdb.retention.size=2GB", \
              "--web.console.libraries=/usr/share/prometheus/console_libraries", \
              "--web.console.templates=/usr/share/prometheus/consoles" ]
