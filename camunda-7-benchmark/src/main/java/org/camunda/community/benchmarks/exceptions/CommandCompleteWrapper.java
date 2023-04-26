package org.camunda.community.benchmarks.exceptions;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;

public class CommandCompleteWrapper extends CommandWrapper {

    private ExternalTaskService externalTaskService;
    private ExternalTask externalTask;
    private DefaultCommandExceptionHandlingStrategy commandExceptionHandlingStrategy;

    public CommandCompleteWrapper(ExternalTaskService externalTaskService, ExternalTask externalTask, long deadline, String entityLogInfo, DefaultCommandExceptionHandlingStrategy commandExceptionHandlingStrategy) {
        super(externalTaskService,externalTask,deadline,entityLogInfo,commandExceptionHandlingStrategy);
        this.externalTaskService = externalTaskService;
        this.externalTask = externalTask;
        this.commandExceptionHandlingStrategy = commandExceptionHandlingStrategy;
    }

    public void executeAsync() {
        //System.out.println("ENTROU: " + externalTask.getActivityId());
        super.executeAsync();
        //System.out.println("ENTROU: " + externalTask.getBusinessKey());
        try {
            this.externalTaskService.complete(this.externalTask);
            //System.out.println("ENTROU: " + externalTask.getProcessDefinitionKey());
        } catch (Exception e) {
            this.commandExceptionHandlingStrategy.handleCommandError(this, e);
        }
    }

    public String toString() {
        return "{Task=" + this.externalTask.getId();// + super.toString();
    }

}
