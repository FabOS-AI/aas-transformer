package de.fhg.ipa.aas_transformer.service.job_api;

import de.fhg.ipa.aas_transformer.clients.redis.RedisJobConsumer;
import de.fhg.ipa.aas_transformer.clients.redis.RedisTransformationJob;
import de.fhg.ipa.aas_transformer.model.TransformationJob;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/jobs")
public class JobRestController {

    private final RedisJobConsumer redisJobConsumer;

    public JobRestController(RedisJobConsumer redisJobConsumer) {
        this.redisJobConsumer = redisJobConsumer;
    }

    @RequestMapping(path = "/get-next", method = RequestMethod.GET)
    @Operation(summary = "Checkout next unlocked job from waiting list")
    public Optional<TransformationJob> getNextJob() {
        Optional<TransformationJob> job = Optional.empty();
        Optional<RedisTransformationJob> redisJob = redisJobConsumer.moveJobInProcessingList();
        if (redisJob.isPresent())
            job = Optional.of(redisJob.get().getTransformationJob());
        return job;
    }

    @RequestMapping(path = "/finish", method = RequestMethod.POST)
    @Operation(summary = "Remove job from processing list and unlock it")
    public void finishJob(@RequestBody TransformationJob job) {
        redisJobConsumer.markJobAsFinished(job);
    }

}
