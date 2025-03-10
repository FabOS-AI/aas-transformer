package de.fhg.ipa.aas_transformer.test.system.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.clients.alertmanager.AlertManagerClient;
import de.fhg.ipa.aas_transformer.clients.management.*;
import de.fhg.ipa.aas_transformer.clients.prometheus.PrometheusClient;
import de.fhg.ipa.aas_transformer.clients.prometheus.model.Alert;
import de.fhg.ipa.aas_transformer.clients.prometheus.model.AlertState;
import de.fhg.ipa.aas_transformer.model.TransformationLog;
import de.fhg.ipa.aas_transformer.test.system.performance.model.AggregatedTestResult;
import de.fhg.ipa.aas_transformer.test.system.performance.model.TestResult;
import de.fhg.ipa.aas_transformer.test.system.performance.model.TransformationDurations;
import de.fhg.ipa.aas_transformer.test.utils.GrafanaClient;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.basyx.submodelregistry.client.model.SubmodelDescriptor;
import org.junit.jupiter.api.AfterEach;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.registerAasObjectsFromTriples;
import static de.fhg.ipa.aas_transformer.test.utils.AasTimeseriesObjects.getRandomTimeseriesTriples;
import static java.lang.Thread.sleep;

public abstract class ExtAbstractPerformanceTest {
    protected static int TEST_RUN_NO = 1;

    //region Test Vars
    // IP addresses/hostnames:
    String host = "aas-transformer.local";

    // Subdomains
    String transformerManagementSubdomain = "management";
    String transformerExecutorSubdomain = "executor";
    String grafanaSubdomain = "grafana";

    // Service Ports:
    String transformerManagementPort = "80";
    String transformerExecutorPort = "80";
    String grafanaPort = "80";
    String aasRegistryPort = "80";
    String aasRegistryPath = "/shell-registry";
    String aasRepositoryPort = "80";
    String aasRepositoryPath = "";
    String smRegistryPort = "80";
    String smRegistryPath = "/sm-registry";
    String smRepositoryPort = "80";
    String smRepositoryPath = "";

    // Transformer Service Urls:
    String transformerManagementUrl = "http://" + transformerManagementSubdomain + "." + host +":" + transformerManagementPort;
    String transformerExecutorUrl = "http://" + transformerExecutorSubdomain + "." + host + ":" + transformerExecutorPort;
    String grafanaUrl = "http://"+ grafanaSubdomain + "." + host +":" + grafanaPort;

    // AAS Service Urls:
    String aasRegistryUrl = "http://"+host+":"+aasRegistryPort+aasRegistryPath;
    String aasRepoUrl = "http://"+host+":"+aasRepositoryPort+aasRepositoryPath;
    String smRegistryUrl = "http://"+host+":"+smRegistryPort+smRegistryPath;
    String smRepoUrl = "http://"+host+":"+smRepositoryPort+smRepositoryPath;

    // Service Clients:
    protected static ManagementClient managementClient;
    protected static WebClient executorWebclient;
    protected static MetricsClient metricsClient;
    protected static JobsClient jobsClient;
    protected static ScalingClient scalingClient;
    protected static AasRegistry aasRegistry;
    protected static AasRepository aasRepository;
    protected static SubmodelRegistry smRegistry;
    protected static SubmodelRepository smRepository;
    protected static GrafanaClient grafanaClient;

    // Credentials
    static String grafanaUsername = "admin";
    static String grafanaPassword = "admin";

    // Result vars:
    static List<TestResult> testResults = new ArrayList<>();
    static AggregatedTestResult aggregatedTestResult;
    // endregion


    public ExtAbstractPerformanceTest() {
        managementClient = new ManagementClient(transformerManagementUrl);
        executorWebclient = getExecutorWebclient(transformerExecutorUrl);
        metricsClient = new MetricsClient(transformerManagementUrl);
        jobsClient = new JobsClient(transformerManagementUrl);
        scalingClient = new ScalingClient(transformerManagementUrl);
        grafanaClient = new GrafanaClient(grafanaUrl, grafanaUsername, grafanaPassword);

        aasRegistry = new AasRegistry(aasRegistryUrl, aasRepoUrl);
        aasRepository = new AasRepository(aasRepoUrl);
        smRegistry = new SubmodelRegistry(smRegistryUrl, smRepoUrl);
        smRepository = new SubmodelRepository(smRepoUrl);
    }

    private WebClient getExecutorWebclient(String baseUrl) {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(new SubmodelSerializer(Submodel.class));
        objectMapper.registerModule(simpleModule);

        ExchangeStrategies strategies = ExchangeStrategies
                .builder()
                .codecs(clientDefaultCodecsConfigurer -> {
                    clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON));
                    clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON));
                }).build();

        return WebClient.builder()
                .baseUrl(baseUrl)
                .exchangeStrategies(strategies)
                .build();
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

    protected void getAllSubmodels() {
        List<Submodel> submodels = smRepository.getSubmodelsWithLimit(100);
    }

    protected void batchDeleteAllSubmodels() {
        List<Submodel> submodels = smRepository.getSubmodelsWithLimit(100);
        int batchCount = 0;
        do {
            batchCount++;
            System.out.println("Batch " + batchCount + " - Deleting " + submodels.size() + " submodels");
            submodels.forEach(submodel -> {
                smRepository.deleteSubmodel(submodel.getId());
            });
            System.out.println("Batch #" + batchCount + " finished.");
            submodels = smRepository.getSubmodelsWithLimit(100);
        } while(submodels.size() > 0);
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

    class AlertWatcher {
        PrometheusAlertWatcher prometheusAlertWatcher;
        AlertManagerWatcher alertManagerWatcher;
        public AlertWatcher(String prometheusBaseUrl, String alertmanagerBaseUrl, GrafanaClient grafanaClient) {
            prometheusAlertWatcher = new PrometheusAlertWatcher(prometheusBaseUrl, grafanaClient);
            alertManagerWatcher = new AlertManagerWatcher(alertmanagerBaseUrl, grafanaClient);
        }

        public void start() {
            prometheusAlertWatcher.start();
            alertManagerWatcher.start();
        }

        public void stop() throws InterruptedException {
            prometheusAlertWatcher.stop();
            alertManagerWatcher.stop();
        }
    }

    class AlertManagerWatcher implements Runnable {
        Thread thread = new Thread(this);
        boolean stopped = false;
        AlertManagerClient alertManagerClient;
        GrafanaClient grafanaClient;
        Map<String, OffsetDateTime> latestFiredAlerts = new HashMap<>();

        public AlertManagerWatcher(String baseUrl, GrafanaClient grafanaClient) {
            alertManagerClient = new AlertManagerClient(baseUrl);
            this.grafanaClient = grafanaClient;
        }

        public void start() {
            thread.start();
            this.stopped = false;
        }

        public void stop() throws InterruptedException {
            this.stopped = true;
            System.out.println("Alertmanager alertList: " + latestFiredAlerts.toString());
            thread.join();
        }

        @Override
        public void run() {
            while (!stopped) {
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                List<de.fhg.ipa.aas_transformer.clients.alertmanager.model.Alert> alerts = alertManagerClient.getAlerts();
                alerts.forEach(alert -> {
                    if(addAlertToList(alert))
                        createGrafanaAnnotation(alert);
                });
            }
        }

        private void createGrafanaAnnotation(de.fhg.ipa.aas_transformer.clients.alertmanager.model.Alert alert) {
            grafanaClient.createAnnotation(
                    alert.getStartsAt().toInstant(),
                    alert.getStartsAt().toInstant(),
                    List.of("alertmanager", "alert", alert.getAlertname()),
                    "Alert: " + alert.getAlertname()
            );
        }

        private boolean addAlertToList(de.fhg.ipa.aas_transformer.clients.alertmanager.model.Alert alert) {
            String alertName = alert.getAlertname();
            OffsetDateTime currentAlertDate = latestFiredAlerts.get(alertName);
            OffsetDateTime newAlertDate = alert.getStartsAt();
            if(currentAlertDate != newAlertDate) {
                latestFiredAlerts.put(alertName, newAlertDate);
                return true;
            }
            return false;
        }
    }

    class PrometheusAlertWatcher implements Runnable {
        Thread thread = new Thread(this);
        boolean stopped = false;
        PrometheusClient prometheusClient;
        GrafanaClient grafanaClient;
        Map<String, OffsetDateTime> latestFiredAlerts = new HashMap<>();

        public PrometheusAlertWatcher(String prometheusBaseUrl, GrafanaClient grafanaClient) {
            prometheusClient = new PrometheusClient(prometheusBaseUrl);
            this.grafanaClient = grafanaClient;
        }

        public void start() {
            thread.start();
            this.stopped = false;
        }

        public void stop() throws InterruptedException {
            this.stopped = true;
            System.out.println("Prometheus alertList: " + latestFiredAlerts.toString());
            thread.join();
        }

        @Override
        public void run() {
            while (!stopped) {
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                List<Alert> alerts = prometheusClient.getAlerts().getData().getAlerts();
                alerts.forEach(alert -> {
                    if(alert.getState().equals(AlertState.firing) && addAlertToList(alert))
                        createGrafanaAnnotation(alert);
                });
            }
        }

        private void createGrafanaAnnotation(Alert alert) {
            grafanaClient.createAnnotation(
                    alert.getActiveAt().toInstant(),
                    alert.getActiveAt().toInstant(),
                    List.of("prometheus", "alert", alert.getAlertName()),
                    "Alert: " + alert.getAlertName()
            );
        }

        private boolean addAlertToList(Alert alert) {
            String alertName = alert.getAlertName();
            OffsetDateTime currentAlertDate = latestFiredAlerts.get(alertName);
            OffsetDateTime newAlertDate = alert.getActiveAt();
           if(currentAlertDate != newAlertDate) {
               latestFiredAlerts.put(alertName, newAlertDate);
               return true;
           }
          return false;
        }
    }

    class TimeSeriesSubmodelCreator implements Runnable {

        Thread thread = new Thread(this);
        boolean stopped = false;
        int submodelCount = 0;
        int sleepInMs = 0;
        List<String> submodelIds = new LinkedList<>();
        SubmodelRemover submodelRemover = new SubmodelRemover(this);

        public TimeSeriesSubmodelCreator() {}

        public TimeSeriesSubmodelCreator(int submodelCount, int sleepInMs) {
            this.submodelCount = submodelCount;
            this.sleepInMs = sleepInMs;
        }

        public void start() {
            thread.start();
            this.stopped = false;
        }

        public void stop() throws InterruptedException {
            this.stopped = true;
            thread.join();
        }

        @Override
        public void run() {
            if(submodelCount == 0) {
                System.out.println("Start registering submodels...");
                while(!this.stopped) {
                    List<List<Object>> triples = getRandomTimeseriesTriples(1);
                    registerAasObjectsFromTriples(
                            aasRegistry,
                            aasRepository,
                            smRegistry,
                            smRepository,
                            triples
                    );
                    submodelIds.add(
                            ((Submodel)triples.get(0).get(1)).getId()
                    );
                    try {
                        sleep(sleepInMs);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                List<List<Object>> triples = getRandomTimeseriesTriples(submodelCount);
                System.out.println("Start registering " + submodelCount + " submodels");
                registerAasObjectsFromTriples(
                        aasRegistry,
                        aasRepository,
                        smRegistry,
                        smRepository,
                        triples
                );
            }
        }
    }

    class SubmodelRemover implements Runnable {
        Thread thread = new Thread(this);
        boolean stopped = false;
        TimeSeriesSubmodelCreator creator;

        public SubmodelRemover(TimeSeriesSubmodelCreator creator) {
            this.creator = creator;
        }

        public void start() {
            thread.start();
            stopped = false;
        }

        public void stop() {
            stopped = true;
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run() {
            while(!stopped) {
                if (creator.submodelIds.size() > 0) {
                    String submodelId = creator.submodelIds.remove(0);
                    aasRepository.getAllAasContainingSubmodelBySubmodelId(submodelId).forEach(aas -> {
                        List<Reference> submodelRefs = aas.getSubmodels();
                        while(submodelRefs.size() < 2) {
                            try {
                                sleep(10);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            submodelRefs = aasRepository.getAas(aas.getId()).getSubmodels();
                        }
                        submodelRefs.forEach(submodel -> {
                            String smId = submodel.getKeys().get(0).getValue();
                            try {
                                smRepository.deleteSubmodel(smId);
                            } catch (Exception e) {
                                System.out.println("Error deleting submodel: " + smId);
                            }
                        });

                    });
                }
                try {
                    sleep(creator.sleepInMs*2);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
