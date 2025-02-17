package de.fhg.ipa.aas_transformer.service.management;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Service;
import com.github.dockerjava.api.model.ServiceModeConfig;
import com.github.dockerjava.api.model.ServiceReplicatedModeOptions;
import com.github.dockerjava.api.model.ServiceSpec;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DockerHandler {
    private static final Logger LOG = LoggerFactory.getLogger(DockerHandler.class);
    @Value("${scaling.max-replicas.executor: #{5}}")
    public long MAX_REPLICAS_EXEUCTOR;
    @Value("${scaling.max-replicas.listener: #{2}}")
    public long MAX_REPLICAS_LISTENER;
    private static String EXECUTOR_SERVICE_NAME = "aas-transformer-executor";
    private static String LISTENER_SERVICE_NAME = "aas-transformer-listener";

    DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
    DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder().dockerHost(config.getDockerHost()).build();
    DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);

    public DockerClient getDockerClient() {
        return dockerClient;
    }

    private Service getServiceByName(String name) {
        List<Service> services = dockerClient.listServicesCmd().exec().stream()
                .filter(service -> service.getSpec().getName().contains(name))
                .toList();

        if(services.size() == 0)
            return null;
        else if(services.size() > 1) {
            LOG.warn("Found more than one swarm service which contain '{}' in their names.", name);
        }
        return services.get(0);
    }

    private long getReplicasOfService(Service service) {
        return service.getSpec().getMode().getReplicated().getReplicas();
    }

    public void scaleService(Service service, long replicas) {
        if(replicas < 1) {
            LOG.warn("Desired replica count must be greater than 0. Scaling aborted.");
            return;
        }

        ServiceSpec serviceSpec = service.getSpec();
        ServiceModeConfig serviceModeConfig = service.getSpec().getMode();
        ServiceReplicatedModeOptions replicationOptions = service.getSpec().getMode().getReplicated().withReplicas((int) replicas);
        serviceModeConfig = serviceModeConfig.withReplicated(replicationOptions);
        serviceSpec = serviceSpec.withMode(serviceModeConfig);

        dockerClient.updateServiceCmd(service.getId(), serviceSpec)
                .withVersion(service.getVersion().getIndex())
                .exec();

        LOG.info("Service '{}' scaled to {} replica(s).", service.getSpec().getName(), replicas);
    }

    public void scaleExecutorService(long replicas) {
        if(replicas<=MAX_REPLICAS_EXEUCTOR)
            scaleService(getExecutorService(), replicas);
        else
            LOG.warn("Replica count must be less or equal {}. Scaling aborted.", MAX_REPLICAS_EXEUCTOR);
    }

    public void scaleListenerService(long replicas) {
        if(replicas<=MAX_REPLICAS_LISTENER)
            scaleService(getListenerService(), replicas);
        else
            LOG.warn("Replica count must be less or equal {}. Scaling aborted.", MAX_REPLICAS_LISTENER);
    }

    public Service getExecutorService() {
        return getServiceByName(EXECUTOR_SERVICE_NAME);
    }

    public Service getListenerService() {
        return getServiceByName(LISTENER_SERVICE_NAME);
    }

    public long getReplicaCountOfExecutorService() {
        return getReplicasOfService(getExecutorService());
    }

    public long getReplicaCountOfListenerService() {
        return getReplicasOfService(getListenerService());
    }
}
