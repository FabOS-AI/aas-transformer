package de.fhg.ipa.aas_transformer.service.management.controller;

import de.fhg.ipa.aas_transformer.model.ScaleDirection;
import de.fhg.ipa.aas_transformer.model.alertmanager.AlertMessage;
import de.fhg.ipa.aas_transformer.service.management.TransformerServiceHandler;
import de.fhg.ipa.aas_transformer.service.management.exceptions.WaitForScaleTimeoutException;
import de.fhg.ipa.aas_transformer.model.ServiceType;
import jakarta.ws.rs.QueryParam;
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

    @RequestMapping(path = "/{serviceType}/enable", method = RequestMethod.POST)
    public void enableServiceType(
            @PathVariable(name = "serviceType") ServiceType serviceType,
            @RequestParam(name = "enabled") Boolean enabled
    ) {
        transformerServiceHandler.enableServiceType(serviceType, enabled);
    }

    @RequestMapping(path = "/{serviceType}/current-desired-scale", method = RequestMethod.GET)
    public long getCurrentDesiredScaleOfServiceType(
            @PathVariable(name = "serviceType") ServiceType serviceType
    ) {
        return transformerServiceHandler.getReplicaCountOfServiceType(serviceType);
    }

    @RequestMapping(path = "/{serviceType}/current-running-tasks", method = RequestMethod.GET)
    public int getRunningTasksOfServiceType(
            @PathVariable(name = "serviceType") ServiceType serviceType
    ) {
        return transformerServiceHandler.getRunningServiceTasksOfServiceType(serviceType).size();
    }

    @RequestMapping(path = "/{serviceType}/scale", method = RequestMethod.POST)
    public void scaleServiceType(
            @PathVariable(name = "serviceType") ServiceType serviceType,
            @RequestParam(name = "replicas") long replicas,
            @RequestParam(name = "wait", defaultValue = "false") boolean wait
    ) throws WaitForScaleTimeoutException {
        transformerServiceHandler.scaleServiceType(serviceType, replicas, wait);
    }

    @RequestMapping(path = "/{serviceType}/scale-by-one", method = RequestMethod.POST)
    public void scaleServiceTypeByOne(
            @PathVariable(name = "serviceType") ServiceType serviceType,
            @RequestParam(name = "scaleDirection") ScaleDirection scaleDirection,
            @RequestParam(name = "wait", defaultValue = "false") boolean wait
    ) throws WaitForScaleTimeoutException {
        transformerServiceHandler.scaleServiceTypeByOne(serviceType, scaleDirection, wait);
    }
}
