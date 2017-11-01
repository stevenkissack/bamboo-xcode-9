/*
 * Regex copied from https://github.com/jenkinsci/xcode-plugin/blob/master/src/main/java/au/com/rayh/XCodeBuildOutputParser.java
 * 
 * The MIT License
 *
 * Copyright (c) 2011 Ray Yamamoto Hilton
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.atlassian.bamboo.plugins.xcode.tests.ocunit;

import com.atlassian.bamboo.plugins.xcode.tests.api.XcodeTestParser;
import com.atlassian.bamboo.results.tests.TestResults;
import com.atlassian.bamboo.resultsummary.tests.TestCaseResultErrorImpl;
import com.atlassian.bamboo.resultsummary.tests.TestState;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test parser for OCUnit
 */
public class OCUnitTestParser implements XcodeTestParser
{
    private static final Logger log = Logger.getLogger(OCUnitTestParser.class);

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final Pattern TEST_ARCHITECTURE = Pattern.compile("Run unit tests for architecture '(\\S+)'.*");

    private static final Pattern START_SUITE = Pattern.compile("^.*Test Suite '(\\S+)'.*started at\\s+(.*)$");
    private static final Pattern END_SUITE = Pattern.compile("^.*Test Suite '(\\S+)'.*finished at\\s+(.*).$");
    private static final Pattern START_TESTCASE = Pattern.compile("^.*Test Case '-\\[\\S+\\s+(\\S+)\\]' started.$");
    private static final Pattern SUCCESSFUL_TESTCASE = Pattern.compile("^.*Test Case '-\\[\\S+\\s+(\\S+)\\]' passed \\((.*) seconds\\).$");
    private static final Pattern FAILED_TESTCASE = Pattern.compile("^.*Test Case '-\\[\\S+ (\\S+)\\]' failed \\((\\S+) seconds\\).$");
    private static final Pattern ERROR_TESTCASE = Pattern.compile("^.*(.*): error: -\\[(\\S+) (\\S+)\\] : (.*)$");

    // ------------------------------------------------------------------------------------------------- Type Properties

    private final Set<TestResults> successfulTestResults = Sets.newHashSet();
    private final Set<TestResults> failingTestResults = Sets.newHashSet();

    private String currentArchitecture;
    private String currentSuiteName;
    private String currentTestName;
    private String currentTestDuration;
    private StringBuilder currentTestOutput = new StringBuilder();
    private List<String> currentTestErrors = Lists.newLinkedList();

    // ---------------------------------------------------------------------------------------------------- Dependencies
    // ---------------------------------------------------------------------------------------------------- Constructors
    // ----------------------------------------------------------------------------------------------- Interface Methods
    // -------------------------------------------------------------------------------------------------- Action Methods
    // -------------------------------------------------------------------------------------------------- Public Methods

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

    // -------------------------------------------------------------------------------------- Basic Accessors / Mutators

    @Override
    public void processLine(@NotNull String line)
    {
        final Matcher architectureMatcher = TEST_ARCHITECTURE.matcher(line);
        if (architectureMatcher.matches())
        {
            currentArchitecture = architectureMatcher.group(1);
            log.debug("Found test architecture '" + currentArchitecture + "'");
        }

        final Matcher startSuiteMatcher = START_SUITE.matcher(line);
        if (startSuiteMatcher.matches())
        {
            currentSuiteName = startSuiteMatcher.group(1);
        }

        final Matcher endSuiteMatcher = END_SUITE.matcher(line);
        if (endSuiteMatcher.matches())
        {
            endTestSuite();
        }

        final Matcher startTestCaseMatcher = START_TESTCASE.matcher(line);
        if (startTestCaseMatcher.matches())
        {
            currentTestName = startTestCaseMatcher.group(1);
        }

        final Matcher successfulTestCase = SUCCESSFUL_TESTCASE.matcher(line);
        if (successfulTestCase.matches())
        {
            currentTestDuration = successfulTestCase.group(2);
            recordRestResult(TestState.SUCCESS);
            endTestCase();
        }

        final Matcher failedTestCaseMatcher = FAILED_TESTCASE.matcher(line);
        if (failedTestCaseMatcher.matches())
        {
            currentTestDuration = failedTestCaseMatcher.group(2);
            recordRestResult(TestState.FAILED);
            endTestCase();
        }

        final Matcher testCaseErrorMatcher = ERROR_TESTCASE.matcher(line);
        if (testCaseErrorMatcher.matches())
        {
            currentTestErrors.add(line);
        }

        recordTestOutput(line);
    }

    private void recordRestResult(@NotNull TestState testState)
    {
        final String suiteName = StringUtils.isNotEmpty(currentArchitecture) ? currentSuiteName + " (" + currentArchitecture + ")" : currentSuiteName;
        final TestResults testResult = new TestResults(suiteName, currentTestName, currentTestDuration);
        testResult.setSystemOut(currentTestOutput.toString());

        testResult.setState(testState);

        if (testState == TestState.SUCCESS)
        {
            successfulTestResults.add(testResult);
        }
        else if (testState == TestState.FAILED)
        {
            for (String errorLine : currentTestErrors)
            {
                testResult.addError(new TestCaseResultErrorImpl(errorLine));
            }
            failingTestResults.add(testResult);
        }
    }

    private void recordTestOutput(@NotNull String line)
    {
        if (StringUtils.isNotEmpty(currentSuiteName) && StringUtils.isNotEmpty(currentTestName))
        {
            currentTestOutput.append(line);
            currentTestOutput.append('\n');
        }
    }

    private void endTestSuite()
    {
        currentSuiteName = null;
    }

    private void endTestCase()
    {
        currentTestName = null;
        currentTestOutput = new StringBuilder();
        currentTestErrors = Lists.newLinkedList();
    }
}
