package com.microservices.saga.choreography.supervisor.service;

import com.microservices.saga.choreography.supervisor.domain.Event;
import com.microservices.saga.choreography.supervisor.domain.Message;
import com.microservices.saga.choreography.supervisor.domain.StepStatus;
import com.microservices.saga.choreography.supervisor.domain.entity.KafkaCompensation;
import com.microservices.saga.choreography.supervisor.domain.entity.SagaStepDefinition;
import com.microservices.saga.choreography.supervisor.domain.entity.SagaStepInstance;
import com.microservices.saga.choreography.supervisor.dto.definition.SagaStepDefinitionDto;
import com.microservices.saga.choreography.supervisor.exception.StepDefinitionNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class GraphService {
    /**
     * Kafka headers keys constants
     */
    private static final String EVENT_TYPE_KEY = "event-type";
    private static final String SAGA_ID_KEY = "saga-id";

    private final DefinitionService definitionService;
    private final InstanceService instanceService;

    public SagaStepDefinition addDefinition(SagaStepDefinitionDto definitionDto) {
        return definitionService.addDefinition(definitionDto);
    }

    public SagaStepDefinition updateDefinition(Long definitionId, SagaStepDefinitionDto definitionDto) {
        try {
            return definitionService.updateDefinition(definitionId, definitionDto);
        } catch (StepDefinitionNotFoundException e) {
            log.error("Can't found definition {} for update", definitionId);
        }
        return new SagaStepDefinition();
    }

    public void deleteDefinition(Long definitionId) {
        definitionService.deleteDefinition(definitionId);
    }

    public void handleSagaInstanceEvent(Event event) {
        instanceService.handleEvent(event);
    }

    public boolean isEventSuccessful(Event event) {
        return instanceService.isEventSuccessful(event);
    }

    public Queue<Message> getMessagesQueueToCompensation(Long sagaId) {
        String sagaName = instanceService.getSagaNameBySagaId(sagaId);
        List<SagaStepDefinition> endNodes = definitionService.getEndNodesOfSaga(sagaName);

        return searchByBreadth(endNodes).stream()
                .map(stepDefinition -> getMessageToNode(sagaId, stepDefinition))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    public SagaStepInstance getSagaStepInstance(SagaStepDefinition stepDefinition, Long sagaId) {
        return instanceService.getSagaStepInstance(sagaId, stepDefinition.getStepName());
    }

    public void updateStepStatus(SagaStepInstance stepInstance, StepStatus status) {
        instanceService.updateStepStatus(stepInstance, status);
    }

    private Message getMessageToNode(Long sagaId, SagaStepDefinition stepDefinition) {
        Headers headers = getHeadersToNode(sagaId, stepDefinition);
        KafkaCompensation kafkaCompensation = stepDefinition.getCompensationInfo().getKafkaCompensation();
        return new Message(headers, kafkaCompensation.getTopicName(),
                String.format("COMPENSATION STEP %s", stepDefinition.getStepName()), stepDefinition);
    }

    private Headers getHeadersToNode(Long sagaId, SagaStepDefinition stepDefinition) {
        Headers headers = new RecordHeaders();
        KafkaCompensation kafkaCompensation = stepDefinition.getCompensationInfo().getKafkaCompensation();
        headers.add(EVENT_TYPE_KEY, kafkaCompensation.getEventType().getBytes());
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(sagaId);
        headers.add(SAGA_ID_KEY, buffer.array());
        return headers;
    }

    private Queue<SagaStepDefinition> searchByBreadth(List<SagaStepDefinition> endNodes) {
        Queue<SagaStepDefinition> visitedNodes = new LinkedList<>();
        Queue<SagaStepDefinition> unvisitedNodes = new LinkedList<>(endNodes);

        while (!unvisitedNodes.isEmpty()) {
            Queue<SagaStepDefinition> newNodes = unvisitedNodes.stream()
                    .flatMap(node -> definitionService.getIncomingSteps(node).stream())
                    .filter(node -> !visitedNodes.contains(node))
                    .collect(Collectors.toCollection(LinkedList::new));

            visitedNodes.addAll(unvisitedNodes);
            unvisitedNodes = newNodes;
        }
        return visitedNodes;
    }
}
