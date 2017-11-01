package com.atlassian.bamboo.plugins.xcode.tests.xctest;

import com.atlassian.bamboo.results.tests.TestResults;

class QuickSpecTestResults extends TestResults
{
    private final QuickSpecName testName;

    public static boolean isQuickTestName(String testName)
    {
        return QuickSpecName.isValidName(testName);
    }

    public QuickSpecTestResults(String className, String methodName, String duration)
    {
        super(className, methodName, duration);
        this.testName = new QuickSpecName(methodName);
    }

    public QuickSpecName getQuickTestName()
    {
        return this.testName;
    }

    @Override
    public String getMethodName()
    {
        return getQuickTestName().getPrettyName();
    }
}
