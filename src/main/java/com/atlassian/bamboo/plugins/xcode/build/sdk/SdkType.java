package com.atlassian.bamboo.plugins.xcode.build.sdk;

/**
 * Type of Apple SDK
 */
public enum SdkType
{
    MACOSX("OS X SDK"),
    IOS("iOS SDK"),
    IOS_SIMULATOR("iOS Simulator SDK");

    private final String name;

    SdkType(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }
}
