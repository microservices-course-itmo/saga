package com.microservices.saga.choreography.supervisor.logging;

import com.microservices.saga.choreography.supervisor.components.SagaMetrics;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;

/**
 * This is specialized version of logback logger.
 * Every time we log specific {@link Events}
 * we record metric called @{code events_total}
 * with event type as label derived from name of event.
 *
 * <p>
 * Example of instance declaration
 * <pre>{@code
 * class SomeClass {
 *   @InjectEventLogger
 *   KPIEventsLogger logger;
 * }
 * }</pre>
 *
 * <p>
 * Whenever you want to log an event
 * call it as you would typically do with logback
 * {@link EventLogger#trace}
 * {@link EventLogger#debug}
 * {@link EventLogger#info}
 * {@link EventLogger#warn}
 * {@link EventLogger#error}
 */
@AllArgsConstructor
public class EventLogger {
    private final Logger log;
    private final SagaMetrics metrics;

    public void trace(Events event, Object... payload) {
        log.info(event.getTemplate(), payload);
        metrics.countEvent(event);
    }

    public void debug(Events event, Object... payload) {
        log.debug(event.getTemplate(), payload);
        metrics.countEvent(event);
    }

    public void info(Events event, Object... payload) {
        log.info(event.getTemplate(), payload);
        metrics.countEvent(event);
    }

    public void warn(Events event, Object... payload) {
        log.warn(event.getTemplate(), payload);
        metrics.countEvent(event);
    }

    public void error(Events event, Object... payload) {
        log.error(event.getTemplate(), payload);
        metrics.countEvent(event);
    }
}
