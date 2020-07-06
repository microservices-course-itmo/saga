package com.microservices.saga.choreography.supervisor.service;

import com.microservices.saga.choreography.supervisor.annotations.InjectEventLogger;
import com.microservices.saga.choreography.supervisor.components.SagaMetrics;
import com.microservices.saga.choreography.supervisor.domain.entity.SagaStepDefinition;
import com.microservices.saga.choreography.supervisor.domain.entity.SagaStepDefinitionTransitionEvent;
import com.microservices.saga.choreography.supervisor.dto.definition.SagaStepDefinitionDto;
import com.microservices.saga.choreography.supervisor.exception.StepDefinitionNotFoundException;
import com.microservices.saga.choreography.supervisor.logging.EventLogger;
import com.microservices.saga.choreography.supervisor.logging.Events;
import com.microservices.saga.choreography.supervisor.repository.SagaStepDefinitionRepository;
import com.microservices.saga.choreography.supervisor.repository.SagaStepDefinitionTransitionEventRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Service that responsible for handling {@link SagaStepDefinition} and {@link SagaStepDefinitionTransitionEvent} info
 */
@Service
@RequiredArgsConstructor
public class DefinitionService {
    @Autowired
    private SagaMetrics sagaMetrics;

    /**
     * Step definition repo
     */
    private final SagaStepDefinitionRepository stepDefinitionRepository;

    /**
     * Step definition transition event repo
     */
    private final SagaStepDefinitionTransitionEventRepository transitionEventRepository;

    /**
     * Model mapper
     */
    private final ModelMapper mapper;

    @InjectEventLogger
    EventLogger logger;

    /**
     * Saving step definition in database
     *
     * @param stepDefinitionDto dto of step definition
     * @return saved {@link SagaStepDefinition}
     */
    public SagaStepDefinition addDefinition(SagaStepDefinitionDto stepDefinitionDto) {
        SagaStepDefinition stepDefinition = mapper.map(stepDefinitionDto, SagaStepDefinition.class);
        @NonNull List<String> previousSteps = stepDefinitionDto.getPreviousSteps();

        // If it's the start of Saga template
        // Update total number of templates
        if (previousSteps.isEmpty()) {
            sagaMetrics.countSagaTemplate(stepDefinition.getSagaName());
        }

        saveTransitionEvent(stepDefinition, previousSteps);
        return stepDefinitionRepository.save(stepDefinition);
    }

    /**
     * Updates step definition
     *
     * @param definitionId      needed for find {@link SagaStepDefinition} to update
     * @param stepDefinitionDto contains update info
     * @return updated {@link SagaStepDefinition}
     * @throws StepDefinitionNotFoundException if can not find {@link SagaStepDefinition} with required definitionId
     */
    public SagaStepDefinition updateDefinition(Long definitionId, SagaStepDefinitionDto stepDefinitionDto)
            throws StepDefinitionNotFoundException {
        SagaStepDefinition updatedDefinition = mapper.map(stepDefinitionDto, SagaStepDefinition.class);

        SagaStepDefinition foundDefinition = stepDefinitionRepository.findById(definitionId)
                .orElseThrow(() -> new StepDefinitionNotFoundException("Can't find definition with definitionId {}", definitionId));

        foundDefinition.update(updatedDefinition);

        logger.info(Events.I_UPDATE_STEP_DEFINITION,
                updatedDefinition.getId(),
                updatedDefinition.getSagaName(),
                updatedDefinition.getStepName(),
                updatedDefinition.getSuccessExecutionInfo(),
                updatedDefinition.getFailExecutionInfo(),
                updatedDefinition.getTimeout()
        );

        return stepDefinitionRepository.save(foundDefinition);
    }

    /**
     * Deleting {@link SagaStepDefinition}
     *
     * @param definitionId needed to find {@link SagaStepDefinition}
     */
    public void deleteDefinition(Long definitionId) {
        stepDefinitionRepository.findById(definitionId)
                .ifPresent(sagaStepDefinition -> {
                    List<SagaStepDefinitionTransitionEvent> allEventsWithDefinition = getAllEventsWithDefinition(sagaStepDefinition);
                    deleteTransitions(allEventsWithDefinition);
                });
        logger.info(Events.I_DELETE_STEP_DEFINITION, definitionId);
    }

    /**
     * Returns end nodes of the saga
     *
     * @param sagaName - name of the saga
     * @return list of end nodes
     */
    public List<SagaStepDefinition> getEndNodesOfSaga(String sagaName) {
        return stepDefinitionRepository.findEndNodesBySagaName(sagaName);
    }

    /**
     * Returns incoming steps
     *
     * @param stepDefinition step to find incoming
     * @return list of incoming steps
     */
    public List<SagaStepDefinition> getIncomingSteps(SagaStepDefinition stepDefinition) {
        List<SagaStepDefinitionTransitionEvent> incomingTransitions = transitionEventRepository
                .findSagaStepDefinitionTransitionEventsBySagaName(stepDefinition.getSagaName()).stream()
                .filter(event -> event.getNextStep().equals(stepDefinition))
                .collect(toList());

        return incomingTransitions.stream()
                .filter(sagaStepDefinitionTransitionEvent -> sagaStepDefinitionTransitionEvent.getPreviousStep() != null)
                .map(SagaStepDefinitionTransitionEvent::getPreviousStep)
                .collect(toList());
    }

    /**
     * Getting all events that interacts with definition
     *
     * @param definition object to find all related {@link SagaStepDefinitionTransitionEvent}
     * @return list of all related {@link SagaStepDefinitionTransitionEvent}
     */
    private List<SagaStepDefinitionTransitionEvent> getAllEventsWithDefinition(SagaStepDefinition definition) {
        List<SagaStepDefinitionTransitionEvent> previousTransitions = transitionEventRepository
                .findSagaStepDefinitionTransitionEventsByNextStep(definition);
        List<SagaStepDefinitionTransitionEvent> nextTransitions = transitionEventRepository
                .findSagaStepDefinitionTransitionEventsByPreviousStep(definition);

        return Stream.concat(previousTransitions.stream(), nextTransitions.stream()).collect(toList());
    }

    /**
     * Deleting transition events from repo
     *
     * @param transitionEvents list of events that need to delete
     */
    private void deleteTransitions(@NonNull List<SagaStepDefinitionTransitionEvent> transitionEvents) {
        transitionEvents.forEach(transitionEventRepository::delete);
    }

    /**
     * Saving transition event
     *
     * @param stepDefinition step definition of event
     * @param previousSteps  previous steps of event
     */
    private void saveTransitionEvent(SagaStepDefinition stepDefinition, List<String> previousSteps) {
        for (String previousStepName : previousSteps) {
            SagaStepDefinition previousStepDefinition = stepDefinitionRepository
                    .findSagaStepDefinitionBySagaNameAndStepName(stepDefinition.getSagaName(), previousStepName);

            SagaStepDefinitionTransitionEvent definitionTransitionEvent = SagaStepDefinitionTransitionEvent.builder()
                    .sagaName(stepDefinition.getSagaName())
                    .eventName(previousStepDefinition.getSuccessExecutionInfo().getKafkaSuccessExecutionInfo().getEventType())
                    .failedEventName(previousStepDefinition.getFailExecutionInfo().getKafkaFailExecutionInfo().getEventType())
                    .creationTime(ZonedDateTime.now().toInstant().toEpochMilli())
                    .previousStep(previousStepDefinition)
                    .nextStep(stepDefinition)
                    .build();
            transitionEventRepository.save(definitionTransitionEvent);

            logger.info(Events.I_SAVE_TRANSITION_EVENT,
                    definitionTransitionEvent.getEventName(),
                    definitionTransitionEvent.getEventId(),
                    definitionTransitionEvent.getNextStep(),
                    definitionTransitionEvent.getPreviousStep(),
                    definitionTransitionEvent.getCreationTime()
            );
        }
    }
}
