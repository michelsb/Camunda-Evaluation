package org.camunda.community.benchmarks.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
@ConfigurationProperties(prefix = "benchmark")
public class BenchmarkConfiguration {

    private String starterId = "benchmarkStarter1";

    private boolean startProcesses = true;
    private long startPiPerSecond = 500;
    private String jobType = "benchmark-task";
    private int multipleJobTypes = 0;
    private boolean startWorkers = true;
    private long taskCompletionDelay = 200;
    private String bpmnProcessId = "benchmark";
    private Resource payloadPath; // = new UrlResource("classpath:bpmn/typical_payload.json");
    private Resource[] bpmnResource;
    private boolean autoDeployProcess = true;

    private long warmupPhaseDurationMillis = 0;
    private String startRateAdjustmentStrategy="backpressure";

    private double maxBackpressurePercentage = 10.0;
    private double maxPIExceptionPercentage = 10.0;
    private double startPiReduceFactor = 0.4;
    private double startPiIncreaseFactor = 0.4;

    private int taskPiRatio = 10;

    //Specifies the maximum amount of tasks that can be fetched within one request. Default is 1000 jobs
    private int maxJobs = 1000;

    //The time for how long a job is exclusively assigned for this worker (in milliseconds). (Default is 5 minutes)
    private long lockTimeout = 300000;

    // Set the request timeout for activate job request used to poll for new job (in milliseconds). Default is 10 seconds.
    private long requestTimeout = 10000;

    // Define the backoff between two requests (in milliseconds). When 0, use the default Exponential Backoff Supplier. Otherwise, specify fixed number of millis backoff.
    private long fixedBackOffDelay = 0;

    private Resource messageScenario;
    private long messagesTtl;
    private long messagesScenariosPerSecond;
    private long delayBetweenMessages;
    private long messagesLoadDuration;

    public String getStartRateAdjustmentStrategy() {
        return startRateAdjustmentStrategy;
    }

    public void setStartRateAdjustmentStrategy(String startRateAdjustmentStrategy) {
        this.startRateAdjustmentStrategy = startRateAdjustmentStrategy;
    }

    public long getWarmupPhaseDurationMillis() {
        return warmupPhaseDurationMillis;
    }

    public void setWarmupPhaseDurationMillis(long warmupPhaseDurationMillis) {
        this.warmupPhaseDurationMillis = warmupPhaseDurationMillis;
    }

    public int getTaskPiRatio() {
        return taskPiRatio;
    }

    public void setTaskPiRatio(int taskPiRatio) {
        this.taskPiRatio = taskPiRatio;
    }

    public Resource getPayloadPath() {
        return payloadPath;
    }

    public void setPayloadPath(Resource payloadPath) {
        this.payloadPath = payloadPath;
    }

    public long getTaskCompletionDelay() {
        return taskCompletionDelay;
    }

    public void setTaskCompletionDelay(long taskCompletionDelay) {
        this.taskCompletionDelay = taskCompletionDelay;
    }


    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public int getMultipleJobTypes() {
        return multipleJobTypes;
    }

    public void setMultipleJobTypes(int multipleJobTypes) {
        this.multipleJobTypes = multipleJobTypes;
    }

    public boolean isStartWorkers() {
        return startWorkers;
    }

    public void setStartWorkers(boolean startWorkers) {
        this.startWorkers = startWorkers;
    }

    public long getStartPiPerSecond() {
        return startPiPerSecond;
    }

    public void setStartPiPerSecond(long startPiPerSecond) {
        this.startPiPerSecond = startPiPerSecond;
    }

    public void setBpmnProcessId(String bpmnProcessId) {
        this.bpmnProcessId = bpmnProcessId;
    }

    public String getBpmnProcessId() {
        return bpmnProcessId;
    }

    public double getMaxBackpressurePercentage() {
        return maxBackpressurePercentage;
    }

    public void setMaxBackpressurePercentage(double maxBackpressurePercentage) {
        this.maxBackpressurePercentage = maxBackpressurePercentage;
    }

    public double getMaxPIExceptionPercentage() {
        return maxPIExceptionPercentage;
    }

    public void setMaxPIExceptionPercentage(double maxPIExceptionPercentage) {
        this.maxPIExceptionPercentage = maxPIExceptionPercentage;
    }

    public double getStartPiReduceFactor() {
        return startPiReduceFactor;
    }

    public double getStartPiIncreaseFactor() {
        return startPiIncreaseFactor;
    }

    public void setStartPiIncreaseFactor(double startPiIncreaseFactor) {
        this.startPiIncreaseFactor = startPiIncreaseFactor;
    }

    public boolean isAutoDeployProcess() {
        return autoDeployProcess;
    }

    public void setAutoDeployProcess(boolean autoDeployProcess) {
        this.autoDeployProcess = autoDeployProcess;
    }

    public Resource[] getBpmnResource() {
        return bpmnResource;
    }

    public void setBpmnResource(Resource[] bpmnResource) {
        this.bpmnResource = bpmnResource;
    }

    public void setStartPiReduceFactor(double startPiReduceFactor) {
        this.startPiReduceFactor = startPiReduceFactor;
    }

    public String getStarterId() {
        return starterId;
    }

    public void setStarterId(String starterId) {
        this.starterId = starterId;
    }

    public int getMaxJobs() {
        return maxJobs;
    }

    public void setMaxJobs(int maxJobs) {
        this.maxJobs = maxJobs;
    }

    public long getLockTimeout() {
        return lockTimeout;
    }

    public void setLockTimeout(long lockTimeout) {
        this.lockTimeout = lockTimeout;
    }

    public long getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(long requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public long getFixedBackOffDelay() {
        return fixedBackOffDelay;
    }

    public void setFixedBackOffDelay(long fixedBackOffDelay) {
        this.fixedBackOffDelay = fixedBackOffDelay;
    }
    public boolean isStartProcesses() {
        return startProcesses;
    }

    public void setStartProcesses(boolean startProcesses) {
        this.startProcesses = startProcesses;
    }

    public Resource getMessageScenario() {
        return messageScenario;
    }

    public void setMessageScenario(Resource messageScenario) {
        this.messageScenario = messageScenario;
    }

    public long getMessagesTtl() {
        return messagesTtl;
    }

    public void setMessagesTtl(long messagesTtl) {
        this.messagesTtl = messagesTtl;
    }

    public long getMessagesScenariosPerSecond() {
        return messagesScenariosPerSecond;
    }

    public void setMessagesScenariosPerSecond(long messagesScenariosPerSecond) {
        this.messagesScenariosPerSecond = messagesScenariosPerSecond;
    }

    public long getDelayBetweenMessages() {
        return delayBetweenMessages;
    }

    public void setDelayBetweenMessages(long delayBetweenMessages) {
        this.delayBetweenMessages = delayBetweenMessages;
    }

    public long getMessagesLoadDuration() {
        return messagesLoadDuration;
    }

    public void setMessagesLoadDuration(long messagesLoadDuration) {
        this.messagesLoadDuration = messagesLoadDuration;
    }

}

