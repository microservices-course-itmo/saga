package com.microservices.saga.choreography.supervisor.domain.entity;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.internal.shaded.reactor.util.annotation.NonNull;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

import java.io.Serializable;

@NodeEntity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@Slf4j
public class SagaStepDefinition implements Serializable {
    @Id
    @GeneratedValue
    private Long id;

    @NonNull
    private String sagaName;

    @NonNull
    private String stepName;

    private SuccessExecutionInfo successExecutionInfo;

    private FailExecutionInfo failExecutionInfo;

    private CompensationInfo compensationInfo;

    private Long timeout;

    public void update(SagaStepDefinition stepDefinition) {
        this.sagaName = stepDefinition.getSagaName();
        log.debug("Saga name is updated {}", this.sagaName);
        this.stepName = stepDefinition.getStepName();
        log.debug("Step name is updated {}", this.stepName);
        this.successExecutionInfo = stepDefinition.getSuccessExecutionInfo();
        log.debug("Success execution info {}", this.successExecutionInfo);
        this.failExecutionInfo = stepDefinition.getFailExecutionInfo();
        log.debug("Fail execution info {}", this.failExecutionInfo);
        this.timeout = stepDefinition.getTimeout();
        log.debug("Timeout started {}", this.timeout);
    }
}


