package com.microservices.saga.choreography.supervisor.kafka;

import com.microservices.saga.choreography.supervisor.components.SagaMetrics;
import com.microservices.saga.choreography.supervisor.domain.Event;
import com.microservices.saga.choreography.supervisor.domain.entity.SagaStepDefinition;
import com.microservices.saga.choreography.supervisor.exception.KafkaRuntimeException;
import com.microservices.saga.choreography.supervisor.service.EventHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Service that responsible for interacting with kafka
 */
@Service
@Slf4j
public class KafkaClient {
    /**
     * Kafka headers keys constants
     */

    private static final String EVENT_TYPE_KEY = "event-type";
    private static final String EVENT_ID_KEY = "event-id";
    private static final String SAGA_NAME_KEY = "saga-name";
    private static final String SAGA_ID_KEY = "saga-id";

    /**
     * is listening closed flag
     */
    private final AtomicBoolean isListeningClosed;

    /**
     * Message handler
     */
    private final EventHandler eventHandler;

    /**
     * Kafka consumer
     */
    private final KafkaConsumer<String, String> consumer;

    /**
     * Set of listening topics
     */
    private final Set<String> listeningTopics;

    @Autowired
    private SagaMetrics sagaMetrics;

    /**
     * Constructor
     */
    public KafkaClient(EventHandler eventHandler,
                       KafkaConsumer<String, String> kafkaConsumer) {
        this.listeningTopics = new HashSet<>();
        this.isListeningClosed = new AtomicBoolean(true);
        this.eventHandler = eventHandler;
        this.consumer = kafkaConsumer;
    }

    /**
     * Subscribe on topics to listen
     *
     * @param topicNames regex describing the naming rule for topics to subscribe to listen
     */
    public void subscribe(List<String> topicNames) {
        List<String> newTopics = topicNames.stream()
                .filter(topic -> !listeningTopics.contains(topic))
                .collect(Collectors.toList());
        if (!newTopics.isEmpty()) {
            if (isListeningClosed.get()) {
                startListeningTopics();
            }
            listeningTopics.addAll(newTopics);
            log.debug("Topics are subscribed: {}", newTopics);
        }
    }

    /**
     * Subscribe on on participant's topics
     *
     * @param stepDefinition saga participant
     */
    public void subscribeOnStepDefinition(SagaStepDefinition stepDefinition) {
        String successTopic = stepDefinition.getSuccessExecutionInfo().getKafkaSuccessExecutionInfo().getTopicName();
        String failTopic = stepDefinition.getFailExecutionInfo().getKafkaFailExecutionInfo().getTopicName();
        subscribe(Arrays.asList(successTopic, failTopic));
        log.info("Success topics are {}, Fail topics are {} from {} step",
                successTopic, failTopic, stepDefinition.getStepName());
    }

    /**
     * Start of listening topics
     */
    private void startListeningTopics() {
        isListeningClosed.set(false);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                //noinspection InfiniteLoopStatement
                while (true) {
                    log.debug("Pooling messages");
                    consumer.subscribe(listeningTopics);
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
                    log.debug("Messages polled");
                    for (ConsumerRecord<String, String> record : records) {
                        try {
                            eventHandler.handle(getEventFromHeaders(record.headers())); //TODO executor
                        } catch (Exception e) {
                            log.error("Error while handling event", e);
                        } finally {
                            sagaMetrics.countCoordinatorKafkaMessagesPolled(record.topic());
                        }
                    }
                }
            } catch (WakeupException wakeupException) {
                if (!isListeningClosed.get()) {
                    log.error("Kafka Runtime exception");
                    throw new KafkaRuntimeException("Caught WakeupException, but isListeningClosed variable is false", wakeupException);
                }
            } finally {
                isListeningClosed.set(true);
                log.debug("Listening is closed");
                consumer.close();
            }
        });
        consumer.unsubscribe();
    }

    /**
     * Stop listening topics
     */
    public void stopListeningTopics() {
        isListeningClosed.set(true);
        consumer.wakeup();
    }

    /**
     * Getting event from received headers
     *
     * @param headers received headers
     * @return {@link Event}
     */
    private Event getEventFromHeaders(Headers headers) {
        Event.EventBuilder eventBuilder = Event.builder();
        for (Header header : headers) {
            switch (header.key()) {
                case EVENT_TYPE_KEY:
                    eventBuilder.eventName(new String(header.value()));
                    log.debug("Event type is {}", eventBuilder.eventName(header.value().toString()));
                    break;
                case EVENT_ID_KEY:
                    eventBuilder.eventId(ByteBuffer.wrap(header.value()).getLong());
                    log.debug("Event id is {}", eventBuilder.eventId(ByteBuffer.wrap(header.value()).getLong()));
                    break;
                case SAGA_NAME_KEY:
                    eventBuilder.sagaName(new String(header.value()));
                    log.debug("Saga name is {}", eventBuilder.sagaName(header.value().toString()));
                    break;
                case SAGA_ID_KEY:
                    eventBuilder.sagaInstanceId(ByteBuffer.wrap(header.value()).getLong());
                    log.debug("Saga id is {}", eventBuilder.sagaInstanceId(ByteBuffer.wrap(header.value()).getLong()));
                    break;
            }

        }
        return eventBuilder.build(); //TODO handle null fields
    }
}
