package de.fhg.ipa.aas_transformer.test.utils.extentions;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MariaDbExtension extends SpringExtension implements BeforeAllCallback, AfterAllCallback {
    protected GenericContainer mariaDbContainer;
    protected List<GenericContainer> runningContainers = new ArrayList<>();

    Map<String, String> containerLabels = Map.of(
            "de.fhg.ipa.aas-transformer.container-group","transformer"
    );

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        var mariaDbSchema = "aas_transformer";
        var mariaDbUser = "aas_transformer";
        var mariaDbPassword = "aas_transformer";

        if(runningContainers.size() == 0) {
            mariaDbContainer = new GenericContainer("mariadb:10.5")
                    .withLabels(containerLabels)
                    .withEnv("MARIADB_ROOT_PASSWORD", "root")
                    .withEnv("MARIADB_DATABASE", mariaDbSchema)
                    .withEnv("MARIADB_USER", mariaDbUser)
                    .withEnv("MARIADB_PASSWORD", mariaDbPassword)
                    .withExposedPorts(3306);
            mariaDbContainer.start();
            runningContainers.add(mariaDbContainer);
        }

        var mariaDbPort = mariaDbContainer.getMappedPort(3306);

        System.setProperty("database.host", "localhost");
        System.setProperty("database.port", mariaDbPort.toString());
        System.setProperty("database.schema", mariaDbSchema);
        System.setProperty("database.username", mariaDbUser);
        System.setProperty("database.password", mariaDbPassword);

        super.beforeAll(extensionContext);
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
