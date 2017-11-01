package com.atlassian.bamboo.plugins.xcode.tests.ocunit;

import com.atlassian.bamboo.plugins.xcode.tests.api.XcodeTestParser;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class OCUnitTestParserTest
{
    XcodeTestParser parser;

    @Before
    public void setupLogFile() throws IOException
    {
        parser = new OCUnitTestParser();
        final InputStream testLogStream = getClass().getResourceAsStream("/com/atlassian/bamboo/plugins/xcode/tests/testlog.txt");
        for (String line : IOUtils.readLines(testLogStream))
        {
            parser.processLine(line);
        }
    }

    @Test
    public void testParsesAllTestsFromLogs()
    {
        Assert.assertEquals(4, parser.getFailingTestResults().size());
        Assert.assertEquals(2, parser.getSuccessfulTestResults().size());
    }
}
