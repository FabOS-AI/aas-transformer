package de.fhg.ipa.aas_transformer.test.system.performance;

import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.clients.management.JobsClient;
import de.fhg.ipa.aas_transformer.clients.management.ManagementClient;
import de.fhg.ipa.aas_transformer.clients.management.MetricsClient;
import de.fhg.ipa.aas_transformer.model.TransformationLog;
import de.fhg.ipa.aas_transformer.test.system.performance.model.AggregatedTestResult;
import de.fhg.ipa.aas_transformer.test.system.performance.model.TestResult;
import de.fhg.ipa.aas_transformer.test.system.performance.model.TransformationDurations;
import de.fhg.ipa.aas_transformer.test.utils.extentions.AasITExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.AasTransformerExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.PrometheusExtension;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractPerformanceTest {
    @RegisterExtension
    static PrometheusExtension prometheusExtension = new PrometheusExtension();

    @RegisterExtension
    static AasTransformerExtension aasTransformerExtension = new AasTransformerExtension(
            1,
            1
    );
    protected static int TEST_RUN_NO = 1;

    //region Test Vars
    // Service Ports:
    String transformerManagementPort = System.getProperty("aas_transformer.services.management.port");
    String aasRegistryPort = System.getProperty("aas.aas-registry.port");
    String aasRepositoryPort = System.getProperty("aas.aas-repository.port");
    String aasRepositoryPath = System.getProperty("aas.aas-repository.path");
    String smRegistryPort = System.getProperty("aas.submodel-registry.port");
    String smRepositoryPort = System.getProperty("aas.submodel-repository.port");
    String smRepositoryPath = System.getProperty("aas.submodel-repository.path");

    // Service Clients:
    static ManagementClient managementClient;
    static MetricsClient metricsClient;
    static JobsClient jobsClient;
    static AasRegistry aasRegistry;
    static AasRepository aasRepository;
    static SubmodelRegistry smRegistry;
    static SubmodelRepository smRepository;

    // Result vars:
    static List<TestResult> testResults = new ArrayList<>();
    static AggregatedTestResult aggregatedTestResult;
    // endregion


    public AbstractPerformanceTest() {
        managementClient = new ManagementClient("http://localhost:" + transformerManagementPort);
        metricsClient = new MetricsClient("http://localhost:" + transformerManagementPort);
        jobsClient = new JobsClient("http://localhost:" + transformerManagementPort);

        aasRegistry = new AasRegistry("http://localhost:" + aasRegistryPort, "http://localhost:" + aasRepositoryPort + aasRepositoryPath);
        aasRepository = new AasRepository("http://localhost:" + aasRepositoryPort + aasRepositoryPath);
        smRegistry = new SubmodelRegistry("http://localhost:" + smRegistryPort, "http://localhost:" + smRepositoryPort + smRepositoryPath);
        smRepository = new SubmodelRepository("http://localhost:" + smRepositoryPort + smRepositoryPath);
    }

    @AfterEach
    void tearDown() throws DeserializationException {
        // Clear all transformers:
        managementClient.getAllTransformer().collectList().block().forEach(
                transformer -> managementClient.deleteTransformer(transformer.getId(),false).block()
        );
        Instant start = Instant.now();
        // Clear all Aas Objects:
        clearAasObjects();
        Instant end = Instant.now();

        prometheusExtension.createAnnotation(
                start,
                end,
                List.of("test", "clear_aas_objects"),
                "Clear Aas Objects"
        );

        TEST_RUN_NO++;
    }

    protected void printResultsOfTestrun(int executorCount, int initialSubmodelCount, int finalSubmodelCount, Duration duration) {
        long durationSeconds = duration.toSeconds();
        long durationMilliSeconds = duration.minusSeconds(durationSeconds).toMillis();

        System.out.println("Executor count: " + executorCount);
        System.out.println("Initial submodel count: " + initialSubmodelCount);
        System.out.println("Final submodel count: " + finalSubmodelCount);
        System.out.println("Time to transform " + (finalSubmodelCount-initialSubmodelCount) + " submodels: " + durationSeconds + "s " + durationMilliSeconds + "ms");
    }

    protected Map<UUID, Long> getExcutionCountMap() {
        List<TransformationLog> logs = metricsClient.getTransformationLogs().collectList().block();

        Set<UUID> executorIds = logs.stream()
                .map(TransformationLog::getExecutorId)
                .collect(Collectors.toSet());

        Map<UUID, Long> executionCountMap = new HashMap<>();

        executorIds.forEach(executorId -> {
            long executionCount = logs.stream()
                    .filter(log -> log.getExecutorId().equals(executorId))
                    .count();
            executionCountMap.put(executorId, executionCount);
        });

        System.out.println("Executor / Execution count:" + executionCountMap);

        return executionCountMap;
    }

    protected TransformationDurations getTransformationDurationStats() {
        List<TransformationLog> logs = metricsClient.getTransformationLogs().collectList().block();

        Long transformationDurationAvg = logs.stream()
                .map(TransformationLog::getTransformationDurationInMs)
                .collect(Collectors.averagingLong(Long::longValue))
                .longValue();

        Long transformationDurationMin = logs.stream()
                .map(TransformationLog::getTransformationDurationInMs)
                .min(Comparator.naturalOrder())
                .orElse(0L);

        Long transformationDurationMax = logs.stream()
                .map(TransformationLog::getTransformationDurationInMs)
                .max(Comparator.naturalOrder())
                .orElse(0L);

        Long lookupSourceAvg = logs.stream()
                .map(TransformationLog::getSourceLookupInMs)
                .collect(Collectors.averagingLong(Long::longValue))
                .longValue();

        Long lookupSourceMin = logs.stream()
                .map(TransformationLog::getSourceLookupInMs)
                .min(Comparator.naturalOrder())
                .orElse(0L);

        Long lookupSourceMax = logs.stream()
                .map(TransformationLog::getSourceLookupInMs)
                .max(Comparator.naturalOrder())
                .orElse(0L);

        Long saveDestinationAvg = logs.stream()
                .map(TransformationLog::getDestinationSaveInMs)
                .collect(Collectors.averagingLong(Long::longValue))
                .longValue();

        Long saveDestinationMin = logs.stream()
                .map(TransformationLog::getDestinationSaveInMs)
                .min(Comparator.naturalOrder())
                .orElse(0L);

        Long saveDestinationMax = logs.stream()
                .map(TransformationLog::getDestinationSaveInMs)
                .max(Comparator.naturalOrder())
                .orElse(0L);

        return new TransformationDurations(
                transformationDurationAvg,
                transformationDurationMin,
                transformationDurationMax,
                lookupSourceAvg,
                lookupSourceMin,
                lookupSourceMax,
                saveDestinationAvg,
                saveDestinationMin,
                saveDestinationMax
        );
    }

    protected void clearAasObjects() throws DeserializationException {
        aasRepository.deleteAllAas();
        smRepository.deleteAllSubmodels();
    }
}
