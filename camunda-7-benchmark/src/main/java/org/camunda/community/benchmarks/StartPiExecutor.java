package org.camunda.community.benchmarks;

import org.camunda.community.benchmarks.config.BenchmarkConfiguration;
import org.camunda.community.benchmarks.exceptions.CommandStartPIWrapper;
import org.camunda.community.rest.client.api.ProcessDefinitionApi;
import org.camunda.community.rest.client.dto.VariableValueDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class StartPiExecutor {

    public static final String BENCHMARK_START_DATE_MILLIS = "benchmark_start_date_millis";
    private static final String BENCHMARK_STARTER_ID = "benchmark_starter_id";

    @Autowired
    private BenchmarkConfiguration config;

    //private ApiClient client;

    @Autowired
    private ProcessDefinitionApi processDefinitionApi;

    @Autowired
    private BenchmarkStartPiExceptionHandlingStrategy exceptionHandlingStrategy;

    private Map<String, Object> benchmarkPayload;

    @PostConstruct
    public void init() throws IOException {
        //client = new ApiClient();
        //client.setBasePath("http://localhost:8088/engine-rest");


        //String variablesJsonString = tryReadVariables(config.getPayloadPath().getInputStream());
        //benchmarkPayload = zeebeClientConfiguration.getJsonMapper().fromJsonAsMap(variablesJsonString);
    }

    public void startProcessInstance() {
        /*HashMap<String, Object> variables = new HashMap<>();
        variables.putAll(this.benchmarkPayload);
        variables.put(BENCHMARK_START_DATE_MILLIS, Instant.now().toEpochMilli());
        variables.put(BENCHMARK_STARTER_ID, config.getStarterId());*/

        //runtimeService.startProcessInstanceByKey(config.getBpmnProcessId(),variables);

        Map<String, VariableValueDto> variables = new HashMap<>();
        variables.put(
                BENCHMARK_START_DATE_MILLIS,
                new VariableValueDto().value(Instant.now().toEpochMilli()).type("long"));
        variables.put(
                BENCHMARK_STARTER_ID,
                new VariableValueDto().value(config.getStarterId()).type("string"));
        variables.put(
                "var2",
                new VariableValueDto().value(true).type("boolean"));

        CommandStartPIWrapper command = new CommandStartPIWrapper(
                processDefinitionApi,
                config.getBpmnProcessId(),
                variables,
                System.currentTimeMillis() + 5 * 60 * 1000, // 5 minutes
                "CreatePi" + config.getBpmnProcessId(),
                exceptionHandlingStrategy);
        command.executeAsync();

        //System.out.println("STARTED");
    }

    /*private String tryReadVariables(final InputStream inputStream) throws IOException {
        final StringBuilder stringBuilder = new StringBuilder();
        try (final InputStreamReader reader = new InputStreamReader(inputStream)) {
            try (final BufferedReader br = new BufferedReader(reader)) {
                String line;
                while ((line = br.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
            }
        }
        return stringBuilder.toString();
    }*/
}
