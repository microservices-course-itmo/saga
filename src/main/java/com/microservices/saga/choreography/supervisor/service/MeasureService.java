package com.microservices.saga.choreography.supervisor.service;

import com.microservices.saga.choreography.supervisor.domain.entity.SagaStepInstance;
import com.microservices.saga.choreography.supervisor.dto.measure.SagaInstanceStats;
import com.microservices.saga.choreography.supervisor.dto.measure.SagaMetrics;
import com.microservices.saga.choreography.supervisor.dto.measure.SagaStats;
import com.microservices.saga.choreography.supervisor.dto.measure.StepMetrics;
import com.microservices.saga.choreography.supervisor.exception.FormattedRuntimeException;
import com.microservices.saga.choreography.supervisor.exception.StepDefinitionNotFoundException;
import com.microservices.saga.choreography.supervisor.repository.SagaStepInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.StreamSupport;

import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.*;

@Service
@RequiredArgsConstructor
public class MeasureService {
    private final SagaStepInstanceRepository stepInstanceRepository;
    private final StatisticService statisticService;

    public List<StepMetrics> getAllStepMetrics() {
        return StreamSupport.stream(stepInstanceRepository.findAll().spliterator(), false)
                .map(this::mapStepToMetric)
                .collect(toList());
    }

    public StepMetrics getStepMetricById(Long stepId) throws StepDefinitionNotFoundException {
        return stepInstanceRepository.findById(stepId)
                .map(this::mapStepToMetric)
                .orElseThrow(() -> new StepDefinitionNotFoundException("Step with id {} not found"));
    }

    public List<SagaMetrics> getAllSagaMetrics() {
        Map<Long, List<SagaStepInstance>> sagaSteps = StreamSupport.stream(stepInstanceRepository.findAll().spliterator(), false)
                .collect(groupingBy(SagaStepInstance::getSagaInstanceId));

        return sagaSteps.entrySet().stream()
                .map(e -> mapSagaToMetric(e.getKey(), e.getValue()))
                .collect(toList());
    }

    public SagaMetrics getSagaMetricByInstance(Long sagaId) throws StepDefinitionNotFoundException {
        List<SagaStepInstance> sagsSteps = stepInstanceRepository.findSagaStepInstancesBySagaInstanceId(sagaId);
        if (sagsSteps.isEmpty()) {
            throw new StepDefinitionNotFoundException("No steps for saga with id {} found.", sagaId);
        }
        return mapSagaToMetric(sagaId, sagsSteps);
    }

    public SagaStats getStatsForSaga(String sagaName) {
        Map<Long, List<SagaStepInstance>> sagaSteps = stepInstanceRepository.findSagaStepInstanceBySagaName(sagaName).stream()
                .collect(groupingBy(SagaStepInstance::getSagaInstanceId));

        Map<Long, SagaInstanceStats> statsByInstance = sagaSteps.entrySet().stream()
                .collect(toMap(Map.Entry::getKey, e -> statisticService.getStatisticForSteps(e.getValue())));

        return statisticService.getStatisticForSaga(statsByInstance);
    }

    public SagaInstanceStats getStatsForSagaInstance(Long sagaId) throws StepDefinitionNotFoundException {
        List<SagaStepInstance> sagaSteps = stepInstanceRepository.findSagaStepInstancesBySagaInstanceId(sagaId);
        if (sagaSteps.isEmpty()) {
            throw new StepDefinitionNotFoundException("No steps for saga with id {} found", sagaId);
        }
        return statisticService.getStatisticForSteps(sagaSteps);
    }

    private StepMetrics mapStepToMetric(SagaStepInstance stepInstance) {
        return StepMetrics.builder()
                .stepId(stepInstance.getId())
                .startTime(stepInstance.getStartTime())
                .endTime(stepInstance.getEndTime())
                .status(stepInstance.getStepStatus())
                .build();
    }

    private SagaMetrics mapSagaToMetric(Long sagaId, List<SagaStepInstance> sagaStepInstances) {
        Long startTime = sagaStepInstances.stream()
                .filter(step -> Objects.nonNull(step.getStartTime()))
                .mapToLong(SagaStepInstance::getStartTime)
                .min()
                .orElseThrow(() -> new FormattedRuntimeException("Saga doesn't have start time. Saga instance: {}", sagaId));
        SagaStepInstance lastStep = sagaStepInstances.stream()
                .max(comparingLong(step -> step.getEndTime() == null ? Long.MAX_VALUE : step.getEndTime()))
                .orElseThrow(() -> new FormattedRuntimeException("Should not happen. Only if no steps"));
        return SagaMetrics.builder()
                .sagaId(sagaId)
                .startTime(startTime)
                .endTime(lastStep.getEndTime() == null ? 0 : lastStep.getEndTime())
                .status(lastStep.getStepStatus())
                .sagaName(lastStep.getSagaName())
                .stepMetrics(sagaStepInstances.stream().map(this::mapStepToMetric).collect(toList()))
                .build();
    }
}
