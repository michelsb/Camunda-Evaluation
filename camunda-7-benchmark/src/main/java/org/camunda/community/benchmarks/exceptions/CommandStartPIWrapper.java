package org.camunda.community.benchmarks.exceptions;

import org.camunda.community.rest.client.api.ProcessDefinitionApi;
import org.camunda.community.rest.client.dto.StartProcessInstanceDto;
import org.camunda.community.rest.client.dto.VariableValueDto;
import org.camunda.community.rest.client.invoker.ApiException;

import java.util.Collections;
import java.util.Map;

public class CommandStartPIWrapper extends CommandWrapper {

    private ProcessDefinitionApi processesDefinitionApi;
    private String bpmnProcessId;
    private Map<String, VariableValueDto> variables;
    private DefaultCommandExceptionHandlingStrategy commandExceptionHandlingStrategy;

    public CommandStartPIWrapper(ProcessDefinitionApi processesDefinitionApi, String bpmnProcessId, Map<String, VariableValueDto> variables, long deadline, String entityLogInfo, DefaultCommandExceptionHandlingStrategy commandExceptionHandlingStrategy) {
        super(processesDefinitionApi,bpmnProcessId,variables,deadline,entityLogInfo,commandExceptionHandlingStrategy);
        this.processesDefinitionApi = processesDefinitionApi;
        this.bpmnProcessId = bpmnProcessId;
        this.variables = variables;
        this.commandExceptionHandlingStrategy = commandExceptionHandlingStrategy;
    }

    public void executeAsync() {
        super.executeAsync();
        try {
            this.processesDefinitionApi.startProcessInstanceByKey(
                    this.bpmnProcessId,
                    new StartProcessInstanceDto()
                            .variables(this.variables));
        } catch (ApiException e) {
            this.commandExceptionHandlingStrategy.handleCommandError(this, e);
        }
    }

    public String toString() {
        return "{BPMN Process ID=" + this.bpmnProcessId + super.toString();
    }

}
