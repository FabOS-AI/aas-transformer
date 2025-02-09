package de.fhg.ipa.aas_transformer.service.management;

import de.fhg.ipa.aas_transformer.model.TransformationLog;
import de.fhg.ipa.aas_transformer.persistence.api.TransformationLogJpaRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/metrics")
public class MetricsRestController {
    private final MqttListener mqttListener;
    private final MeterRegistry meterRegistry;
    private final TransformationLogJpaRepository transformationLogJpaRepository;

    private final Counter counter;

    public MetricsRestController(
            MqttListener mqttListener,
            MeterRegistry meterRegistry,
            TransformationLogJpaRepository transformationLogJpaRepository
    ) {
        this.mqttListener = mqttListener;
        this.meterRegistry = meterRegistry;
        this.transformationLogJpaRepository = transformationLogJpaRepository;

        this.counter = Counter.builder("aas_processed_submodel_change_count")
                .description("a number of processed incoming submodel changes during runtime of this service")
                .tag("name", "aas_processed_submodel_change_count")
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
