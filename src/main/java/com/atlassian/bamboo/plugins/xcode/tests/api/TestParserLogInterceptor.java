package com.atlassian.bamboo.plugins.xcode.tests.api;

import com.atlassian.bamboo.build.LogEntry;
import com.atlassian.bamboo.build.logger.LogInterceptor;
import com.atlassian.bamboo.results.tests.TestResults;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class TestParserLogInterceptor implements LogInterceptor
{
    // ------------------------------------------------------------------------------------------------------- Constants
    // ------------------------------------------------------------------------------------------------- Type Properties

    private final XcodeTestParser xcodeTestParser;

    // ---------------------------------------------------------------------------------------------------- Dependencies
    // ---------------------------------------------------------------------------------------------------- Constructors

    public TestParserLogInterceptor(XcodeTestParser xcodeTestParser)
    {
        this.xcodeTestParser = xcodeTestParser;
    }

    // ----------------------------------------------------------------------------------------------- Interface Methods

    public void intercept(@NotNull final LogEntry logEntry)
    {
        xcodeTestParser.processLine(logEntry.getLog());
    }

    public void interceptError(@NotNull final LogEntry logEntry)
    {
        xcodeTestParser.processLine(logEntry.getLog());
    }

    // -------------------------------------------------------------------------------------------------- Action Methods
    // -------------------------------------------------------------------------------------------------- Public Methods

    public Set<TestResults> getFailingTestResults()
    {
        return xcodeTestParser.getFailingTestResults();
    }

    public Set<TestResults> getSuccessfulTestResults()
    {
        return xcodeTestParser.getSuccessfulTestResults();
    }

    // -------------------------------------------------------------------------------------- Basic Accessors / Mutators
}