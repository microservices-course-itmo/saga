package com.microservices.saga.choreography.supervisor.controller;

import com.microservices.saga.choreography.supervisor.kafka.KafkaClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

@RestController
@RequiredArgsConstructor
@RequestMapping("/topics")
public class KafkaController {
    private final KafkaClient kafkaClient;

    @PostMapping(value = "/subscribe/{topicName}")
    public void subscribe(@PathVariable String topicName) {
        kafkaClient.subscribe(Collections.singletonList(topicName));
    }
}
