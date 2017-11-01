package com.atlassian.bamboo.plugins.xcode.tests.xctest;

import com.atlassian.bamboo.results.tests.TestResults;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public class XCTestParserTest
{
    XCTestParser xcTestParser;
    Set<TestResults> successfulTestResults;
    Set<TestResults> failingTestResults;

    @Test
    public void testSuccessfulTestParsing() throws Exception
    {
        parse("/com/atlassian/bamboo/plugins/xcode/tests/xctest-success.txt");
        assertSuccessfulResultCount(12);
        assertNoFailingResults();
    }

    @Test
    public void testFailingTestParsing() throws Exception
    {
        parse("/com/atlassian/bamboo/plugins/xcode/tests/xctest-failed.txt");
        assertSuccessfulResultCount(1);
        assertFailingResultCount(1);
    }

    @Test
    public void testConsecutiveTestsWithSameNameParsing() throws Exception
    {
        parse("/com/atlassian/bamboo/plugins/xcode/tests/xctest-consecutive-identical-test-names.txt");
        assertSuccessfulResultCount(218);
        assertNoFailingResults();
    }

    @Test
    public void testQuickTestNameParsing() throws Exception
    {
        parse("/com/atlassian/bamboo/plugins/xcode/tests/xctest-quick-results.txt");
        assertSuccessfulResultCount(9);
        assertNoFailingResults();

        for (TestResults results : successfulTestResults) {
            Assert.assertTrue("Test results are Quick results", results instanceof QuickSpecTestResults);
        }
    }

    @Before
    public void setup()
    {
        xcTestParser = new XCTestParser();
    }

    @After
    public void tearDown()
    {
        xcTestParser = null;
        successfulTestResults = null;
        failingTestResults = null;
    }

    private void parse(String resource) throws IOException
    {
        InputStream testLogStream = null;
        try {
            testLogStream = getClass().getResourceAsStream(resource);
            for (String line : IOUtils.readLines(testLogStream)) {
                xcTestParser.processLine(line);
            }
            successfulTestResults = xcTestParser.getSuccessfulTestResults();
            failingTestResults = xcTestParser.getFailingTestResults();
        }
        finally {
            IOUtils.closeQuietly(testLogStream);
        }
    }

    private void assertSuccessfulResultCount(int count) {
        Assert.assertEquals("Correct number of successful tests", count, successfulTestResults.size());
    }

    private void assertFailingResultCount(int count) {
        Assert.assertEquals("Correct number of failing tests", count, failingTestResults.size());
    }

    private void assertNoFailingResults() {
        assertFailingResultCount(0);
    }
}