package com.microservices.saga.choreography.supervisor.service;

import com.microservices.saga.choreography.supervisor.domain.Event;
import com.microservices.saga.choreography.supervisor.service.compensation.CompensationService;
import com.microservices.saga.choreography.supervisor.components.SagaMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
public class EventHandler {
    @Autowired
    private SagaMetrics sagaMetrics;

    private final CompensationService compensationService;
    private final ScheduleService scheduleService;
    private final GraphService graphService;

    @Transactional
    public void handle(Event event) { //TODO add scheduling
        log.info("Polled message event name {}", event.getEventName());
        if (!graphService.isEventSuccessful(event)) {
            compensationService.startCompensation(event.getSagaInstanceId());
            sagaMetrics.countSagaInstanceCompensated(event.getSagaName());
        }
        graphService.handleSagaInstanceEvent(event);
    }
}
