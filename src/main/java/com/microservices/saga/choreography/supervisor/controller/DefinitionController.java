package com.microservices.saga.choreography.supervisor.controller;

import com.microservices.saga.choreography.supervisor.domain.entity.SagaStepDefinition;
import com.microservices.saga.choreography.supervisor.dto.definition.SagaStepDefinitionDto;
import com.microservices.saga.choreography.supervisor.kafka.KafkaClient;
import com.microservices.saga.choreography.supervisor.repository.SagaStepDefinitionRepository;
import com.microservices.saga.choreography.supervisor.service.GraphService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/definition")
public class DefinitionController {
    private final GraphService graphService;
    private final KafkaClient kafkaClient;
    private final SagaStepDefinitionRepository stepDefinitionRepository;

    private static final Logger logger = LoggerFactory.getLogger(
            DefinitionController.class);

    @PostMapping(value = "", headers = {"Content-type=application/json"})
    public ResponseEntity<SagaStepDefinition> addDefinition(@RequestBody @Valid SagaStepDefinitionDto stepDefinitionDto) {
        SagaStepDefinition sagaStepDefinition = graphService.addDefinition(stepDefinitionDto);
        kafkaClient.subscribeOnStepDefinition(sagaStepDefinition);
        return ResponseEntity.ok().body(sagaStepDefinition);
    }

    @GetMapping(value = "/{id}", produces = "application/json")
    public ResponseEntity<SagaStepDefinition> getStepDefinition(@PathVariable Long id) {
        Optional<SagaStepDefinition> sagaStepDefinitionOptional = stepDefinitionRepository.findById(id, 2);
        return sagaStepDefinitionOptional.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/{definitionId}", produces = "application/json")
    public ResponseEntity<SagaStepDefinition> updateStepDefinition(@PathVariable Long definitionId,
                                                                   @RequestBody @Valid SagaStepDefinitionDto stepDefinitionDto) {
        return ResponseEntity.ok().body(graphService.updateDefinition(definitionId, stepDefinitionDto));
    }

    @DeleteMapping(value = "/{definitionId}")
    public void deleteDefinition(@PathVariable Long definitionId) {
        graphService.deleteDefinition(definitionId);
    }

    @GetMapping("/endnodes/{sagaName}")
    public List<SagaStepDefinition> findEndNodesForSaga(@PathVariable String sagaName) {
        return stepDefinitionRepository.findEndNodesBySagaName(sagaName);
    }
}
