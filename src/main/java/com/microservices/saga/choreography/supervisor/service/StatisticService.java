package com.microservices.saga.choreography.supervisor.service;

import com.microservices.saga.choreography.supervisor.domain.StepStatus;
import com.microservices.saga.choreography.supervisor.domain.entity.SagaStepInstance;
import com.microservices.saga.choreography.supervisor.dto.measure.SagaInstanceStats;
import com.microservices.saga.choreography.supervisor.dto.measure.SagaStats;
import com.microservices.saga.choreography.supervisor.exception.FormattedRuntimeException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Comparator.comparingLong;

@Service
public class StatisticService {
    public SagaStats getStatisticForSaga(Map<Long, SagaInstanceStats> statsForInstances) {
        Optional<ImmutablePair<Long, Long>> mostTimeConsuming = statsForInstances.entrySet().stream()
                .max(comparingLong(e -> e.getValue().getTotalExecutionTime()))
                .map(max -> ImmutablePair.of(max.getKey(), max.getValue().getTotalExecutionTime()));

        double averageTime = statsForInstances.values().stream()
                .mapToLong(SagaInstanceStats::getTotalExecutionTime)
                .average().orElse(0.);

        return SagaStats.builder()
                .instanceStatistic(statsForInstances)
                .mostTimeConsumingSaga(mostTimeConsuming.isEmpty() ? null : mostTimeConsuming.get())
                .averageTimeConsumingSaga((long) averageTime)
                .build();
    }

    public SagaInstanceStats getStatisticForSteps(List<SagaStepInstance> sagaSteps) {
        if (sagaSteps.isEmpty()) {
            throw new FormattedRuntimeException("Step list to analyze cannot be empty");
        }
        long totalTime = 0;
        ImmutablePair<Long, Long> maxTimeConsumingStep = ImmutablePair.of(-1L, Long.MIN_VALUE);
        int numberOfFailedSteps = 0;
        int numberOfSucceedSteps = 0;
        long startTime = Long.MAX_VALUE;
        long endTime = Long.MIN_VALUE;
        for (SagaStepInstance step : sagaSteps) {
            if (step.getStepStatus().equals(StepStatus.FAILED.name())) {
                numberOfFailedSteps++;
            }
            if (step.getStepStatus().equals(StepStatus.SUCCESSFUL.name())) {
                numberOfSucceedSteps++;
            }
            if (Objects.nonNull(step.getStartTime()) && Objects.nonNull(step.getEndTime())) {
                long executionTime = step.getEndTime() - step.getStartTime();
                totalTime += executionTime;
                if (executionTime > maxTimeConsumingStep.getRight()) {
                    maxTimeConsumingStep = ImmutablePair.of(step.getSagaStepDefinitionId(), executionTime);
                }
                if (step.getStartTime() < startTime) {
                    startTime = step.getStartTime();
                }
                if (step.getEndTime() > endTime) {
                    endTime = step.getEndTime();
                }
            }
        }

        return SagaInstanceStats.builder()
                .averageStepExecutionTime(totalTime / sagaSteps.size())
                .maxStepExecutionTime(maxTimeConsumingStep.getLeft() > 0 ? maxTimeConsumingStep : null)
                .totalExecutionTime(endTime == Long.MIN_VALUE ? null : endTime - startTime)
                .numberOfSteps(sagaSteps.size())
                .numberOfSucceedSteps(numberOfSucceedSteps)
                .numberOfFailedSteps(numberOfFailedSteps)
                .build();
    }
}
