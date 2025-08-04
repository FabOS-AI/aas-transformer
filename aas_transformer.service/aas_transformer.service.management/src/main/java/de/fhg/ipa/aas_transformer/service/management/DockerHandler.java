package de.fhg.ipa.aas_transformer.service.management;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import de.fhg.ipa.aas_transformer.service.management.exceptions.WaitForScaleTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.lang.Thread.sleep;

abstract class DockerHandler {
    private static final Logger LOG = LoggerFactory.getLogger(DockerHandler.class);

    private DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
    private DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder().dockerHost(config.getDockerHost()).build();
    protected DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);

    protected DockerClient getDockerClient() {
        return dockerClient;
    }

    Service getServiceByName(String name) {
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

    protected long getReplicasOfService(Service service) {
        try {
            return service.getSpec().getMode().getReplicated().getReplicas();
        } catch (NullPointerException e) {
            return 0L;
        }
    }

    protected void scaleService(Service service, long replicas) {
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

    protected List<Task> getTasksOfService(Service service) {
        return dockerClient.listTasksCmd().withServiceFilter(service.getId()).exec();
    }

    protected List<Task> getTasksOfServiceFilteredByState(Service service, TaskState state) {
        return dockerClient.listTasksCmd().withServiceFilter(service.getId()).exec().stream()
                .filter(task -> task.getStatus().getState().equals(state))
                .toList();
    }

    protected void waitScaleToFinish(Service service) throws WaitForScaleTimeoutException {
        int tryCount = 0;
        int maxCount = 1000;
        long replicaCount;
        int runningTasksCount;
        do {
            tryCount++;
            if(tryCount > maxCount)
                throw(new WaitForScaleTimeoutException("Scaling of service did not finish after " + maxCount + " tries."));

            try {
                sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            replicaCount = getReplicasOfService(service);
            runningTasksCount = getTasksOfServiceFilteredByState(service, TaskState.RUNNING).size();
        } while (replicaCount != runningTasksCount);

        LOG.info("Scaling of service '{}' finished.", service.getSpec().getName());
    }
}
