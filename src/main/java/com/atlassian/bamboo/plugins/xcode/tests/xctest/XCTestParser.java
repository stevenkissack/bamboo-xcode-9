package com.atlassian.bamboo.plugins.xcode.tests.xctest;

import com.atlassian.bamboo.plugins.xcode.tests.api.XcodeTestParser;
import com.atlassian.bamboo.results.tests.TestResults;
import com.atlassian.bamboo.resultsummary.tests.TestState;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class XCTestParser implements XcodeTestParser
{
    @SuppressWarnings("UnusedDeclaration")
    private static final Logger log = Logger.getLogger(XCTestParser.class);
    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String TEST_SUITE = "Test Suite";
    private static final String TEST_CASE = "Test Case";
    private static final String TEST_SUCCESS = "** TEST SUCCEEDED **";
    private static final String TEST_FAILED = "** TEST FAILED **";

    // ------------------------------------------------------------------------------------------------- Type Properties

    private final Set<TestResults> successfulTestResults = Sets.newHashSet();
    private final Set<TestResults> failingTestResults = Sets.newHashSet();

    private boolean testStarted;
    private String currentTestKey;
    private String currentSuiteName;
    private String currentTestName;
    private String currentTestDuration = "0";
    private StringBuilder currentTestOutput = new StringBuilder();
    private List<String> currentTestErrors = Lists.newLinkedList();

    // ---------------------------------------------------------------------------------------------------- Dependencies
    // ---------------------------------------------------------------------------------------------------- Constructors
    // ----------------------------------------------------------------------------------------------- Interface Methods

    @Override
    public Set<TestResults> getSuccessfulTestResults()
    {
        return successfulTestResults;
    }

    @Override
    public Set<TestResults> getFailingTestResults()
    {
        return failingTestResults;
    }

    @Override
    public void processLine(@NotNull String line)
    {
        //If it starts with test suite or test case, we can process this line
        if (line.startsWith(TEST_SUITE))
        {
            testStarted = true;
            int start = line.indexOf("'");
            int end = line.lastIndexOf("'");
            if (start > -1 && end > -1)
            {
                currentSuiteName = line.substring(start + 1, end);
            }
            else
            {
                log.warn("Could not parse test suite line");
            }
        }
        else if (line.startsWith(TEST_CASE))
        {
            testStarted = true;

            int start = line.indexOf("'") + 1;
            int end = line.lastIndexOf("'");
            if (start > -1 && end > -1)
            {
                // Skips opening '['
                String testKey = line.substring(start + 1, end);

                String[] parts = testKey.split(" ");
                String testName = parts[1].replace("]", "");

                if (testKey.equals(currentTestKey))
                {
                    TestState testState = line.contains("passed") ? TestState.SUCCESS : TestState.FAILED;
                    createTestCaseResult(testState);
                    resetState();
                }
                else
                {
                    currentTestKey = testKey;
                    currentTestName = testName;
                }
            }
            else
            {
                log.warn("Could not process line '" + line + "'");
            }
        }
        else if (line.equals(TEST_FAILED) || line.equals(TEST_SUCCESS))
        {
            System.out.println("Run completed");
            resetState();
        }
        else if (testStarted)
        {
            currentTestOutput.append(line);
        }
    }

    // -------------------------------------------------------------------------------------------------- Action Methods
    // -------------------------------------------------------------------------------------------------- Public Methods
    // ------------------------------------------------------------------------------------------------- Helper Methods
    // -------------------------------------------------------------------------------------- Basic Accessors / Mutators

    private void createTestCaseResult(TestState testState)
    {
        if (StringUtils.isNotEmpty(currentTestKey))
        {
            final TestResults testResults = makeTestResults();
            testResults.setSystemOut(currentTestOutput.toString());
            testResults.setState(testState);
            final Set<TestResults> resultSet = testState == TestState.SUCCESS ? successfulTestResults : failingTestResults;
            resultSet.add(testResults);
        }
    }

    private TestResults makeTestResults()
    {
        if (QuickSpecTestResults.isQuickTestName(currentTestName))
        {
            return new QuickSpecTestResults(currentSuiteName, currentTestName, currentTestDuration);
        }

        return new TestResults(currentSuiteName, currentTestName, currentTestDuration);
    }

    private void resetState()
    {
        testStarted = false;
        currentTestName = null;
        currentTestKey = null;
        currentTestOutput = new StringBuilder();
        currentTestErrors.clear();
    }
}
