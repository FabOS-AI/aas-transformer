package de.fhg.ipa.aas_transformer.service.management.controller;

import de.fhg.ipa.aas_transformer.model.ScaleDirection;
import de.fhg.ipa.aas_transformer.model.alertmanager.AlertMessage;
import de.fhg.ipa.aas_transformer.service.management.TransformerServiceHandler;
import de.fhg.ipa.aas_transformer.service.management.exceptions.WaitForScaleTimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/scaling")
public class ScalingRestController {
    @Autowired
    TransformerServiceHandler transformerServiceHandler;

    @RequestMapping(path = "/alert", method = RequestMethod.POST)
    public void receiveAlert(@RequestBody AlertMessage alertMessage) throws WaitForScaleTimeoutException {
        // handle alert
        transformerServiceHandler.handleScaleAlert(alertMessage);
    }

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
    ) throws WaitForScaleTimeoutException {
        transformerServiceHandler.scaleExecutorService(replicas, wait);
    }

    @RequestMapping(path = "/executor/scale-by-one", method = RequestMethod.POST)
    public void scaleExecutorServicePlusOne(
            @RequestParam(name = "scaleDirection") ScaleDirection scaleDirection,
            @RequestParam(name = "wait", defaultValue = "false") boolean wait
    ) throws WaitForScaleTimeoutException {
        transformerServiceHandler.scaleExecutorServiceByOne(scaleDirection, wait);
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
    ) throws WaitForScaleTimeoutException {
        transformerServiceHandler.scaleListenerService(replicas, wait);
    }

    @RequestMapping(path = "/listener/scale-plus-one", method = RequestMethod.POST)
    public void scaleListenerServicePlusOne(
            @RequestParam(name = "scaleDirection") ScaleDirection scaleDirection,
            @RequestParam(name = "wait", defaultValue = "false") boolean wait
    ) throws WaitForScaleTimeoutException {
        transformerServiceHandler.scaleListenerServiceByOne(scaleDirection, wait);
    }

}
