package com.atlassian.bamboo.plugins.xcode.build;

import com.atlassian.bamboo.v2.build.agent.capability.AbstractExecutableCapabilityTypeModule;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityDefaultsHelper;
import com.google.common.collect.Lists;

import java.util.List;

public class FastlaneCapabilityDefaultsHelper extends AbstractExecutableCapabilityTypeModule
{
    public static final String FASTLANE_CAPABILITY_PREFIX = CapabilityDefaultsHelper.CAPABILITY_BUILDER_PREFIX + ".fastlane";
    public static final String FASTLANE_CAPABILITY = FASTLANE_CAPABILITY_PREFIX + ".fastlane";
    public static final String FASTLANE_EXECUTABLE = "fastlaneExecutable";

    private static final String CAPABILITY_TYPE_ERROR_UNDEFINED_EXECUTABLE = "fastlane.error.undefinedExecutable";

    @Override
    public String getMandatoryCapabilityKey()
    {
        return FASTLANE_CAPABILITY;
    }

    @Override
    public String getExecutableKey()
    {
        return FASTLANE_EXECUTABLE;
    }

    @Override
    public String getCapabilityUndefinedKey()
    {
        return CAPABILITY_TYPE_ERROR_UNDEFINED_EXECUTABLE;
    }

    @Override
    public List<String> getDefaultWindowPaths()
    {
        return Lists.newArrayList();
    }

    @Override
    public String getExecutableFilename()
    {
        return "fastlane";
    }
}
