package com.microservices.saga.choreography.supervisor.domain;

public enum StepStatus {
    FAILED,
    SUCCESSFUL,
    AWAITING,
    COMPENSATED
}
