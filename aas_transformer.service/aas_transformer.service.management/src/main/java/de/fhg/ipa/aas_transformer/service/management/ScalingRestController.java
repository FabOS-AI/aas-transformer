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
    DockerHandler dockerHandler;

    @RequestMapping(path = "/executor/current-scale", method = RequestMethod.GET)
    public long getExecutorCurrentScale() {
        return dockerHandler.getReplicaCountOfExecutorService();
    }

    @RequestMapping(path = "/executor/scale", method = RequestMethod.POST)
    public void scaleExecutorService(@RequestParam(name = "replicas") long replicas) {
        dockerHandler.scaleExecutorService(replicas);
    }

    @RequestMapping(path = "/listener/current-scale", method = RequestMethod.GET)
    public long getListenerCurrentScale() {
        return dockerHandler.getReplicaCountOfListenerService();
    }

    @RequestMapping(path = "/listener/scale", method = RequestMethod.POST)
    public void scaleListenerService(@RequestParam(name = "replicas") long replicas) {
        dockerHandler.scaleListenerService(replicas);
    }

}
