package de.fhg.ipa.aas_transformer.service.management;

import com.github.dockerjava.api.model.Service;
import com.github.dockerjava.api.model.Task;
import com.github.dockerjava.api.model.TaskState;
import de.fhg.ipa.aas_transformer.model.ScaleDirection;
import de.fhg.ipa.aas_transformer.model.alertmanager.Alert;
import de.fhg.ipa.aas_transformer.model.alertmanager.AlertMessage;
import de.fhg.ipa.aas_transformer.service.management.exceptions.WaitForScaleTimeoutException;
import de.fhg.ipa.aas_transformer.model.ServiceType;
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

    private boolean listenerScalingEnabled = true;
    private boolean executorScalingEnabled = true;

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
            ServiceType serviceType = null;
            ScaleDirection scaleDirection;

            switch(service) {
                case "executor":
                    serviceType = ServiceType.EXECUTOR;
                    break;
                case "listener":
                    serviceType = ServiceType.LISTENER;
                    break;
                default:
                    LOG.warn("Unknown service: {}. Scaling aborted.", service);
                    return;
            }

            if(action.equals("scale-to-minimum")) {
                scaleServiceType(serviceType, 1, false);
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

            scaleServiceTypeByOne(serviceType, scaleDirection, false);
        }
    }

    public void enableServiceType(ServiceType serviceType, Boolean enabled) {
        switch(serviceType) {
            case EXECUTOR:
                executorScalingEnabled = enabled;
                break;
            case LISTENER:
                listenerScalingEnabled = enabled;
                break;
            default:
                LOG.warn("Unknown service type: {}. Scaling not changed.", serviceType);
                return;
        }

        LOG.info("{} scaling is now {}", serviceType.name(), enabled ? "enabled" : "disabled");
    }

    public void scaleServiceTypeByOne(ServiceType serviceType, ScaleDirection scaleDirection, boolean wait) throws WaitForScaleTimeoutException {
        long currentReplicas = getReplicaCountOfServiceType(serviceType);
        long desiredScale = (scaleDirection.equals(ScaleDirection.SCALE_UP)) ? ++currentReplicas : --currentReplicas;
        scaleServiceType(serviceType, desiredScale, wait);
    }

    public void scaleServiceType(ServiceType serviceType, long replicas, boolean wait) throws WaitForScaleTimeoutException {
        Service service = null;
        int maxReplicas = 0;

        if(!isScalingOfServiceTypeEnabled(serviceType))
            return;

        switch(serviceType) {
            case EXECUTOR:
                service = getExecutorService();
                maxReplicas = (int) MAX_REPLICAS_EXEUCTOR;
                break;
            case LISTENER:
                service = getListenerService();
                maxReplicas = (int) MAX_REPLICAS_LISTENER;
                break;
            default:
                LOG.warn("Unknown service type: {}. Scaling aborted.", serviceType);
                return;
        }

        if(replicas<=maxReplicas) {
            scaleService(service, replicas);
            if (wait)
                waitExecutorScaleToFinish();
        } else
            LOG.warn("Replica count must be less or equal {}. Scaling aborted.", MAX_REPLICAS_EXEUCTOR);
    }

    private boolean isScalingOfServiceTypeEnabled(ServiceType serviceType) {
        switch(serviceType) {
            case EXECUTOR:
                return executorScalingEnabled;
            case LISTENER:
                return listenerScalingEnabled;
            default:
                LOG.warn("Unknown service type: {}. Scaling is not enabled.", serviceType);
                return false;
        }
    }

    public boolean isServiceTypeScaling(ServiceType serviceType) {
        switch(serviceType) {
            case EXECUTOR:
            case LISTENER:
                return getReplicaCountOfServiceType(serviceType) != getRunningServiceTasksOfServiceType(serviceType).size();
            default:
                LOG.warn("Unknown service type: {}. Returning false.", serviceType);
                return false;
        }
    }

    public List<Task> getRunningServiceTasksOfServiceType(ServiceType serviceType) {
        switch(serviceType) {
            case EXECUTOR:
                return getTasksOfServiceFilteredByState(getExecutorService(), TaskState.RUNNING);
            case LISTENER:
                return getTasksOfServiceFilteredByState(getListenerService(), TaskState.RUNNING);
            default:
                LOG.warn("Unknown service type: {}. Returning empty list.", serviceType);
                return List.of();
        }
    }

    public Service getExecutorService() {
        return getServiceByName(EXECUTOR_SERVICE_NAME);
    }

    public Service getListenerService() {
        return getServiceByName(LISTENER_SERVICE_NAME);
    }

    public long getReplicaCountOfServiceType(ServiceType serviceType) {
        switch (serviceType) {
            case EXECUTOR:
                return getReplicasOfService(getExecutorService());
            case LISTENER:
                return getReplicasOfService(getListenerService());
            default:
                LOG.warn("Unknown service type: {}. Returning 0.", serviceType);
                return 0;
        }
    }

    public void waitExecutorScaleToFinish() throws WaitForScaleTimeoutException {
        waitScaleToFinish(getExecutorService());
    }

    public void waitListenerScaleToFinish() throws WaitForScaleTimeoutException {
        waitScaleToFinish(getListenerService());
    }
}
