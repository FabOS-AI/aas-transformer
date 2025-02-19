package de.fhg.ipa.aas_transformer.service.management;

import com.github.dockerjava.api.model.Service;
import com.github.dockerjava.api.model.Task;
import com.github.dockerjava.api.model.TaskState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TransformerServiceHandler extends DockerHandler {
    private static final Logger LOG = LoggerFactory.getLogger(TransformerServiceHandler.class);

    @Value("${scaling.max-replicas.executor: #{5}}")
    public long MAX_REPLICAS_EXEUCTOR;
    @Value("${scaling.max-replicas.listener: #{2}}")
    public long MAX_REPLICAS_LISTENER;
    private static String EXECUTOR_SERVICE_NAME = "aas-transformer-executor";
    private static String LISTENER_SERVICE_NAME = "aas-transformer-listener";

    public void scaleExecutorService(long replicas, boolean wait) throws WaitForScaleTimeoutException {
        if(replicas<=MAX_REPLICAS_EXEUCTOR) {
            scaleService(getExecutorService(), replicas);
            if (wait)
                waitExecutorScaleToFinish();
        } else
            LOG.warn("Replica count must be less or equal {}. Scaling aborted.", MAX_REPLICAS_EXEUCTOR);
    }

    public void scaleListenerService(long replicas, boolean wait) throws WaitForScaleTimeoutException {
        if(replicas<=MAX_REPLICAS_LISTENER) {
            scaleService(getListenerService(), replicas);
            if(wait)
                waitListenerScaleToFinish();
        }
        else
            LOG.warn("Replica count must be less or equal {}. Scaling aborted.", MAX_REPLICAS_LISTENER);
    }

    public List<Task> getRunningExecutorServiceTasks() {
        return getTasksOfServiceFilteredByState(getExecutorService(), TaskState.RUNNING);
    }

    public List<Task> getRunningListenerServiceTasks() {
        return getTasksOfServiceFilteredByState(getListenerService(), TaskState.RUNNING);
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

    public void waitExecutorScaleToFinish() throws WaitForScaleTimeoutException {
        waitScaleToFinish(getExecutorService());
    }

    public void waitListenerScaleToFinish() throws WaitForScaleTimeoutException {
        waitScaleToFinish(getListenerService());
    }
}
