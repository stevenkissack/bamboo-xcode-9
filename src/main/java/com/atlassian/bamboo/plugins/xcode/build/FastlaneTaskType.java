package com.atlassian.bamboo.plugins.xcode.build;

import com.atlassian.bamboo.ResultKey;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.process.BambooProcessHandler;
import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.process.ErrorStreamToBuildLoggerOutputHandler;
import com.atlassian.bamboo.process.ExternalProcessViaBatchBuilder;
import com.atlassian.bamboo.process.StreamToBuildLoggerOutputHandler;
import com.atlassian.bamboo.task.CommonTaskContext;
import com.atlassian.bamboo.task.CommonTaskType;
import com.atlassian.bamboo.task.TaskConfigConstants;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.utils.process.ExternalProcess;
import com.atlassian.utils.process.OutputHandler;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class FastlaneTaskType implements CommonTaskType
{
    private final CapabilityContext capabilityContext;
    private final EnvironmentVariableAccessor environmentVariableAccessor;

    public FastlaneTaskType(@ComponentImport CapabilityContext capabilityContext,
                            @ComponentImport EnvironmentVariableAccessor environmentVariableAccessor)
    {
        this.capabilityContext = capabilityContext;
        this.environmentVariableAccessor = environmentVariableAccessor;
    }

    @NotNull
    public TaskResult execute(@NotNull CommonTaskContext taskContext) throws TaskException
    {
        try
        {
            final ConfigurationMap configurationMap = taskContext.getConfigurationMap();

            final String builderLabel = configurationMap.get(TaskConfigConstants.CFG_BUILDER_LABEL);
            Preconditions.checkArgument(StringUtils.isNotBlank(builderLabel), "Builder label is not defined");
            final String capabilityKey = String.format("%s.%s", FastlaneCapabilityDefaultsHelper.FASTLANE_CAPABILITY_PREFIX, builderLabel);
            final String fastlanePath = Preconditions.checkNotNull(capabilityContext.getCapabilityValue(capabilityKey),
                                                                   String.format("Path is not defined for [%s.%s]",
                                                                                 FastlaneCapabilityDefaultsHelper.FASTLANE_CAPABILITY_PREFIX, builderLabel));

            final ExternalProcessViaBatchBuilder externalProcessViaBatchBuilder = new ExternalProcessViaBatchBuilder();
            externalProcessViaBatchBuilder.command(Lists.newArrayList(fastlanePath, configurationMap.get(FastlaneConfigurator.FASTLANE_LANE)),
                                                   taskContext.getWorkingDirectory());
            //ExternalProcess passes argument with spaces as a byte array which will be handled by Ruby and Fastlane
            //as a single argument, therefore Fastlane won't process it correctly. It fails to execute any lane with
            //a cryptic message.
            externalProcessViaBatchBuilder.forceBatch();

            final ResultKey resultKey = taskContext.getCommonContext().getResultKey();
            final OutputHandler stdOut = new StreamToBuildLoggerOutputHandler(taskContext.getBuildLogger(), resultKey);
            final OutputHandler errOut = new ErrorStreamToBuildLoggerOutputHandler(taskContext.getBuildLogger(), resultKey);
            externalProcessViaBatchBuilder.handler(new BambooProcessHandler(stdOut, errOut));
            externalProcessViaBatchBuilder.env(createEnvironmentMap(configurationMap));

            final ExternalProcess fastlaneProcess = externalProcessViaBatchBuilder.build();
            fastlaneProcess.execute();

            return TaskResultBuilder
                    .newBuilder(taskContext)
                    .checkReturnCode(fastlaneProcess)
                    .build();
        }
        catch (Exception e)
        {
            throw new TaskException("Could not execute Fastlane task", e);
        }
    }

    private Map<String, String> createEnvironmentMap(ConfigurationMap configurationMap)
    {
        final String environmentVariables = StringUtils.defaultString(configurationMap.get(TaskConfigConstants.CFG_ENVIRONMENT_VARIABLES));
        return environmentVariableAccessor.splitEnvironmentAssignments(environmentVariables, false);
    }
}
