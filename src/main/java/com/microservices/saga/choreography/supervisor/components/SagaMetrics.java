package com.microservices.saga.choreography.supervisor.components;

import com.microservices.saga.choreography.supervisor.logging.Events;
import io.micrometer.core.instrument.Metrics;
import io.prometheus.client.Counter;
import io.prometheus.client.Summary;
import org.springframework.stereotype.Component;

/**
 * This Class expose methods for recording specific metrics
 * It changes metrics of Micrometer and Prometheus simultaneously
 * Micrometer's metrics exposed at /actuator/prometheus
 * Prometheus' metrics exposed at /metrics-prometheus
 *
 * TODO: consider better namings for class name and name methods
 */
@Component
public class SagaMetrics {
    private static final String SAGA_NAME_LABEL = "saga_name";
    private static final String STEP_NAME_LABEL = "step_name";
    private static final String EXCEPTIONS_NAME_LABEL = "exception";
    private static final String KAFKA_TOPIC_NAME_LABEL = "topic";
    private static final String EVENTS_TYPE_LABEL = "type";

    private static final String SAGA_TEMPLATE_STEP_EXECUTED_TITLE = "saga_template_step_executed";
    private static final Summary prometheusSagaTemplateStepExecutedSummary = Summary.build()
            .name(SAGA_TEMPLATE_STEP_EXECUTED_TITLE)
            .help("Saga's step execution summary")
            .labelNames("saga_name", "step_name")
            .register();

    private static final String SAGA_TEMPLATE_TOTAL_TITLE = "saga_template_total";
    private static final Counter prometheusSagaTemplateTotalCounter = Counter.build()
            .name(SAGA_TEMPLATE_TOTAL_TITLE)
            .help("Total number of registered Saga templates")
            .labelNames(SAGA_NAME_LABEL)
            .register();

    private static final String SAGA_INSTANCE_STARTED_TITLE = "saga_instance_started_total";
    private static final Counter prometheusSagaInstanceStartedCounter = Counter.build()
            .name(SAGA_INSTANCE_STARTED_TITLE)
            .help("Number of started Saga instances by this Choreographer instance")
            .labelNames(SAGA_NAME_LABEL)
            .register();

    private static final String SAGA_INSTANCE_COMPLETED_TOTAL_TITLE = "saga_instance_completed_total";
    private static final Counter prometheusSagaInstanceCompletedCounter = Counter.build()
            .name(SAGA_INSTANCE_COMPLETED_TOTAL_TITLE)
            .help("Number of completed Saga instances by this Choreographer instance")
            .labelNames(SAGA_NAME_LABEL)
            .register();

    private static final String SAGA_INSTANCE_COMPENSATED_TOTAL_TITLE = "saga_instance_compensated_total";
    private static final Counter prometheusSagaInstanceCompensatedCounter = Counter.build()
            .name(SAGA_INSTANCE_COMPENSATED_TOTAL_TITLE)
            .help("Number of compensated Saga instances by this Choreopgraher instance")
            .labelNames(SAGA_NAME_LABEL)
            .register();

    private static final String COORDINATOR_EXCEPTIONS_THROWN = "coordinator_exception_thrown_total";
    private static final Counter prometheusCoordinatorExceptionsThrown = Counter.build()
            .name(COORDINATOR_EXCEPTIONS_THROWN)
            .help("Number of thrown exceptions")
            .labelNames(EXCEPTIONS_NAME_LABEL)
            .register();

    private static final String COORDINATOR_KAFKA_TOPIC_SUBSCRIBED = "coordinator_kafka_topic_subscribed_total";
    private static final Counter prometheusCoordinatorKafkaTopicSubscribed = Counter.build()
            .name(COORDINATOR_KAFKA_TOPIC_SUBSCRIBED)
            .help("Number of subscribption to topics")
            .labelNames(KAFKA_TOPIC_NAME_LABEL)
            .register();

    private static final String COORDINATOR_KAFKA_MESSAGES_POLLED = "coordinator_kafka_messages_polled_total";
    private static final Counter prometheusCoordinatorKafkaMessagesPolled = Counter.build()
            .name(COORDINATOR_KAFKA_MESSAGES_POLLED)
            .help("Number of polled messages by topics")
            .labelNames(KAFKA_TOPIC_NAME_LABEL)
            .register();

    private static final String COORDINATOR_KAFKA_MESSAGES_COMPENSATED_PRODUCED = "coordinator_kafka_messages_compensated_produced_total";
    private static final Counter prometheusCoordinatorKafkaMessagesCompensationProduced = Counter.build()
            .name(COORDINATOR_KAFKA_MESSAGES_COMPENSATED_PRODUCED)
            .help("Number of produced messages by topics")
            .labelNames(KAFKA_TOPIC_NAME_LABEL)
            .register();

    private static final String EVENTS = "events_total";
    private static final Counter prometheusEventsCounter = Counter.build()
            .name(EVENTS)
            .help("Amount of occurred events")
            .labelNames(EVENTS_TYPE_LABEL)
            .register();


    public void recordSagaInstanceStep(String sagaName, String stepName, double timeExecution) {
        Metrics.summary(SAGA_TEMPLATE_STEP_EXECUTED_TITLE,
                SAGA_NAME_LABEL, sagaName,
                STEP_NAME_LABEL, stepName
        ).record(timeExecution);
        prometheusSagaTemplateStepExecutedSummary.labels(sagaName, stepName).observe(timeExecution);
    }

    public void countSagaTemplate(String sagaName) {
        Metrics.counter(SAGA_TEMPLATE_TOTAL_TITLE, SAGA_NAME_LABEL, sagaName).increment();
        prometheusSagaTemplateTotalCounter.labels(sagaName).inc();
    }

    public void countSagaInstanceStarted(String sagaName) {
        Metrics.counter(SAGA_INSTANCE_STARTED_TITLE, SAGA_NAME_LABEL, sagaName).increment();
        prometheusSagaInstanceStartedCounter.labels(sagaName).inc();
    }

    public void countSagaInstanceCompleted(String sagaName) {
        Metrics.counter(SAGA_INSTANCE_COMPLETED_TOTAL_TITLE, SAGA_NAME_LABEL, sagaName).increment();
        prometheusSagaInstanceCompletedCounter.inc();
    }

    public void countSagaInstanceCompensated(String sagaName) {
        Metrics.counter(SAGA_INSTANCE_COMPENSATED_TOTAL_TITLE, SAGA_NAME_LABEL, sagaName).increment();
        prometheusSagaInstanceCompensatedCounter.labels(sagaName).inc();
    }

    public void countCoordinatorExceptionsThrown(Exception exception) {
        var exceptionName = exception.getClass().getSimpleName();
        Metrics.counter(COORDINATOR_EXCEPTIONS_THROWN, EXCEPTIONS_NAME_LABEL, exceptionName).increment();
        prometheusCoordinatorExceptionsThrown.labels(exceptionName).inc();
    }

    public void countCoordinatorKafkaSubscribedTopics(String topicName) {
        Metrics.counter(COORDINATOR_KAFKA_TOPIC_SUBSCRIBED, KAFKA_TOPIC_NAME_LABEL, topicName).increment();
        prometheusCoordinatorKafkaTopicSubscribed.labels(topicName).inc();
    }

    public void countCoordinatorKafkaMessagesPolled(String topicName) {
        Metrics.counter(COORDINATOR_KAFKA_MESSAGES_POLLED, KAFKA_TOPIC_NAME_LABEL, topicName).increment();
        prometheusCoordinatorKafkaMessagesPolled.labels(topicName).inc();
    }

    public void countCoordinatorKafkaMessagesCompensationProduced(String topicName) {
        Metrics.counter(COORDINATOR_KAFKA_MESSAGES_COMPENSATED_PRODUCED, KAFKA_TOPIC_NAME_LABEL, topicName).increment();
        prometheusCoordinatorKafkaMessagesCompensationProduced.labels(topicName).inc();
    }

    public void countEvent(Events event) {
        Metrics.counter(EVENTS, EVENTS_TYPE_LABEL, event.name()).increment();
        prometheusEventsCounter.labels(event.name()).inc();
    }
}
