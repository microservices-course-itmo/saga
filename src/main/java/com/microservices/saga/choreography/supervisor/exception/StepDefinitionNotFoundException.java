package com.microservices.saga.choreography.supervisor.exception;

public class StepDefinitionNotFoundException extends FormattedException {
    /**
     * Constructor that creates an exception with a parameterized message
     *
     * @param messagePattern a message with replacable params.  All {} will be replaced with the params in order they
     *                       are provided
     * @param params         list of params to substitute in message
     */
    public StepDefinitionNotFoundException(String messagePattern, Object... params) {
        super(messagePattern, params);
    }
}
