package org.camunda.community.benchmarks.exceptions;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;

public class CommandHandleFailureWrapper extends CommandWrapper {

    private ExternalTaskService externalTaskService;
    private ExternalTask externalTask;
    private DefaultCommandExceptionHandlingStrategy commandExceptionHandlingStrategy;
    private Exception exception;

    public CommandHandleFailureWrapper(ExternalTaskService externalTaskService, ExternalTask externalTask, long deadline, String entityLogInfo, DefaultCommandExceptionHandlingStrategy commandExceptionHandlingStrategy, Exception exception) {
        super(externalTaskService,externalTask,deadline,entityLogInfo,commandExceptionHandlingStrategy);
        this.externalTaskService = externalTaskService;
        this.externalTask = externalTask;
        this.commandExceptionHandlingStrategy = commandExceptionHandlingStrategy;
        this.exception = exception;
    }

    public void executeAsync() {
        super.executeAsync();
        try {
            this.externalTaskService.handleFailure(
                    this.externalTask.getId(),
                    this.externalTask.getWorkerId(),
                    this.exception.getMessage(),
                    0,
                    10L * 60L * 1000L);
        } catch (Exception e) {
            this.commandExceptionHandlingStrategy.handleCommandError(this, e);
        }
    }

    public String toString() {
        return "{Task=" + this.externalTask.getId() + super.toString();
    }

}
