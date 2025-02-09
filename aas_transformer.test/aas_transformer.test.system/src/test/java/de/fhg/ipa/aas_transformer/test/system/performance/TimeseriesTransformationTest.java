package de.fhg.ipa.aas_transformer.test.system.performance;

import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.test.system.performance.model.AggregatedTestResult;
import de.fhg.ipa.aas_transformer.test.system.performance.model.TestResult;
import de.fhg.ipa.aas_transformer.test.utils.extentions.AasITExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.MariaDbExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.PrometheusExtension;
import de.fhg.ipa.aas_transformer.test.utils.extentions.RedisExtension;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.registerAasObjectsFromTriples;
import static de.fhg.ipa.aas_transformer.test.utils.AasTimeseriesObjects.getRandomTimeseriesTriples;
import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MariaDbExtension.class)
@ExtendWith(AasITExtension.class)
@ExtendWith(RedisExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled
public class TimeseriesTransformationTest extends AbstractPerformanceTest implements Runnable {
    private static final int PARALLEL_SUBMODEL_CREATOR_COUNT = 2;
    private static final int SOURCE_SUBMODEL_COUNT = 50;
    private static final List<Integer> EXECUTOR_COUNTS = List.of(1,3,5);
    private static final int TEST_COUNT_PER_EXECUTOR_COUNT = 1;
    private static final String FILE_PREFIX = "TimeseriesTransformationTest_";

    Transformer transformer =  (Transformer) getRandomTimeseriesTriples(1).get(0).get(2);

    @BeforeEach
    void setUp() {
        managementClient.createTransformer(
                transformer,
                true
        ).block();
    }

    @AfterAll
    static void afterAll() {
        aggregatedTestResult = new AggregatedTestResult(testResults);
        TestResult.toFile(testResults, FILE_PREFIX);
        aggregatedTestResult.toFile(FILE_PREFIX);
    }

    private static Stream<Arguments> getExecutorCounts() {
        List<Arguments> executorCounts = new ArrayList<>();

        EXECUTOR_COUNTS.stream().forEach(executorCount -> {
            // Very first test is always an outlier and will be removed in the afterAll method:
            executorCounts.add(Arguments.of(executorCount));

            for (int i = 0; i < TEST_COUNT_PER_EXECUTOR_COUNT; i++) {
                executorCounts.add(Arguments.of(executorCount));
            }
        });

        return executorCounts.stream();
    }

    @Order(10)
    @ParameterizedTest
    // Very first test after every scale up is always an outlier and will be ignored at the end of the test run:
    @MethodSource("getExecutorCounts")
    public void testImpactOfExecutorCountOnTransformationSpeed(int count) throws DeserializationException {
        aasTransformerExtension.resetExecutorContainer(count, prometheusExtension);

        assertTrue(managementClient.getAllTransformer().collectList().block().size() == 1);

        Instant start = Instant.now();
        List<Thread> smCreatorThreads = new ArrayList<>();
        for (int i = 0; i < PARALLEL_SUBMODEL_CREATOR_COUNT; i++) {
            Thread thread = new Thread(this);
            smCreatorThreads.add(thread);
            thread.start();
        }


        smCreatorThreads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        Instant endSmCreate = Instant.now();

        prometheusExtension.createAnnotation(
                start,
                endSmCreate,
                List.of("test", "create_submodels"),
                "Create Submodels"
        );

        int smCountInitial = SOURCE_SUBMODEL_COUNT*PARALLEL_SUBMODEL_CREATOR_COUNT;

        int smCount;
        while(true) {
            smCount = smRepository.getAllSubmodels().size();
            if(smCount >= smCountInitial*2)
                break;
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Instant end = Instant.now();

        prometheusExtension.createAnnotation(
                start,
                end,
                List.of("test", "run"),
                "test run"
        );
        Duration duration = Duration.between(start, end);

        printResultsOfTestrun(count, smCountInitial, smCount, duration);

        // Ignore first test result of every executor count raise as it is always an outlier
        if(isRunAfterScaleUp()) {
            // TODO:
            //  - Get timestamp of max CPU utilization during testrun
            //  - Get CPU utilization of each executor during testrun at timestamp of max overall CPU utilization


            testResults.add(new TestResult(
                    count,
                    smCountInitial,
                    smCount,
                    duration,
                    getExcutionCountMap(),
                    getTransformationDurationStats()
            ));
        }
        metricsClient.clearTransformationLog().block();
    }

    private boolean isRunAfterScaleUp() {
        return (TEST_RUN_NO-1)%(TEST_COUNT_PER_EXECUTOR_COUNT+1) != 0;
    }

    @Override
    public void run() {
        List<List<Object>> triples = getRandomTimeseriesTriples(SOURCE_SUBMODEL_COUNT);
        System.out.println("Start registering " + SOURCE_SUBMODEL_COUNT + " submodels");
        registerAasObjectsFromTriples(
                aasRegistry,
                aasRepository,
                smRegistry,
                smRepository,
                triples
        );
    }
}
