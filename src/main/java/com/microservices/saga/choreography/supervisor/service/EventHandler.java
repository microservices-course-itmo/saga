package com.microservices.saga.choreography.supervisor.service;

import com.microservices.saga.choreography.supervisor.domain.Event;
import com.microservices.saga.choreography.supervisor.service.compensation.CompensationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
public class EventHandler {
    private final CompensationService compensationService;
    private final ScheduleService scheduleService;
    private final GraphService graphService;

    private static final Logger logger = LoggerFactory.getLogger(
            EventHandler.class);

    @Transactional
    public void handle(Event event) { //TODO add scheduling
        logger.info("Polled message event name {}", event.getEventName());
        if (!graphService.isEventSuccessful(event)) {
            compensationService.compensate(event.getSagaInstanceId());
        }
        graphService.handleSagaInstanceEvent(event);
    }
}
