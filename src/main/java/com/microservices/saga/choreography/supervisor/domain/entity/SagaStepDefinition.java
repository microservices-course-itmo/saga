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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(
            SagaStepDefinition.class);

    public void update(SagaStepDefinition stepDefinition) {
        this.sagaName = stepDefinition.getSagaName();
        this.stepName = stepDefinition.getStepName();
        this.successExecutionInfo = stepDefinition.getSuccessExecutionInfo();
        this.failExecutionInfo = stepDefinition.getFailExecutionInfo();
        this.timeout = stepDefinition.getTimeout();
        logger.debug("Saga and step definition are updated:" +
                "saga name {}, step name {}, success execution info {}" +
                "fail execution info{} timeout{}",
                this.sagaName, this.stepName, this.successExecutionInfo,
                this.failExecutionInfo, this.timeout);
    }
}


