package org.camunda.community.benchmarks;

import org.camunda.community.benchmarks.config.BenchmarkConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Instant;

@Component
@ConditionalOnProperty(name = "benchmark.startProcesses", havingValue = "true", matchIfMissing = true)
public class StartPiScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(StartPiScheduler.class);

    @Autowired
    private BenchmarkConfiguration config;

    @Autowired
    private StatisticsCollector stats;

    @Autowired
    private StartPiExecutor executor;

    private long startTimeInMillis = System.currentTimeMillis();
    private long piStarted = 0;
    private long piStartedGoal = 0;
    private long counter = 0;

    private long batchSize = 1;
    private long howOften = 1; // every 1, 2 or 3rd row

    @PostConstruct
    public void init() {
        calculateParameters(config.getStartPiPerSecond());
    }

    public void calculateParameters(long piPerSecondGoal) {
        this.piStartedGoal = piPerSecondGoal;
        stats.hintOnNewPiPerSecondGoald(piPerSecondGoal);
        if (piStartedGoal < 100) {
            // we can handle this by starting one PI every x times 10 ms
            batchSize = 1;
            // better more than too less, then we can stop when we hit the limit
            howOften = Math.round( Math.floor( 100.0 / piPerSecondGoal) );
        } else {
            // we need to start batches every 10 ms
            howOften = 1;
            // better more than too less, then we can stop when we hit the limit
            batchSize = Math.round(Math.ceil( piPerSecondGoal / 100.0));
        }
        LOG.info("Configured benchmark to start " + piPerSecondGoal + " PIs per second. This means every " + howOften + ". interval (of the 100 x 10ms intervals in total) with a batch size of " + batchSize);
    }

    /**
     * Every 10 ms we check how many process instances we need to start
     * 10 ms is what the Java platform seems to be able to do reliably in different environments
     * Add some initial delay to give the workers time to connect
     */
    //@Async
    @Scheduled(fixedRate = 10, initialDelay = 5000)
    public void startSomeProcessInstances() {
      
        long currentTime = System.currentTimeMillis();
        long passedTime = currentTime - startTimeInMillis;

        counter++;
        long processInstancesToStart = 0;

        // if we still have time till the second is up
        if (passedTime < 1000) {
            // Check if we should start another batch
            if (counter % howOften == 0) {
                // now check if we still want to do the full batch
                if (piStarted + batchSize > piStartedGoal) {
                    // just start the remaining instances
                    processInstancesToStart = piStartedGoal - piStarted;
                    piStarted += processInstancesToStart;
                } else {
                    // start the batch size
                    processInstancesToStart = batchSize;
                    piStarted += batchSize;
                }
            }
        } else {
            // check if we have remaining process instances to start
            processInstancesToStart = piStartedGoal - piStarted;

            // restart timer
            LOG.debug("One second is over, resetting timer");
            piStarted = 0;
            startTimeInMillis = System.currentTimeMillis();
        }
        // start after all calculations to avoid that the next scheduler run intervenes.
        // (the above calculations should always be faster than 10ms, starting a big batch might not)
        // TODO: Think about if we should detect if starting takes longer than the 10ms interval
        startProcessInstances( processInstancesToStart );
      
    }

    @Async
    private void startProcessInstances(long batchSize) {
        for (int i = 0; i < batchSize; i++) {
            executor.startProcessInstance();
            stats.incStartedProcessInstances();
        }
    }

    private static final double BACKPRESSURE_MAX_PERSCENTAGEG = 15; // TODO: use percentage
    private static final long BACKPRESSURE_ADJUSTEMENT_AMOUNT_BIG = 10;

    private static final double BACKPRESSURE_MAX_RATE_PER_SECOND_SMALL = 3;
    private static final long BACKPRESSURE_ADJUSTEMENT_AMOUNT_SMALL = 1;

    private double lastRatio = 0;
    //private boolean startingPhase = true;
    private double bestRatio = 0;
    private long bestStartRate = 0;

    private long warmupPhaseEndMillis;

    /**
     * Every 30 seconds the start rate might be adjusted based on the configured algorithm.
     * This starts after an initial warm-up-phase that can be configured.
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    public void adjustStartRates() {
        if (config.getWarmupPhaseDurationMillis() > 0 ) {
            if (warmupPhaseEndMillis == 0) {
                warmupPhaseEndMillis = Instant.now().toEpochMilli() + config.getWarmupPhaseDurationMillis();
            }
            // skip adjustment, if warmup phase is not yet finished
            if (Instant.now().toEpochMilli() < warmupPhaseEndMillis) {
                LOG.info("Benchmark is still in warmup phase");
                return;
            }
        }

        if ("none".equals(config.getStartRateAdjustmentStrategy())) {
            LOG.info("Start rate is fixed and will not be adjusted");
        } else if ("backpressure".equals(config.getStartRateAdjustmentStrategy())) {
            adjustByUsingBackpressure();
        } else if ("jobRatio".equals(config.getStartRateAdjustmentStrategy())) {
            adjustByUsingJobCompletionRatio();
        } else if ("piException".equals(config.getStartRateAdjustmentStrategy())) {
            adjustByUsingProcessException();
        } else if ("piRatio".equals(config.getStartRateAdjustmentStrategy())) {
            adjustByUsingPIRatio();
        } else {
            throw new RuntimeException("Invalid value for startRateAdjustmentStrategy: " + config.getStartRateAdjustmentStrategy());
        }
    }

    public void adjustByUsingPIRatio() {

        double piStartedRate = stats.getStartedPiMeter().getOneMinuteRate();
        double piCompletedRate = stats.getCompletedProcessInstancesMeter().getOneMinuteRate();
        long rate = 0;

        // Calculate current ration jobCompletion/startRate
        double ratio = stats.getCompletedJobsMeter().getOneMinuteRate() / stats.getStartedPiMeter().getOneMinuteRate();

        if (ratio >= bestRatio) {
            LOG.info("Found better ratio: "+ratio+" (instead of "+bestRatio+"). Remember start rate " + piStartedGoal);
            bestRatio = ratio;
            bestStartRate = piStartedGoal;
        } else {
            if (bestStartRate < piStartedGoal) {
                LOG.info("Ratio is getting worse: " + ratio + " (instead of " + bestRatio + "). Set original start rate " + bestStartRate);
                adjustStartRateTo(bestStartRate);
                return;
            }
        }

        if (piStartedRate < (0.5*piStartedGoal)) {
            rate = Math.round(Math.ceil(piStartedRate));
            LOG.info("Started PI one minute rate of "+piStartedRate+" less than 50% of delimited Start PI per second of "+config.getStartPiPerSecond()+", drop start rate to " + rate );
            adjustStartRateTo(rate);
        } else {
            if (piCompletedRate < (0.5*piStartedRate)) {
                //rate = Math.round(Math.ceil((double)config.getStartPiPerSecond()/10));
                rate = Math.round(Math.ceil((double)piStartedRate/2));
                LOG.info("Completed PI one minute rate of "+piCompletedRate+" less than 50% of current PI Started Rate  of "+piStartedRate+", drop start rate to " + rate );
                adjustStartRateTo(rate);
            } else {
                // Compare against last minute rate
                if (ratio <= 0.8*config.getTaskPiRatio()) { // keep slightly bigger then 0 to make sure we are going to the limit and not stay in a relaxed rate that fulfills the ratio
                    // if it drops - go back in start rate
                    rate = Math.round(Math.ceil((double)config.getStartPiPerSecond()/10));
                    LOG.info("Task/PI too low: "+ratio+", decrease start rate by " + rate );
                    adjustStartRateBy( -1 * rate );
                } else {
                    if (ratio >= 0.95*config.getTaskPiRatio()) {
                        // otherwise increase start rate
                        rate = Math.round(Math.ceil((double) config.getStartPiPerSecond() / 10));
                        LOG.info("Task/PI too high: " + ratio + ", increase start rate by " + rate);
                        adjustStartRateBy(rate);
                    }
                }
            }
        }
    }

    public void adjustByUsingJobCompletionRatio() {
        // TODO! This does not yield good results and need more thoughts

        // Calculate current ration jobCompletion/startRate
        double ratio = stats.getCompletedJobsMeter().getOneMinuteRate() / stats.getStartedPiMeter().getOneMinuteRate();

        if (ratio >= bestRatio) {
            LOG.info("Found better ratio: "+ratio+" (instead of "+bestRatio+"). Remember start rate " + piStartedGoal);
            bestRatio = ratio;
            bestStartRate = piStartedGoal;
        } else {
            LOG.info("Ratio is getting worse: "+ratio+" (instead of "+bestRatio+"). Set original start rate " + bestStartRate);
            adjustStartRateBy( bestStartRate );
        }

        // Compare against last minute rate
        if (ratio - 0.5 <= config.getTaskPiRatio()) { // keep slightly bigger then 0 to make sure we are going to the limit and not stay in a relaxed rate that fulfills the ratio
            // if it drops - go back in start rate
            long rate = Math.round(Math.ceil(config.getStartPiPerSecond()/10));
            //LOG.info("Job/PI ratio dropped from "+lastRatio+" to "+ratio+", decrease start rate by " + rate );
            LOG.info("Task/PI too low: "+ratio+", increase start rate by " + rate );
            adjustStartRateBy( rate );
        } else {
            // otherwise increase start rate
            long rate = Math.round(Math.ceil(config.getStartPiPerSecond()/10));
            //LOG.info("Job/PI ratio increased from "+lastRatio+" to "+ratio+", increase start rate by " + rate );
            LOG.info("Task/PI too high: "+ratio+", decrease start rate by " + rate );
            adjustStartRateBy( -1 * rate );
        }
        lastRatio = ratio;
    }

    public void adjustByUsingBackpressure() {
        // Handle "almost no backressure" as special case with small numbers
        if (stats.getBackpressureOnStartPiMeter().getOneMinuteRate() < 1) {
            // increase it by bigger junk (10% of goal)
            long rate = Math.round(Math.ceil(config.getStartPiPerSecond()/10));
            LOG.info("Backpressure dropped, increasing start rate by " + rate );
            adjustStartRateBy( rate );
        }  else {
            double backpressurePercentage = stats.getBackpressureOnStartPercentage();
            if (backpressurePercentage > config.getMaxBackpressurePercentage()) {
                // Backpressure too high - reduce start rate
                long rate = Math.round( (config.getMaxBackpressurePercentage() - backpressurePercentage)/100 * piStartedGoal * config.getStartPiReduceFactor());
                LOG.info("Backpressure percentage too high ("+backpressurePercentage+" > "+config.getMaxBackpressurePercentage()+"), reducing start rate by " + rate );
                adjustStartRateBy(rate);
            } else{
                // Backpressure is there, but lower than the maximum considered optimal for throughput
                // slightly increase start rate
                long rate = Math.round( (config.getMaxBackpressurePercentage() - backpressurePercentage)/100 * piStartedGoal * config.getStartPiIncreaseFactor());
                LOG.info("Backpressure percentage too low ("+backpressurePercentage+" <= "+config.getMaxBackpressurePercentage()+"), increasing start rate by " + rate );
                adjustStartRateBy(rate);
            }
        }
    }
    public void adjustByUsingProcessException() {
        // Handle "almost no exception" as special case with small numbers
        long rate = 0;
        if (stats.getProcessInstancesExceptionOnStartPiMeter().getOneMinuteRate() < 1) {
            // increase it by bigger junk (10% of goal)
            rate = Math.round(Math.ceil((float)config.getStartPiPerSecond()/10));
            LOG.info("Exception percentage (one minute rate) less than 1%, increasing start rate by " + rate );
        }  else {
            double exceptionPercentage = stats.getProcessInstancesExceptionOnStartPercentage();
            if (exceptionPercentage > config.getMaxPIExceptionPercentage()) {
                // Exception too high - reduce start rate
                rate = Math.round((config.getMaxPIExceptionPercentage() - exceptionPercentage)/100 * piStartedGoal * config.getStartPiReduceFactor());
                if ((piStartedGoal>=1)&&(rate==0)) {
                    rate = -1;
                }
                LOG.info("Backpressure percentage too high ("+exceptionPercentage+" > "+config.getMaxPIExceptionPercentage()+"), reducing start rate by " + rate );
            } else{
                // Exception is there, but lower than the maximum considered optimal for throughput
                // slightly increase start rate
                rate = Math.round((config.getMaxPIExceptionPercentage() - exceptionPercentage)/100 * piStartedGoal * config.getStartPiIncreaseFactor());
                if (rate==0) {
                    rate = 1;
                }
                LOG.info("Backpressure percentage too low ("+exceptionPercentage+" <= "+config.getMaxPIExceptionPercentage()+"), increasing start rate by " + rate );
            }
        }
        adjustStartRateBy(rate);
    }

    private void adjustStartRateTo(long amount) {
        long newGoal =  amount;
        if (newGoal<=0) {
            newGoal = 1;
        }
        calculateParameters(newGoal);
    }

    private void adjustStartRateBy(long amount) {
        long newGoal =  piStartedGoal + amount;
        if (newGoal<=0) {
            newGoal = 1;
        }
        calculateParameters(newGoal);
    }
}
