package com.atlassian.bamboo.plugins.xcode.build;

import com.atlassian.bamboo.plugins.xcode.build.sdk.Sdk;
import com.atlassian.bamboo.v2.build.agent.capability.AbstractCapabilityTypeModule;
import com.atlassian.bamboo.v2.build.agent.capability.Capability;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityDefaultsHelper;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityImpl;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilitySet;
import com.atlassian.struts.TextProvider;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Map;

public class XCodeSDKCapabilityTypeModule extends AbstractCapabilityTypeModule implements CapabilityDefaultsHelper
{
    @SuppressWarnings("UnusedDeclaration")
    private static final Logger log = Logger.getLogger(XCodeSDKCapabilityTypeModule.class);
    // ------------------------------------------------------------------------------------------------------- Constants
    private static final String SDK = "sdk";
    private static final String NAME = "name";
    // ------------------------------------------------------------------------------------------------- Type Properties

    private TextProvider textProvider;

    // ---------------------------------------------------------------------------------------------------- Dependencies
    // ---------------------------------------------------------------------------------------------------- Constructors
    // ----------------------------------------------------------------------------------------------- Interface Methods

    @NotNull
    @Override
    public Map<String, String> validate(@NotNull final Map<String, String[]> params)
    {
        final Map<String, String> map = Maps.newHashMap();

        if (StringUtils.isEmpty(getParamValue(params, SDK)))
        {
            map.put(SDK, textProvider.getText("xcode.sdk.label.error"));
        }

        if (StringUtils.isEmpty(getParamValue(params, NAME)))
        {
            map.put(NAME, textProvider.getText("xcode.sdk.name.error"));
        }

        return map;
    }

    @NotNull
    @Override
    public Capability getCapability(@NotNull final Map<String, String[]> params)
    {
        final String sdkName =  getParamValue(params, NAME);
        final String sdkLabel = getParamValue(params, SDK);

        return createXcodeSdkCapability(sdkName, sdkLabel);
    }

    @NotNull
    @Override
    public String getLabel(@NotNull final String key)
    {
        return null;
    }

    @NotNull
    @Override
    public CapabilitySet addDefaultCapabilities(@NotNull final CapabilitySet capabilitySet)
    {
        String xcodebuildPath = XCodeBuild.getXcodebuildPath();
        if (StringUtils.isNotBlank(xcodebuildPath) && new File(xcodebuildPath).exists())
        {
            for (Sdk sdk : XCodeBuild.findSdks())
            {
                Capability capability = createXcodeSdkCapability(sdk.getName(), sdk.getLabel());
                capabilitySet.addCapability(capability);
            }
        }
        return capabilitySet;
    }

    // -------------------------------------------------------------------------------------------------- Action Methods
    // -------------------------------------------------------------------------------------------------- Public Methods
    // -------------------------------------------------------------------------------------- Basic Accessors / Mutators

    private Capability createXcodeSdkCapability(final String sdkName, final String sdkLabel)
    {
        return new CapabilityImpl(XCodeTaskType.XCODE_CAPABILITY_PREFIX + "." + sdkName, sdkLabel);
    }

    @Nullable
    private String getParamValue(@NotNull final Map<String, String[]> params, @NotNull String key)
    {
        final String[] param = params.get(key);
        if (param != null && param.length > 0)
        {
            return param[0];
        }
        return null;
    }

    public void setTextProvider(final TextProvider textProvider)
    {
        this.textProvider = textProvider;
    }
}
