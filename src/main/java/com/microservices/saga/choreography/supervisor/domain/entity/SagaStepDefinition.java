package com.microservices.saga.choreography.supervisor.domain.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
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
        this.stepName = stepDefinition.getStepName();
        this.successExecutionInfo = stepDefinition.getSuccessExecutionInfo();
        this.failExecutionInfo = stepDefinition.getFailExecutionInfo();
        this.timeout = stepDefinition.getTimeout();
    }
}


