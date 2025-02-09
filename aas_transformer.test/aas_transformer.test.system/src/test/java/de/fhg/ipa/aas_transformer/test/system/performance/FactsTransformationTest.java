package de.fhg.ipa.aas_transformer.test.system.performance;

import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.test.system.performance.model.AggregatedTestResult;
import de.fhg.ipa.aas_transformer.test.system.performance.model.TestResult;
import de.fhg.ipa.aas_transformer.test.utils.extentions.*;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.getRandomAnsibleFactsTriples;
import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.registerAasObjectsFromTriples;
import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MariaDbExtension.class)
@ExtendWith(AasITExtension.class)
@ExtendWith(RedisExtension.class)
@ExtendWith(PrometheusExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled
public class FactsTransformationTest extends AbstractPerformanceTest {
    private static final int SOURCE_SUBMODEL_COUNT = 50;
    private static final List<Integer> EXECUTOR_COUNTS = List.of(1,3,5);
    private static final int TEST_COUNT_PER_EXECUTOR_COUNT = 5;
    private static final String FILE_PREFIX = "FactsTransformationTest_";

    List<List<Object>> triples;

    @BeforeEach
    void setUp() throws Exception {
        triples = getRandomAnsibleFactsTriples(SOURCE_SUBMODEL_COUNT);
        managementClient.createTransformer(
                (Transformer) triples.get(0).get(2),
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
        executorCounts.add(Arguments.of(1));

        EXECUTOR_COUNTS.stream().forEach(executorCount -> {
            for (int i = 0; i < TEST_COUNT_PER_EXECUTOR_COUNT; i++) {
                executorCounts.add(Arguments.of(executorCount));
            }
        });

        return executorCounts.stream();
    }

    @Order(10)
    @ParameterizedTest
    // Very first test is always an outlier and will be removed in the afterAll method:
    @MethodSource("getExecutorCounts")
    public void testImpactOfExecutorCountOnTransformationSpeed(int count) throws DeserializationException {
        aasTransformerExtension.resetExecutorContainer(count);

        assertTrue(managementClient.getAllTransformer().collectList().block().size()==1);

        Instant start = Instant.now();
        registerAasObjectsFromTriples(
                aasRegistry,
                aasRepository,
                smRegistry,
                smRepository,
                triples
        );
        int smCountInitial = triples.size();

        int smCount;
        while(true) {
            smCount = smRepository.getAllSubmodels().size();
            if(smCount >= triples.size()*2)
                break;
            try {
                sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);

        printResultsOfTestrun(count, smCountInitial, smCount, duration);

        // Ignore first test result of every executor count raise as it is always an outlier
        if((TEST_RUN_NO-1)%(TEST_COUNT_PER_EXECUTOR_COUNT+1) != 0) {
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
}
