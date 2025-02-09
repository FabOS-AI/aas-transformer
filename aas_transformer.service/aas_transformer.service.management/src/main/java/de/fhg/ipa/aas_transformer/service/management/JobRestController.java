package de.fhg.ipa.aas_transformer.service.management;

import de.fhg.ipa.aas_transformer.clients.redis.RedisJobReader;
import de.fhg.ipa.aas_transformer.model.TransformationJob;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/transformer-jobs")
public class JobRestController {
    @Autowired
    RedisJobReader redisJobReader;

    @RequestMapping(path = "/waiting", method = RequestMethod.GET)
    @Operation(summary = "Get all waiting jobs")
    public List<TransformationJob> getWaitingJobs() {
        return redisJobReader.getWaitingJobs();
    }

    @RequestMapping(path = "/in-progress", method = RequestMethod.GET)
    @Operation(summary = "Get all in-progress jobs")
    public List<TransformationJob> getInProgressJobs() {
        return redisJobReader.getInProgressJobs();
    }

    @RequestMapping(path = "/count/total", method = RequestMethod.GET)
    @Operation(summary = "Get count of all jobs")
    public int getTotalJobCount() {
        return redisJobReader.getTotalJobCount();
    }

    @RequestMapping(path = "/count/waiting", method = RequestMethod.GET)
    @Operation(summary = "Get count of waiting jobs")
    public long getWaitingJobCount() {
        return redisJobReader.getWaitingJobCount();
    }

    @RequestMapping(path = "/count/in-progress", method = RequestMethod.GET)
    @Operation(summary = "Get count of in-progress jobs")
    public long getInProgressJobCount() {
        return redisJobReader.getInProgressJobCount();
    }
}
