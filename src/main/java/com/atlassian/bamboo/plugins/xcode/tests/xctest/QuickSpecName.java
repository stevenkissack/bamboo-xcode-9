package com.atlassian.bamboo.plugins.xcode.tests.xctest;

import org.apache.commons.lang.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class QuickSpecName
{
    private static final Pattern quickSpecNamePattern = Pattern.compile("^(.+?)__(.+)(_+.+_\\d+)$");

    // The xctest method name without the file reference at the end
    final String specKey;

    final String describes;
    final String behaviour;

    public static boolean isValidName(String testName)
    {
        return newTestNameMatcher(testName).matches();
    }

    private static Matcher newTestNameMatcher(String testName)
    {
        return quickSpecNamePattern.matcher(testName);
    }

    public QuickSpecName(String methodName)
    {
        final Matcher nameMatcher = newTestNameMatcher(methodName);
        if (!nameMatcher.matches())
        {
            throw new IllegalArgumentException("'" + methodName + "' is not a valid Quick spec name");
        }

        this.specKey = nameMatcher.group(1) + "__" + nameMatcher.group(2).trim();
        this.describes = squeezeUnderscoresToSpace(nameMatcher.group(1));
        this.behaviour = squeezeUnderscoresToSpace(nameMatcher.group(2));
    }

    public String getPrettyName()
    {
        String wordsSeparated = this.specKey.replaceAll("\\p{Lu}", " $0").replace('_', ' ').replaceAll("\\s+", " ").toLowerCase().trim();
        return StringUtils.isBlank(wordsSeparated) ? this.specKey : firstCharacterToUpper(wordsSeparated);
    }

    private String firstCharacterToUpper(String str)
    {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String squeezeUnderscoresToSpace(String str)
    {
        return str.replaceAll("_+", " ");
    }
}
