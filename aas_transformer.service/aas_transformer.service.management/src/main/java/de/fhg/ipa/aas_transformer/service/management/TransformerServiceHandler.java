package de.fhg.ipa.aas_transformer.service.management;

import com.github.dockerjava.api.model.Service;
import com.github.dockerjava.api.model.Task;
import com.github.dockerjava.api.model.TaskState;
import de.fhg.ipa.aas_transformer.model.ScaleDirection;
import de.fhg.ipa.aas_transformer.model.alertmanager.Alert;
import de.fhg.ipa.aas_transformer.model.alertmanager.AlertMessage;
import de.fhg.ipa.aas_transformer.service.management.exceptions.WaitForScaleTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TransformerServiceHandler extends DockerHandler {
    private static final Logger LOG = LoggerFactory.getLogger(TransformerServiceHandler.class);

    @Value("${scaling.alert-enabled}")
    public boolean alertEnabled;

    @Value("${scaling.max-replicas.executor: #{5}}")
    public long MAX_REPLICAS_EXEUCTOR;
    @Value("${scaling.max-replicas.listener: #{2}}")
    public long MAX_REPLICAS_LISTENER;
    private static String EXECUTOR_SERVICE_NAME = "aas-transformer-executor";
    private static String LISTENER_SERVICE_NAME = "aas-transformer-listener";

    public void handleScaleAlert(AlertMessage alertMessage) throws WaitForScaleTimeoutException {
        LOG.info("Received alert message: {}", alertMessage);

        if(!alertEnabled) {
            LOG.warn("Scaling by alerts is disabled. Scaling aborted.");
            return;
        }

        if(alertMessage.getAlerts().size() > 0) {
            Alert alert = alertMessage.getAlerts().get(0);
            String action = alert.getLabels().get("action");
            String service = alert.getLabels().get("service");
            ScaleDirection scaleDirection;

            if(action.equals("scale-to-minimum")) {
                scaleService(service, 1, false);
                return;
            }

            if(action.equals("scale-down"))
                scaleDirection = ScaleDirection.SCALE_DOWN;
            else if(action.equals("scale-up"))
                scaleDirection = ScaleDirection.SCALE_UP;
            else {
                LOG.warn("Unknown action: {}. Scaling aborted.", action);
                return;
            }

            scaleServiceByOne(service, scaleDirection, false);
        }
    }

    public void scaleExecutorServiceByOne(ScaleDirection scaleDirection, boolean wait) throws WaitForScaleTimeoutException {
        if(scaleDirection == ScaleDirection.SCALE_UP)
            scaleExecutorService(getReplicaCountOfExecutorService()+1, wait);
        else
            scaleExecutorService(getReplicaCountOfExecutorService()-1, wait);
    }

    public void scaleService(String serviceName, long replicas, boolean wait) throws WaitForScaleTimeoutException {
        switch(serviceName) {
            case "executor":
                scaleExecutorService(replicas, wait);
                break;
            case "listener":
                scaleListenerService(replicas, wait);
                break;
            default:
                LOG.warn("Unknown service name: {}. Scaling aborted.", serviceName);
        }
    }

    public void scaleServiceByOne(String serviceName, ScaleDirection scaleDirection, boolean wait) throws WaitForScaleTimeoutException {
        switch(serviceName) {
            case "executor":
                scaleExecutorServiceByOne(scaleDirection, wait);
                break;
            case "listener":
                scaleListenerServiceByOne(scaleDirection, wait);
                break;
            default:
                LOG.warn("Unknown service name: {}. Scaling aborted.", serviceName);
        }
    }

    public void scaleExecutorService(long replicas, boolean wait) throws WaitForScaleTimeoutException {
        if(replicas<=MAX_REPLICAS_EXEUCTOR) {
            scaleService(getExecutorService(), replicas);
            if (wait)
                waitExecutorScaleToFinish();
        } else
            LOG.warn("Replica count must be less or equal {}. Scaling aborted.", MAX_REPLICAS_EXEUCTOR);
    }

    public void scaleListenerServiceByOne(ScaleDirection scaleDirection, boolean wait) throws WaitForScaleTimeoutException {
        if(scaleDirection == ScaleDirection.SCALE_UP)
            scaleListenerService(getReplicaCountOfListenerService()+1, wait);
        else
            scaleListenerService(getReplicaCountOfListenerService()-1, wait);
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

    public boolean isExecutorScaling() {
        return getReplicaCountOfExecutorService() != getRunningExecutorServiceTasks().size();
    }

    public boolean isListenerScaling() {
        return getReplicaCountOfListenerService() != getRunningListenerServiceTasks().size();
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
