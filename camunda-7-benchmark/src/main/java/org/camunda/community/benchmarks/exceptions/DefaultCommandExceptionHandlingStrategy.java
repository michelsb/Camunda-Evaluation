package org.camunda.community.benchmarks.exceptions;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultCommandExceptionHandlingStrategy implements CommandExceptionHandlingStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private ScheduledExecutorService scheduledExecutorService;

    public DefaultCommandExceptionHandlingStrategy(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
    }

    public void handleCommandError(CommandWrapper command, Throwable throwable) {
        if (command.hasMoreRetries()) {
            command.increaseBackoffUsing();
            LOG.warn("Retrying " + command + " after error of type '" + throwable.getMessage() + "' with backoff");
            //LOG.warn("Retrying " + command + " after error of type NO_RESPONSE with backoff");
            command.scheduleExecutionUsing(this.scheduledExecutorService);
            return;
        }
        throw new RuntimeException("Could not execute " + command + " due to error of type '" + throwable.getMessage() + "' and no retries are left", throwable);
        //throw new RuntimeException("Could not execute " + command + " due to error of type NO_RESPONSE and no retries are left", throwable);
    }
}
