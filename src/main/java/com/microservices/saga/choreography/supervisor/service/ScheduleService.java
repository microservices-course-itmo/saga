package com.microservices.saga.choreography.supervisor.service;

import com.microservices.saga.choreography.supervisor.service.compensation.CompensationService;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ScheduleService {
    private final Map<ScheduleKey, AtomicBoolean> scheduleMap;
    private final ScheduledExecutorService executorService;
    private final CompensationService compensationService;

    public ScheduleService(CompensationService compensationService) {
        this.scheduleMap = new ConcurrentHashMap<>();
        this.executorService = Executors.newScheduledThreadPool(5);
        this.compensationService = compensationService;
    }

    public void scheduleCompensation(String eventName, Long sagaId, Long awaitTime) {
        ScheduleKey scheduleKey = new ScheduleKey(eventName, sagaId);
        scheduleMap.put(scheduleKey, new AtomicBoolean(false));
        final Runnable compensation = () -> {
            if (scheduleMap.get(scheduleKey).get()) {
                compensationService.startCompensation(sagaId);
            }
        };
        executorService.schedule(compensation, awaitTime, TimeUnit.SECONDS);
    }

    @AllArgsConstructor
    @EqualsAndHashCode
    private static class ScheduleKey {
        private final String eventName;
        private final Long sagaId;
    }

}
