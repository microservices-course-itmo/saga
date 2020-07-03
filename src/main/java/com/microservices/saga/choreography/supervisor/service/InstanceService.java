package com.microservices.saga.choreography.supervisor.service;

import com.microservices.saga.choreography.supervisor.domain.Event;
import com.microservices.saga.choreography.supervisor.domain.StepStatus;
import com.microservices.saga.choreography.supervisor.domain.entity.SagaStepDefinition;
import com.microservices.saga.choreography.supervisor.domain.entity.SagaStepDefinitionTransitionEvent;
import com.microservices.saga.choreography.supervisor.domain.entity.SagaStepInstance;
import com.microservices.saga.choreography.supervisor.domain.entity.SagaStepInstanceTransitionEvent;
import com.microservices.saga.choreography.supervisor.repository.SagaStepDefinitionTransitionEventRepository;
import com.microservices.saga.choreography.supervisor.repository.SagaStepInstanceRepository;
import com.microservices.saga.choreography.supervisor.repository.SagaStepInstanceTransitionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Service that responsible for handling new instance transition events
 */
@Service
@AllArgsConstructor
public class InstanceService {
    private final SagaStepDefinitionTransitionEventRepository eventDefinitionRepository;
    final SagaStepInstanceRepository sagaStepInstanceRepository;
    private final SagaStepInstanceTransitionRepository eventInstanceRepository;

    /**
     * Handling new event
     *
     * @param event event
     */
    public void handleEvent(Event event) {
        if (sagaStepInstanceRepository.findSagaStepInstancesBySagaInstanceId(event.getSagaInstanceId()).stream()
                .anyMatch(sagaStepInstance -> sagaStepInstance.getStepName().equals(event.getEventName()))) {
            return;
        }
        SagaStepDefinitionTransitionEvent eventDefinition = getEventDefinition(event);

        SagaStepDefinition stepDefinition = eventDefinition.getPreviousStep();
        Long sagaStartTime = eventDefinition.getCreationTime();
        SagaStepInstance occurredStep = getOccurredSagaStepInstanceWithSuccessfulStatus(event, stepDefinition, sagaStartTime);

        if (eventDefinition.getNextStep() != null) {
            SagaStepInstance nextStep = SagaStepInstance.builder()
                    .stepStatus(StepStatus.AWAITING.name())
                    .sagaInstanceId(event.getSagaInstanceId())
                    .stepName(eventDefinition.getNextStep().getStepName())
                    .sagaName(event.getSagaName())
                    .sagaStepDefinitionId(eventDefinition.getNextStep().getId())
                    .startTime(ZonedDateTime.now().toInstant().toEpochMilli())
                    .build();

            SagaStepInstanceTransitionEvent transitionEvent = SagaStepInstanceTransitionEvent.builder()
                    .eventId(event.getEventId())
                    .eventName(event.getEventName())
                    .sagaInstanceId(event.getSagaInstanceId())
                    .sagaName(event.getSagaName())
                    .creationTime(ZonedDateTime.now().toInstant().toEpochMilli())
                    .build();
            transitionEvent.setNextStep(nextStep);
            transitionEvent.setPreviousStep(occurredStep);

            saveStepsInRepository(occurredStep, nextStep);
            eventInstanceRepository.save(transitionEvent);
        }
    }

    public boolean isEventSuccessful(Event event) {
        return eventDefinitionRepository.findSagaStepDefinitionTransitionEventBySagaNameAndFailedEventName(event.getSagaName(),
                event.getEventName()) == null;
    }

    public String getSagaNameBySagaId(Long sagaId) {
        return sagaStepInstanceRepository.findSagaStepInstancesBySagaInstanceId(sagaId).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Can't find saga with id %d", sagaId)))
                .getSagaName();
    }

    public void updateStepStatus(SagaStepInstance stepInstance, StepStatus status) {
        stepInstance.setStepStatus(status.name());
    }

    public SagaStepInstance getSagaStepInstance(Long sagaId, String stepName) {
        return sagaStepInstanceRepository.findSagaStepInstanceBySagaInstanceIdAndStepName(sagaId, stepName)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Can't find step with name %s and sagaId %d",
                        stepName, sagaId)));
    }

    private SagaStepInstance getOccurredSagaStepInstanceWithSuccessfulStatus(Event event, SagaStepDefinition occurredStepDefinition,
                                                                             Long sagaStartTime) {
        List<SagaStepInstance> instances = sagaStepInstanceRepository.findSagaStepInstancesBySagaInstanceId(event.getSagaInstanceId());

        SagaStepInstance occurredStep;
        occurredStep = instances
                .stream()
                .filter(sagaStepInstance -> sagaStepInstance.getStepName().equals(occurredStepDefinition.getStepName()))
                .findFirst()
                .orElseGet(() -> SagaStepInstance.builder()
                        .stepStatus(StepStatus.AWAITING.name())
                        .sagaInstanceId(event.getSagaInstanceId())
                        .stepName(occurredStepDefinition.getStepName())
                        .sagaName(event.getSagaName())
                        .sagaStepDefinitionId(occurredStepDefinition.getId())
                        .startTime(sagaStartTime)
                        .build())
                .toBuilder()
                .endTime(ZonedDateTime.now().toInstant().toEpochMilli())
                .build();

        StepStatus status = isEventSuccessful(event) ? StepStatus.SUCCESSFUL : StepStatus.FAILED;
        updateStepStatus(occurredStep, status);
        return occurredStep;
    }

    private void saveStepsInRepository(SagaStepInstance occurredStep, SagaStepInstance nextStep) {
        sagaStepInstanceRepository.save(occurredStep);
        sagaStepInstanceRepository.save(nextStep);
    }

    private SagaStepDefinitionTransitionEvent getEventDefinition(Event event) {
        if (isEventSuccessful(event)) {
            return eventDefinitionRepository
                    .findSagaStepDefinitionTransitionEventBySagaNameAndEventName(event.getSagaName(), event.getEventName());
        }
        return eventDefinitionRepository.findSagaStepDefinitionTransitionEventBySagaNameAndFailedEventName(event.getSagaName(),
                event.getEventName());
    }
}
