package de.fhg.ipa.aas_transformer.service.management.controller;

import de.fhg.ipa.aas_transformer.clients.redis.RedisJobConsumer;
import de.fhg.ipa.aas_transformer.clients.redis.RedisMessageEventConsumer;
import de.fhg.ipa.aas_transformer.model.TransformationJob;
import de.fhg.ipa.aas_transformer.model.message_event.MessageEvent;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/")
public class RedisRestController {
    static {
        io.swagger.v3.core.jackson.ModelResolver.enumsAsRef = true;
    }

    @Autowired
    RedisJobConsumer redisJobConsumer;
    @Autowired
    RedisMessageEventConsumer redisMessageEventConsumer;

    @GetMapping(path = "jobs/waiting")
    @Operation(summary = "Get all waiting jobs")
    public List<TransformationJob> getWaitingJobs() {
        return redisJobConsumer.getWaitingJobs();
    }

    @DeleteMapping(path = "jobs/waiting")
    @Operation(summary = "Delete all waiting jobs")
    public void deleteWaitingJobs() {
        redisJobConsumer.deleteWaitingJobs();
    }

    @GetMapping(path = "jobs/in-progress")
    @Operation(summary = "Get all in-progress jobs")
    public List<TransformationJob> getInProgressJobs() {
        return redisJobConsumer.getInProgressJobs();
    }

    @DeleteMapping(path = "jobs/in-progress")
    @Operation(summary = "Delete all in-progress jobs")
    public void deleteInProgressJobs() {
        redisJobConsumer.deleteProcJobs();
    }

    @GetMapping(path = "jobs/count/total")
    @Operation(summary = "Get count of all jobs")
    public int getTotalJobCount() {
        return redisJobConsumer.getTotalJobCount();
    }

    @GetMapping(path = "jobs/count/waiting")
    @Operation(summary = "Get count of waiting jobs")
    public long getWaitingJobCount() {
        return redisJobConsumer.getWaitingJobCount();
    }

    @GetMapping(path = "jobs/count/in-progress")
    @Operation(summary = "Get count of in-progress jobs")
    public long getInProgressJobCount() {
        return redisJobConsumer.getInProgressJobCount();
    }

    @GetMapping(path = "message-events")
    @Operation(summary = "Get all message events")
    public List<MessageEvent> getMessageEvents() {
        return redisMessageEventConsumer.getMessageEvents();
    }

    @DeleteMapping(path = "message-events")
    @Operation(summary = "Delete all message events")
    public void deleteMessageEvents() {
        redisMessageEventConsumer.deleteMessageEvents();
    }

    @GetMapping(path = "message-events/count")
    @Operation(summary = "Get all message events")
    public int getMessageEventsCount() {
        return redisMessageEventConsumer.getMessageEventCount();
    }
}
