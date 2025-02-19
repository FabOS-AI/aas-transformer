package de.fhg.ipa.aas_transformer.service.management;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/scaling")
public class ScalingRestController {
    @Autowired
    TransformerServiceHandler transformerServiceHandler;

    @RequestMapping(path = "/executor/current-desired-scale", method = RequestMethod.GET)
    public long getExecutorCurrentDesiredScale() {
        return transformerServiceHandler.getReplicaCountOfExecutorService();
    }

    @RequestMapping(path = "/executor/current-running-tasks", method = RequestMethod.GET)
    public int getExecutorCurrentRunningTasks() {
        return transformerServiceHandler.getRunningExecutorServiceTasks().size();
    }

    @RequestMapping(path = "/executor/scale", method = RequestMethod.POST)
    public void scaleExecutorService(
            @RequestParam(name = "replicas") long replicas,
            @RequestParam(name = "wait", defaultValue = "false") boolean wait
    ) throws DockerHandler.WaitForScaleTimeoutException {
        transformerServiceHandler.scaleExecutorService(replicas, wait);
    }

    @RequestMapping(path = "/listener/current-desired-scale", method = RequestMethod.GET)
    public long getListenerCurrentDesiredScale() {
        return transformerServiceHandler.getReplicaCountOfListenerService();
    }

    @RequestMapping(path = "/listener/current-running-tasks", method = RequestMethod.GET)
    public long getListenerCurrentRunningTasks() {
        return transformerServiceHandler.getRunningListenerServiceTasks().size();
    }

    @RequestMapping(path = "/listener/scale", method = RequestMethod.POST)
    public void scaleListenerService(
            @RequestParam(name = "replicas") long replicas,
            @RequestParam(name = "wait", defaultValue = "false") boolean wait
    ) throws DockerHandler.WaitForScaleTimeoutException {
        transformerServiceHandler.scaleListenerService(replicas, wait);
    }

}
