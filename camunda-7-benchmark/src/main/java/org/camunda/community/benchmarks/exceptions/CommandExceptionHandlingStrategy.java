package org.camunda.community.benchmarks.exceptions;

public interface CommandExceptionHandlingStrategy {
    void handleCommandError(CommandWrapper var1, Throwable var2);
}
