package com.atlassian.bamboo.plugins.xcode.build;


import com.atlassian.bamboo.plugins.xcode.build.sdk.Sdk;
import com.atlassian.bamboo.plugins.xcode.build.sdk.SdkType;
import com.atlassian.bamboo.utils.Which;
import com.atlassian.util.concurrent.LazyReference;
import com.atlassian.utils.process.ExternalProcessBuilder;
import com.atlassian.utils.process.StringProcessHandler;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class XCodeBuild
{
    public static final String XCODEBUILD = "xcodebuild";
    public static final String XCODEBUILD_ARG_SHOWSDKS = "-showsdks";
    public static final String XCODEBUILD_ARG_CLEAN = "clean";

    private static final LazyReference<String> xcodeBuildPathLazyRef = new LazyReference<String>()
    {
        @Override
        protected String create() throws Exception {
            return Which.execute(XCODEBUILD);
        }
    };

    public static String getXcodebuildPath()
    {
        return xcodeBuildPathLazyRef.get();
    }

    /**
     * Lists all locally available {@link Sdk}s that <i>xcodebuild --showsdks</i> produces
     * @return sdks
     */
    public static Set<Sdk> findSdks()
    {
        final Set<Sdk> sdks = new LinkedHashSet<Sdk>();
        final StringProcessHandler processHandler = new StringProcessHandler();
        new ExternalProcessBuilder().command(Arrays.asList(getXcodebuildPath(), XCODEBUILD_ARG_SHOWSDKS)).handler(processHandler).build().execute();

        String output = processHandler.getOutput();

        SdkType currentSdkType = null;

        for (final String line : output.split("\n"))
        {

            if (currentSdkType == null)
            {
                for (SdkType sdkType : SdkType.values())
                {
                    if (line.startsWith(sdkType.getName()))
                    {
                        currentSdkType = sdkType;
                        break;
                    }
                }
            }
            else
            {
                if (line.startsWith("\t"))
                {
                    String[] split = StringUtils.split(line, "\t");
                    if (split.length != 2)
                    {
                        throw new IllegalStateException("Could not split line into two equal parts '" + line + "'");
                    }

                    String sdkName = StringUtils.trim(split[0]);
                    String sdkLabel = StringUtils.trim(StringUtils.remove(split[1], "-sdk "));
                    sdks.add(new Sdk(currentSdkType, sdkName, sdkLabel));
                }
                else
                {
                    currentSdkType = null;
                }
            }
        }

        return sdks;
    }
}
