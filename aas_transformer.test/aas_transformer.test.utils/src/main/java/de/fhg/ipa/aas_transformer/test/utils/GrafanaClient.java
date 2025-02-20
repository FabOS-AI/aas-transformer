package de.fhg.ipa.aas_transformer.test.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class GrafanaClient {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private WebClient webClient;
    private static String GRAFANA_BASE_URL;

    public GrafanaClient(String baseUrl, String username, String password) {
        webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .filter(ExchangeFilterFunctions.basicAuthentication(username, password))
                .build();
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

    public void createGrafanaDashboard(String fileName, String datasourceUid) throws IOException {
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

    public String createGrafanaDatasource(
            String prometheusUrl,
            String filename
    ) throws IOException {
        JsonNode grafanaDatasource = loadJsonFile(filename);

        // rewrite prometheus url
        ((ObjectNode)grafanaDatasource).put("url", prometheusUrl);

        // send datasource definition to grafana
        ClientResponse response = sendPostToGrafana("/api/datasources", grafanaDatasource);
        ObjectNode responseBody = response.bodyToMono(ObjectNode.class).block();
        return responseBody.get("datasource").get("uid").asText();
    }

    private ClientResponse sendGetToGrafana(String grafanaPath) {
        return webClient.get().uri(grafanaPath)
                .exchange()
                .block();
    }

    private ClientResponse sendPostToGrafana(String grafanaPath, JsonNode body) {
        return webClient.post().uri(grafanaPath)
                .bodyValue(body)
                .exchange()
                .block();
    }

    private JsonNode loadJsonFile(String fileName) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        ClassLoader classLoader = AasTestObjects.class.getClassLoader();
        URL resourceUrl = classLoader.getResource(fileName);
        File file = new File(resourceUrl.getFile());

        return objectMapper.readTree(file);
    }
}
