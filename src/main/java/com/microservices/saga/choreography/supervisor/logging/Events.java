package com.microservices.saga.choreography.supervisor.logging;

public enum Events {
    // TODO: increase amount of available events
    DEFAULT("Default event"),
    EXCEPTION("Some exception occurred");

    private final String template;

    Events(String template) {
        this.template = template;
    }

    public String getTemplate() {
        return template;
    }
}
