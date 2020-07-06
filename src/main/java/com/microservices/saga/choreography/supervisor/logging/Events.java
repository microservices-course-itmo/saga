package com.microservices.saga.choreography.supervisor.logging;

public enum Events {
    I_ADD_STEP_DEFINITION("New step `{}` definition with id `{}` for saga `{}` has been added"),
    I_UPDATE_STEP_DEFINITION("Definition with id `{}` has been updated sagaName = `{}` stepName = `{}` successExecutionInfo = `{}` failExecutionInfo = `{}` timeout = `{}`"),
    I_DELETE_STEP_DEFINITION("Step definition with `{}` id has been deleted"),

    I_SAVE_TRANSITION_EVENT("Transition has been saved : name = `{}` id = `{}` next step is `{}` previous step is `{}` at `{}`"),
    I_CREATE_TRANSITION_EVENT("Was created transition event with `{}` name, `{}` id in `{}` sagaName `{}` sagaID"),

    I_UPDATE_STEP_STATUS("Has been updated stepStatus: `{}` in step with `{}` name `{}` id `{}` sagaName  `{}` sagaID "),
    I_SAVE_OCCURRED_INSTANCE_STEP("Occurred instance step with `{}` name `{}` id `{}` sagaName `{}` sagaID has been saved"),
    I_SAVE_NEXT_INSTANCE_STEP("Next instance step with `{}` name `{}` id `{}` sagaName `{}` sagaID has been saved"),
    I_CREATE_NEXT_INSTANCE_STEP("Was created next step with `{}` name, `{}` id, `{}` status in `{}` sagaName `{}` sagaID"),
    I_CHANGE_CURRENT_INSTANCE_STEP("Current step is `{}` with `{}` id `{}` sagaName `{}` sagaID"),

    I_KAFKA_SUBSCRIBED_ON_TOPICS("Kafka consumer has subscribed on topics `{}`");
    private final String template;

    Events(String template) {
        this.template = template;
    }

    public String getTemplate() {
        return template;
    }
}
