package com.microservices.saga.choreography.supervisor.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FailExecutionInfo implements Serializable {
    @Id
    @GeneratedValue
    private Long id;

    private KafkaFailExecutionInfo kafkaFailExecutionInfo;
}
