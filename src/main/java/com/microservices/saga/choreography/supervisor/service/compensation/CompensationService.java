package com.microservices.saga.choreography.supervisor.service.compensation;

import com.microservices.saga.choreography.supervisor.components.SagaMetrics;
import com.microservices.saga.choreography.supervisor.domain.Message;
import com.microservices.saga.choreography.supervisor.domain.StepStatus;
import com.microservices.saga.choreography.supervisor.service.GraphService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Service that responsible for compensation
 */
@Service
@Slf4j
public class CompensationService {
    /**
     * Kafka producer
     */
    private final KafkaProducer<String, String> producer;

    /**
     * Graph handler
     */
    private final GraphService graphService;

    @Autowired
    private SagaMetrics sagaMetrics;

    /**
     * Compensation map
     */
    private final Set<Long> sagaCompensations;

    /**
     * Constructor
     */
    public CompensationService(GraphService graphService,
                               KafkaProducer<String, String> producer) {
        sagaCompensations = new HashSet<>();
        this.producer = producer;
        this.graphService = graphService;
    }

    /**
     * Starts compensation
     */
    public synchronized void startCompensation(Long sagaId) {
        log.info("Request for compensation. Saga {}", sagaId);
        if (!sagaCompensations.contains(sagaId)) {
            log.info("Start compensation. Saga {}", sagaId);
            sagaCompensations.add(sagaId);
            compensate(sagaId);
        } else {
            log.info("Saga already compensated. Saga {}", sagaId);
        }
    }

    /**
     * Sending compensation messages
     *
     * @param sagaId id of saga to start compensation
     */
    private void compensate(Long sagaId) {
        Queue<Message> messagesQueueToCompensation = graphService.getMessagesQueueToCompensation(sagaId);
        messagesQueueToCompensation.forEach(message -> {
            try {
                sendMessage(message.getTopic(), message.getHeaders(), message.getEventMessage());
                graphService.updateStepStatus(graphService.getSagaStepInstance(message.getStep(), sagaId), StepStatus.COMPENSATED);
            } catch (Exception e) {
                log.error("Error while sending compensation message", e);
            }
        });
    }

    /**
     * Sending messages
     */
    public void sendMessage(String topicName, Headers headers, String message) {
        log.info("COMPENSATION: Send compensation on topic {}, message {}, headers {}", topicName, message, headers);
        ProducerRecord<String, String> record = new ProducerRecord<>(topicName, message);
        headers.forEach(header -> record.headers().add(header));
        producer.send(record);
        sagaMetrics.countCoordinatorKafkaMessagesCompensationProduced(topicName);
    }
}
