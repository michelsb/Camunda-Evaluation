package org.camunda.community.benchmarks;

import org.camunda.community.benchmarks.StatisticsCollector;
import org.camunda.community.benchmarks.exceptions.CommandWrapper;
import org.camunda.community.benchmarks.exceptions.DefaultCommandExceptionHandlingStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledExecutorService;

@Primary
@Component
public class BenchmarkCompleteJobExceptionHandlingStrategy extends DefaultCommandExceptionHandlingStrategy {

    @Autowired
    private StatisticsCollector stats;

    public BenchmarkCompleteJobExceptionHandlingStrategy(@Autowired ScheduledExecutorService scheduledExecutorService) {
        super(scheduledExecutorService);
    }

    public void handleCommandError(CommandWrapper command, Throwable throwable) {
        //System.out.println(throwable.getCause());
        stats.incCompletedJobsException(throwable.getMessage());
        //stats.incCompletedJobsException("NO_RESPONSE");
        // use normal behavior, e.g. increasing back-off for backpressure
        super.handleCommandError(command, throwable);
    }
}
