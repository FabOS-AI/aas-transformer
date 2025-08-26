package de.fhg.ipa.aas_transformer.test.system.performance;

import de.fhg.ipa.aas_transformer.model.ServiceType;
import de.fhg.ipa.aas_transformer.model.TransformationLog;
import de.fhg.ipa.aas_transformer.test.system.AbstractExtSystemTest;
import de.fhg.ipa.aas_transformer.test.system.performance.model.AggregatedTestResult;
import de.fhg.ipa.aas_transformer.test.system.performance.model.TestResult;
import de.fhg.ipa.aas_transformer.test.system.performance.model.TransformationDurations;
import de.fhg.ipa.aas_transformer.test.utils.creator.HistoricDataCreator;
import de.fhg.ipa.aas_transformer.test.utils.creator.MachineDataCreator;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.basyx.submodelregistry.client.model.SubmodelDescriptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

public abstract class ExtAbstractPerformanceTest extends AbstractExtSystemTest {
    protected static int TEST_RUN_NO = 1;

    // Result vars:
    static List<TestResult> testResults = new ArrayList<>();
    static AggregatedTestResult aggregatedTestResult;

    @BeforeAll
    @AfterAll
    static void beforeAll() {
        // Clear Transformer
        System.out.println("Clear all transformers");
        managementClient.getAllTransformer().collectList().block().forEach(
                transformer -> managementClient.deleteTransformer(transformer.getId(),false).block()
        );
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

        grafanaClient.createAnnotation(
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
        System.out.println("Clear all Submodels");
        List<SubmodelDescriptor> submodelDescriptors = smRegistry.getSubmodelDescriptors();
        deleteSubmodelsById(submodelDescriptors.stream().map(smd -> smd.getId()).toList());
//        batchDeleteAllSubmodels();
        System.out.println("Clear all Shells");
        aasRepository.deleteAllAas();
        System.out.println("Clear all ShellDescriptors");
        aasRegistry.deleteAllShellDescriptors();
    }

    private void deleteSubmodelsById(List<String> ids) {
        ids.forEach(id -> {
            try {
                smRepository.deleteSubmodel(id);
            } catch (Exception e) {
                System.out.println("Error deleting submodel: " + id);
            }
        });
    }

    protected HistoricDataCreator createHistoricDataCreator(int submodelCount, int sleepInMs) {
        return new HistoricDataCreator(aasRegistry, aasRepository, smRegistry, smRepository, submodelCount, sleepInMs);
    }

    protected MachineDataCreator createMachineDataCreator(int submodelCount, int sleepInMs) {
        return new MachineDataCreator(aasRegistry, aasRepository, smRegistry, smRepository, submodelCount, sleepInMs);
    }

    protected int waitForListenerCount(Predicate<Integer> condition) {
        int runningCount = Math.toIntExact(scalingClient.getRunningTasksOfServiceType(ServiceType.LISTENER).block());
        while(condition.test(runningCount)) {
            System.out.println("Running listener count: " + runningCount);
            try { sleep(1000);}
            catch (InterruptedException e) { throw new RuntimeException(e);}
            runningCount = Math.toIntExact(scalingClient.getRunningTasksOfServiceType(ServiceType.LISTENER).block());
        }
        System.out.println("Running listener count: " + runningCount);
        return runningCount;
    }

    protected int waitForExecutorCount(Predicate<Integer> condition) {
        int runningCount = scalingClient.getRunningTasksOfServiceType(ServiceType.EXECUTOR).block();
        while(condition.test(runningCount)) {
            System.out.println("Running executor count: " + runningCount);
            try { sleep(1000);}
            catch (InterruptedException e) { throw new RuntimeException(e);}
            runningCount = scalingClient.getRunningTasksOfServiceType(ServiceType.EXECUTOR).block();
        }
        System.out.println("Running executor count: " + runningCount);
        return runningCount;
    }

    protected long waitForWaitingJobCount(Predicate<Integer> condition) throws InterruptedException {
        long waitingJobCount = redisControllerClient.getWaitingJobCount().block();
        while(condition.test((int)waitingJobCount)) {
            System.out.println("Remaining open Jobs: " + waitingJobCount);
            sleep(1000);
            waitingJobCount = redisControllerClient.getWaitingJobCount().block();
        }
        return waitingJobCount;
    }
}
