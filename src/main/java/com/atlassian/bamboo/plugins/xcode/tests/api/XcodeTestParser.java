package com.atlassian.bamboo.plugins.xcode.tests.api;

import com.atlassian.bamboo.results.tests.TestResults;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Defines a test parser for the Xcode plugin
 */
public interface XcodeTestParser
{
    /**
     * Successful tests for processed logs
     * @return successful tests
     */
    Set<TestResults> getSuccessfulTestResults();

    /**
     * Failing tests for processed logs
     * @return failing tests
     */
    Set<TestResults> getFailingTestResults();

    /**
     * Processes line of log
     * @param line
     */
    void processLine(@NotNull String line);
}
