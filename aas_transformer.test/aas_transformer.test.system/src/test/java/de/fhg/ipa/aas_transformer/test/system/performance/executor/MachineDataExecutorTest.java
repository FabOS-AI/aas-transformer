package de.fhg.ipa.aas_transformer.test.system.performance.executor;

import de.fhg.ipa.aas_transformer.model.ServiceType;
import de.fhg.ipa.aas_transformer.test.utils.creator.MachineDataCreator;
import de.fhg.ipa.aas_transformer.test.utils.creator.TimeSeriesSubmodelCreator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.sleep;

@Disabled
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MachineDataExecutorTest extends AbstractExecutorTest {
    protected MachineDataExecutorTest() {
        super(MachineDataExecutorTest.class);
    }

    @ParameterizedTest
    @Order(10)
    @ValueSource(ints = {
            1, 1, 1, 1, 1,
            2, 2, 2, 2, 2,
            3, 3, 3, 3, 3
    })
    public void testMachineDataCreator(int executorCount) throws InterruptedException {
        // Scale Executor:
        scaleExecutor(executorCount, true);

        // Let Executors settle:
        sleep(settleTimeInMs);

        // Create and Start MachineDataCreator Threads:
        List<MachineDataCreator> smCreatorThreads = new ArrayList<>();
        for(int i = 0; i < submodelCreatorCount; i++)
            smCreatorThreads.add(getMachineDataJobCreator(0, 1));

        long start = System.currentTimeMillis();
        smCreatorThreads.forEach(TimeSeriesSubmodelCreator::start);

        sleep(testDurationInMs);

        // Stop Submodel Creators:
        smCreatorThreads.forEach(TimeSeriesSubmodelCreator::stop);
        Long residualTransformationJobs = redisControllerClient.getWaitingJobCount().block();
        long end = System.currentTimeMillis();

        // Scale Executor back to 0 replicas:
        scaleExecutor(0, false);
        int transformedSubmodelCount = metricsClient.getTransformationLogs().collectList().block().size();
        // clear redis and transformation logs
        metricsClient.clearTransformationLog().block();
        redisControllerClient.deleteInProgressJobs().block();
        redisControllerClient.deleteWaitingJobs().block();

        // Save and print test results
        testResults.add(new AbstractExecutorTest.TestResult(
                        executorCount,
                        submodelCreatorCount,
                        transformedSubmodelCount,
                        testDurationInMs,
                        end - start,
                        Math.toIntExact(residualTransformationJobs)
                )
        );

        System.out.println("Executor Count: " + executorCount);
        System.out.println("Submodel Creator Count: " + submodelCreatorCount);
        System.out.println("Transformed Submodel Count: " + transformedSubmodelCount);
        System.out.println("Test duration: " + testDurationInMs / 1000 + "s");
        System.out.println("Measured time: " + (end - start) / 1000 + "s");
        System.out.println("Average transformed submodels per second: " + (transformedSubmodelCount / ((end - start) / 1000.0)));
        System.out.println("Average transformed submodels per executor: " + (transformedSubmodelCount / executorCount));
        System.out.println("Residual Transformation jobs in Redis: " + residualTransformationJobs);


    }
}
