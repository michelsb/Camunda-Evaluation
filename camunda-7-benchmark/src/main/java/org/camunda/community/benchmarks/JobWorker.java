package org.camunda.community.benchmarks;

import org.camunda.bpm.client.backoff.ExponentialBackoffStrategy;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.community.benchmarks.config.BenchmarkConfiguration;
import org.camunda.community.benchmarks.exceptions.CommandCompleteWrapper;
import org.camunda.community.benchmarks.exceptions.CommandHandleFailureWrapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import org.camunda.bpm.client.ExternalTaskClient;

import java.time.Instant;

@Component
public class JobWorker {

    @Autowired
    private BenchmarkConfiguration config;

    @Autowired
    private BenchmarkCompleteJobExceptionHandlingStrategy exceptionHandlingStrategy;

    // TODO: Check if we can/need to check if the scheduler can catch up with all its work (or if it is overwhelmed)
    @Autowired
    private TaskScheduler scheduler;

    private ExternalTaskClient client;

    @Autowired
    private StatisticsCollector stats;

    private void registerWorker(String jobType) {

        long fixedBackOffDelay = config.getFixedBackOffDelay();

        if(fixedBackOffDelay > 0) {
            client = ExternalTaskClient.create()
                    .lockDuration(config.getLockTimeout())
                    .maxTasks(config.getMaxJobs())
                    .asyncResponseTimeout(config.getRequestTimeout())
                    .backoffStrategy(new ExponentialBackoffStrategy(fixedBackOffDelay, 0, fixedBackOffDelay))
                    .baseUrl("http://localhost:8088/engine-rest")
                    .build();
        } else {
            client = ExternalTaskClient.create()
                    .lockDuration(config.getLockTimeout())
                    .maxTasks(config.getMaxJobs())
                    .asyncResponseTimeout(config.getRequestTimeout())
                    .baseUrl("http://localhost:8088/engine-rest")
                    .build();
        }

        // subscribe to the topic
        client.subscribe(jobType)
                .handler(new SimpleDelayCompletionHandler(true))
                .open();

    }

    // Don't do @PostConstruct as this is too early in the Spring lifecycle
    //@PostConstruct
    public void startWorkers() {
        String taskType = config.getJobType();

        boolean startWorkers = config.isStartWorkers();
        int numberOfJobTypes = config.getMultipleJobTypes();

        if(startWorkers) {
            if (numberOfJobTypes <= 0) {
                //registerWorkersForTaskType(taskType);
                registerWorker(taskType + "-" + config.getStarterId());
            } else {
                for (int i = 0; i < numberOfJobTypes; i++) {
                    //registerWorkersForTaskType(taskType + "-" + (i + 1));
                    registerWorker(taskType + "-" + (i + 1) + "-" + config.getStarterId());
                }
            }
            registerWorker(taskType + "-" + config.getStarterId() + "-completed");
        }
    }

    /*private void registerWorkersForTaskType(String taskType) {
        // worker for normal task type
        //registerWorker(taskType);

        // worker for normal "task-type-{starterId}"
        registerWorker(taskType + "-" + config.getStarterId());

        // worker marking completion of process instance via "task-type-complete"
        //registerWorker(taskType + "-completed");

        // worker marking completion of process instance via "task-type-complete"
        registerWorker(taskType + "-" + config.getStarterId() + "-completed");
    }*/

    public class SimpleDelayCompletionHandler implements ExternalTaskHandler {

        private boolean markProcessInstanceCompleted;

        public SimpleDelayCompletionHandler(boolean markProcessInstanceCompleted) {
            this.markProcessInstanceCompleted = markProcessInstanceCompleted;
        }

        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {

            //System.out.println(externalTask.getId());

            long deadline = externalTask.getLockExpirationTime().toInstant().toEpochMilli();

            //System.out.println(externalTask.getWorkerId());

            CommandCompleteWrapper command = new CommandCompleteWrapper(
                    externalTaskService,
                    externalTask,
                    deadline,
                    externalTask.toString(),
                    exceptionHandlingStrategy);

            //System.out.println(command.toString());
            //System.out.println(externalTask.getExecutionId());

            scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    try {
                        //System.out.println(externalTask.getProcessInstanceId());
                        command.executeAsync();
                        //System.out.println(externalTask.getTopicName());
                        stats.incCompletedJobs();
                        if (markProcessInstanceCompleted && externalTask.getTopicName().contains("-completed")) {
                            Object startEpochMillis = externalTask.getAllVariables().get(StartPiExecutor.BENCHMARK_START_DATE_MILLIS);
                            if (startEpochMillis!=null && startEpochMillis instanceof Long) {
                                stats.incCompletedProcessInstances((Long)startEpochMillis, Instant.now().toEpochMilli());
                            } else {
                                stats.incCompletedProcessInstances();
                            }
                        }
                    }
                    catch (Exception e) {
                        CommandHandleFailureWrapper command = new CommandHandleFailureWrapper(
                                externalTaskService,
                                externalTask,
                                deadline,
                                externalTask.toString(),
                                exceptionHandlingStrategy,e);
                        command.executeAsync();
                    }
                }
            }, Instant.now().plusMillis(config.getTaskCompletionDelay()));
        }
    }
}
