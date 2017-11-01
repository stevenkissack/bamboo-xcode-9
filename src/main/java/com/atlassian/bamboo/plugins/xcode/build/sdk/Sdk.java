package com.atlassian.bamboo.plugins.xcode.build.sdk;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Represents an SDK that Apple XCode is aware of
 */
public final class Sdk
{
    private final SdkType sdkType;
    private final String name;
    private final String label;

    public Sdk(SdkType sdkType, String name, String label)
    {
        this.sdkType = sdkType;
        this.name = name;
        this.label = label;
    }

    public SdkType getSdkType()
    {
        return sdkType;
    }

    public String getName()
    {
        return name;
    }

    public String getLabel()
    {
        return label;
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder()
                .append(sdkType)
                .append(name)
                .append(label)
                .hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == null)
        {
            return false;
        }
        if (o == this)
        {
            return true;
        }
        if (o.getClass() != getClass())
        {
            return false;
        }

        Sdk sdk = (Sdk)o;
        return new EqualsBuilder()
                .append(sdkType, sdk.sdkType)
                .append(name, name)
                .append(label, label)
                .isEquals();
    }
}
