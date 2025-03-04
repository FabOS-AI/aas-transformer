package de.fhg.ipa.aas_transformer.service.management;

import de.fhg.ipa.aas_transformer.model.TransformationLog;
import de.fhg.ipa.aas_transformer.persistence.api.TransformationLogJpaRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/metrics")
public class MetricsRestController {
    @Autowired
    TransformerServiceHandler transformerServiceHandler;

    private final MqttListener mqttListener;
    private final MeterRegistry meterRegistry;
    private final TransformationLogJpaRepository transformationLogJpaRepository;

    private final Counter counter;
    private final Counter maxReplicaExecutorCounter;
    private final Counter maxReplicaListenerCounter;
    private final Counter maxQueueJobCountCounter;
    private final Gauge isExecutorScalingGauge;
    private final Gauge isListenerScalingGauge;

    public MetricsRestController(
            MqttListener mqttListener,
            MeterRegistry meterRegistry,
            TransformationLogJpaRepository transformationLogJpaRepository,
            @Value("${scaling.max-replicas.executor: #{1}}") int maxReplicaExecutorCount,
            @Value("${scaling.max-replicas.listener: #{2}}") int maxReplicaListenerCount,
            @Value("${scaling.max-queue-job-count: #{100}}") int maxQueueJobCount
    ) {
        this.mqttListener = mqttListener;
        this.meterRegistry = meterRegistry;
        this.transformationLogJpaRepository = transformationLogJpaRepository;

        this.counter = Counter.builder("aas_processed_submodel_change_count")
                .description("a number of processed incoming submodel changes during runtime of this service")
                .tag("name", "aas_processed_submodel_change_count")
                .register(meterRegistry);
        this.maxReplicaExecutorCounter = Counter.builder("max_replica_executor")
                .description("a number of maximum executor replicas")
                .tag("name", "max_replica_executor")
                .register(meterRegistry);
        this.maxReplicaExecutorCounter.increment(maxReplicaExecutorCount);
        this.maxReplicaListenerCounter = Counter.builder("max_listener_executor")
                .description("a number of maximum listener replicas")
                .tag("name", "max_replica_listener")
                .register(meterRegistry);
        this.maxReplicaListenerCounter.increment(maxReplicaListenerCount);
        this.maxQueueJobCountCounter = Counter.builder("max_queue_job_count")
                .description("a number of maximum jobs in the queue")
                .tag("name", "max_queue_job_count")
                .register(meterRegistry);
        this.maxQueueJobCountCounter.increment(maxQueueJobCount);

        this.isExecutorScalingGauge = Gauge.builder("is_executor_scaling", () -> {
                    return transformerServiceHandler.isExecutorScaling() ? 1 : 0;
                })
                .description("a flag indicating if the executor service is currently scaling")
                .tag("name", "is_executor_scaling")
                .register(meterRegistry);

        this.isListenerScalingGauge = Gauge.builder("is_listener_scaling", () -> {
                    return transformerServiceHandler.isListenerScaling() ? 1 : 0;
                })
                .description("a flag indicating if the listener service is currently scaling")
                .tag("name", "is_listener_scaling")
                .register(meterRegistry);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/logs")
    @Operation(summary = "Get transformation logs")
    public Flux<TransformationLog> getTransformationLogs() {
        return transformationLogJpaRepository.findAll();
    }

    @RequestMapping(method = RequestMethod.POST, value = "/logs")
    @Operation(summary = "Add a transformation log")
    public Mono<TransformationLog> addTransformationLog(@RequestBody TransformationLog log) {
        return transformationLogJpaRepository.save(log);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/logs")
    @Operation(summary = "Clear transformation log")
    public void clearTransformationLog() {
        transformationLogJpaRepository.deleteAll().block();
    }

    @RequestMapping(method = RequestMethod.GET, value = "/submodel-change-count")
    @Operation(summary = "Get the number of submodel changes")
    public double getSubmodelChangeCount() {
        return mqttListener.getCounter().count();
    }

    @RequestMapping(method = RequestMethod.GET, value = "/processed-submodel-change-count")
    @Operation(summary = "Get the number of processed submodel changes")
    public double getProcessedSubmodelChangeCount() {
        return counter.count();
    }

    @RequestMapping(method = RequestMethod.POST, value = "/processed-submodel-change-count/add-one")
    @Operation(summary = "Increase processed submodel change count by one")
    public double addOneToProcessedSubmodelChangeCount() {
        counter.increment();
        return counter.count();
    }
}
