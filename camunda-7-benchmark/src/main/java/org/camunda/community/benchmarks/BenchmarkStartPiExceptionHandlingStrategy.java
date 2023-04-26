package org.camunda.community.benchmarks;


import org.camunda.community.benchmarks.exceptions.CommandWrapper;
import org.camunda.community.benchmarks.exceptions.DefaultCommandExceptionHandlingStrategy;
import org.camunda.community.rest.client.invoker.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledExecutorService;

@Component
public class BenchmarkStartPiExceptionHandlingStrategy extends DefaultCommandExceptionHandlingStrategy {

    @Autowired
    private StatisticsCollector stats;

    public BenchmarkStartPiExceptionHandlingStrategy(@Autowired ScheduledExecutorService scheduledExecutorService) {
        super(scheduledExecutorService);
    }

    public void handleCommandError(CommandWrapper command, Throwable throwable) {
        if (ApiException.class.isAssignableFrom(throwable.getClass())) {
            ApiException exception = (ApiException) throwable;
            stats.incStartedProcessInstancesException(String.valueOf(exception.getCode()));
        } else {
            stats.incStartedProcessInstancesException(throwable.getMessage());
        }
        // use normal behavior
        super.handleCommandError(command, throwable);
    }
}
