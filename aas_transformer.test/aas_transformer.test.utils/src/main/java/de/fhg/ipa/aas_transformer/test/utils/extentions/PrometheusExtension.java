package de.fhg.ipa.aas_transformer.test.utils.extentions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.fhg.ipa.aas_transformer.test.utils.AasTestObjects;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PrometheusExtension extends AbstractExtension implements BeforeAllCallback, AfterAllCallback {
    private static WebClient GRAFANA_WEBCLIENT;
    private static String GRAFANA_BASE_URL;
    private static final String GRAFANA_USERNAME = "admin";
    private static final String GRAFANA_PASSWORD = "admin";
    private static final List<String> GRAFANA_DASHBOARD_FILES = List.of(
            "grafana_dashboard_container.json",
            "grafana_dashboard_container_groups.json",
            "grafana_dashboard_executor_test.json",
            "grafana_dashboard_transformer.json",
            "grafana_dashboard_transformer_container.json"
    );
    private static final ObjectMapper objectMapper = new ObjectMapper();

    protected Network containerNetwork = Network.newNetwork();
    protected GenericContainer prometheusContainer;
    protected GenericContainer grafanaContainer;
    protected GenericContainer cadvisorContainer;
    protected List<GenericContainer> runningContainers = new ArrayList<>();

    Map<String, String> containerLabels = Map.of(
            "de.fhg.ipa.aas-transformer.container-group","monitoring"
    );

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {

        if(runningContainers.size() == 0) {
            prometheusContainer = getPrometheusContainer();
            grafanaContainer = getGrafanaContainer();
            cadvisorContainer = getCadvisorContainer();

            startContainer(List.of(prometheusContainer, grafanaContainer, cadvisorContainer));
            runningContainers.addAll(List.of(prometheusContainer, grafanaContainer, cadvisorContainer));
        }

        Integer prometheusPort = prometheusContainer.getMappedPort(9090);
        Integer grafanaPort = grafanaContainer.getMappedPort(3000);
        Integer cadvisorPort = cadvisorContainer.getMappedPort(8080);

        String prometheusUrl = "http://host.docker.internal:" + prometheusPort;
        String cadvisorUrl = "http://host.docker.internal:" + cadvisorPort;
        GRAFANA_BASE_URL = "http://host.docker.internal:" + grafanaPort;

        // Create Datasource and Dashboard in Grafana
        String datasourceUid = createGrafanaDatasource(prometheusUrl);
        GRAFANA_DASHBOARD_FILES.forEach(fileName -> {
            try {
                createGrafanaDashboard(fileName, datasourceUid);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        ;

        System.out.println("Prometheus Address: " + prometheusUrl);
        System.out.println("Grafana Address: " + GRAFANA_BASE_URL + "/dashboards");
        System.out.println("cAdvisor Address: " + cadvisorUrl);

        super.beforeAll(extensionContext);
    }

    private GenericContainer getPrometheusContainer() {
        return new GenericContainer("prom/prometheus:v2.55.1")
                .withLabels(containerLabels)
                .withNetwork(containerNetwork)
                .withNetworkAliases("prometheus")
                .withExposedPorts(9090)
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("prometheus.yml"),
                        "/etc/prometheus/prometheus.yml"
                );
    }

    private GenericContainer getGrafanaContainer() {
        return new GenericContainer("grafana/grafana:11.4.0")
                .withLabels(containerLabels)
                .withNetwork(containerNetwork)
                .withNetworkAliases("grafana")
                .withExposedPorts(3000)
                .withEnv("GF_SECURITY_ADMIN_USER", GRAFANA_PASSWORD)
                .withEnv("GF_SECURITY_ADMIN_PASSWORD", GRAFANA_USERNAME)
                .withEnv("GF_USERS_ALLOW_SIGN_UP", "false")
                .withEnv("GF_USERS_ALLOW_ORG_CREATE", "false")
                .withEnv("GF_USERS_AUTO_ASSIGN_ORG", "true")
                .withEnv("GF_USERS_AUTO_ASSIGN_ORG_ROLE", "Viewer");
    }

    private GenericContainer getCadvisorContainer() {
        GenericContainer container = new GenericContainer("gcr.io/cadvisor/cadvisor:v0.49.2")
                .withLabels(containerLabels)
                .withNetwork(containerNetwork)
                .withNetworkAliases("cadvisor")
                .withFileSystemBind("/var/run/docker.sock", "/var/run/docker.sock", BindMode.READ_ONLY)
                .withFileSystemBind("/", "/rootfs", BindMode.READ_ONLY)
                .withFileSystemBind("/var/run", "/var/run", BindMode.READ_ONLY)
                .withFileSystemBind("/sys", "/sys", BindMode.READ_ONLY)
                .withFileSystemBind("/var/lib/docker", "/var/lib/docker", BindMode.READ_ONLY);
        container.setPortBindings(List.of("8080:8080"));
        return container;
    }

    public void createAnnotation(Instant start, Instant end, List<String> tags, String text) {
        Map<String, Object> body = Map.of(
                "time", start.toEpochMilli(),
                "timeEnd", end.toEpochMilli(),
                "tags", tags,
                "text", text
        );
        JsonNode jsonNodeBody = objectMapper.valueToTree(body);

        ClientResponse response = sendPostToGrafana(
                "/api/annotations",
                jsonNodeBody
        );
    }


    private void createGrafanaDashboard(String fileName, String datasourceUid) throws IOException {
        JsonNode grafanaDashboard = loadJsonFile(fileName);
        String grafanaDashboardString = grafanaDashboard.toString();
        grafanaDashboardString = grafanaDashboardString.replaceAll("<<datasource-uid>>", datasourceUid);
        grafanaDashboard = new ObjectMapper().readTree(grafanaDashboardString);
        ((ObjectNode)grafanaDashboard).remove("meta");
        try {
            ((ObjectNode) grafanaDashboard.get("dashboard")).remove("id");
        } catch (NullPointerException e) {}

        ClientResponse response = sendPostToGrafana("/api/dashboards/db", grafanaDashboard);
        System.out.println("Response code for creating \"" + fileName + "\": "+response.statusCode());
        return;
    }

    private String createGrafanaDatasource(
            String prometheusUrl
    ) throws IOException {
        JsonNode grafanaDatasource = loadJsonFile("grafana_datasources.json");

        // rewrite prometheus url
        ((ObjectNode)grafanaDatasource).put("url", prometheusUrl);

        // send datasource definition to grafana
        ClientResponse response = sendPostToGrafana("/api/datasources", grafanaDatasource);
        ObjectNode responseBody = response.bodyToMono(ObjectNode.class).block();
        return responseBody.get("datasource").get("uid").asText();
    }

    private ClientResponse sendGetToGrafana(String grafanaPath) {
        return getGrafanaWebclient().get().uri(grafanaPath)
                .exchange()
                .block();
    }

    private ClientResponse sendPostToGrafana(String grafanaPath, JsonNode body) {
        return getGrafanaWebclient().post().uri(grafanaPath)
                .bodyValue(body)
                .exchange()
                .block();
    }

    private WebClient getGrafanaWebclient() {
        if(GRAFANA_WEBCLIENT == null) {
            GRAFANA_WEBCLIENT = WebClient.builder()
                    .baseUrl(GRAFANA_BASE_URL)
                    .filter(ExchangeFilterFunctions.basicAuthentication(GRAFANA_USERNAME, GRAFANA_PASSWORD))
                    .build();
        }
        return GRAFANA_WEBCLIENT;
    }

    private JsonNode loadJsonFile(String fileName) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        ClassLoader classLoader = AasTestObjects.class.getClassLoader();
        URL resourceUrl = classLoader.getResource(fileName);
        File file = new File(resourceUrl.getFile());

        return objectMapper.readTree(file);
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        if (runningContainers.size() > 0 && extensionContext.getParent().get().getParent().isEmpty()) {
            for (var container : runningContainers) {
                container.stop();
            }
            runningContainers = new ArrayList<>();
        }

        super.afterAll(extensionContext);
    }
}
