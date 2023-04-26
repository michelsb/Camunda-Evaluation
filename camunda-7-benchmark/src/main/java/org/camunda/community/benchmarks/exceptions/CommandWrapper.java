package org.camunda.community.benchmarks.exceptions;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.community.rest.client.api.ProcessDefinitionApi;
import org.camunda.community.rest.client.dto.VariableValueDto;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CommandWrapper {

    private ExternalTaskService externalTaskService;
    private ExternalTask externalTask;
    private ProcessDefinitionApi processesDefinitionApi;
    private String bpmnProcessId;
    private Map<String, VariableValueDto> variables;
    private long deadline;
    private String entityLogInfo;
    private DefaultCommandExceptionHandlingStrategy commandExceptionHandlingStrategy;
    private long currentRetryDelay = 50L;
    private int invocationCounter = 0;
    private int maxRetries = 20;
    private long maxDelay = TimeUnit.SECONDS.toMillis(5L);
    private long minDelay = TimeUnit.MILLISECONDS.toMillis(50L);
    private double backoffFactor = 1.6;
    private double jitterFactor = 0.1;
    private Random random = new Random();

    public CommandWrapper(ExternalTaskService externalTaskService, ExternalTask externalTask, long deadline, String entityLogInfo, DefaultCommandExceptionHandlingStrategy commandExceptionHandlingStrategy) {
        this.externalTaskService = externalTaskService;
        this.externalTask = externalTask;
        this.deadline = deadline;
        this.entityLogInfo = entityLogInfo;
        this.commandExceptionHandlingStrategy = commandExceptionHandlingStrategy;
    }

    public CommandWrapper(ProcessDefinitionApi processesDefinitionApi, String bpmnProcessId, Map<String, VariableValueDto> variables,  long deadline, String entityLogInfo, DefaultCommandExceptionHandlingStrategy commandExceptionHandlingStrategy) {
        this.processesDefinitionApi = processesDefinitionApi;
        this.bpmnProcessId = bpmnProcessId;
        this.variables = variables;
        this.deadline = deadline;
        this.entityLogInfo = entityLogInfo;
        this.commandExceptionHandlingStrategy = commandExceptionHandlingStrategy;
    }

    public void executeAsync() {
        ++this.invocationCounter;
    }

    private double computeJitter(final double value) {
        double minFactor = value * -this.jitterFactor;
        double maxFactor = value * this.jitterFactor;
        return this.random.nextDouble() * (maxFactor - minFactor) + minFactor;
    }

    public void increaseBackoffUsing() {
        double delay = Math.max(Math.min((double)this.maxDelay, (double)currentRetryDelay * this.backoffFactor), (double)this.minDelay);
        double jitter = this.computeJitter(delay);
        this.currentRetryDelay = Math.round(delay + jitter);
    }

    public void scheduleExecutionUsing(ScheduledExecutorService scheduledExecutorService) {
        scheduledExecutorService.schedule(this::executeAsync, this.currentRetryDelay, TimeUnit.MILLISECONDS);
    }

    public String toString() {
        return ", entity=" + this.entityLogInfo + ", currentRetryDelay=" + this.currentRetryDelay + '}';
    }

    public boolean hasMoreRetries() {
        if (this.jobDeadlineExceeded()) {
            return false;
        } else {
            return this.invocationCounter < this.maxRetries;
        }
    }

    public boolean jobDeadlineExceeded() {
        return Instant.now().getEpochSecond() > this.deadline;
    }

}
