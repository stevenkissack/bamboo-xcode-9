package com.atlassian.bamboo.plugins.xcode.keychain;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.build.logger.interceptors.ErrorMemorisingInterceptor;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.process.ExternalProcessBuilder;
import com.atlassian.bamboo.process.ProcessService;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskState;
import com.atlassian.bamboo.task.TaskType;
import com.atlassian.utils.process.ExternalProcess;
import com.atlassian.utils.process.StringProcessHandler;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

public class UnlockKeychainTaskType implements TaskType
{
    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String SECURITY_UTIL_PATH = "/usr/bin/security";
    private static final String KEYCHAIN_EXT = ".keychain";

    // ------------------------------------------------------------------------------------------------- Type Properties
    // ---------------------------------------------------------------------------------------------------- Dependencies
    // ---------------------------------------------------------------------------------------------------- Constructors
    // ----------------------------------------------------------------------------------------------- Interface Methods

    private final ProcessService processService;

    public UnlockKeychainTaskType(final ProcessService processService)
    {
        this.processService = processService;
    }

    @NotNull
    @Override
    public TaskResult execute(@NotNull final TaskContext taskContext) throws TaskException
    {
        final TaskResultBuilder resultBuilder = TaskResultBuilder.newBuilder(taskContext);
        final ErrorMemorisingInterceptor errorLines = ErrorMemorisingInterceptor.newInterceptor();
        final BuildLogger buildLogger = taskContext.getBuildLogger();
        buildLogger.getInterceptorStack().add(errorLines);

        final ConfigurationMap configurationMap = taskContext.getConfigurationMap();
        final File workingDirectory = taskContext.getWorkingDirectory();

        final String password = configurationMap.get(UnlockKeychainConfigurator.PASSWORD);
        String keychain = configurationMap.get(UnlockKeychainConfigurator.KEYCHAIN);

        // Need to ensure if we are using a non-fully qualified keychain name that we append .keychain to the keychain name for the `security` util.
        if (!new File(keychain).exists() && !keychain.endsWith(KEYCHAIN_EXT))
        {
            keychain = keychain + KEYCHAIN_EXT;
        }

        // Set as default keychain.
        if (configurationMap.getAsBoolean(UnlockKeychainConfigurator.SET_AS_DEFAULT_KEYCHAIN))
        {
            final List<String> command = Lists.newArrayList(SECURITY_UTIL_PATH,
                                                            "default-keychain",
                                                            "-s", keychain);

            resultBuilder.checkReturnCode(processService.executeExternalProcess(
                    taskContext,
                    new ExternalProcessBuilder().
                            command(command).
                            workingDirectory(workingDirectory)));

            if (resultBuilder.getTaskState() != TaskState.SUCCESS)
            {
                return resultBuilder.build();
            }
        }

        // Unlock the keychain. Avoid revealing the password by not recording the command in the log.
        buildLogger.addBuildLogEntry("Unlocking keychain '" + keychain + "'");

        final List<String> command = Lists.newArrayList(SECURITY_UTIL_PATH,
                                                        "unlock-keychain",
                                                        "-p", password,
                                                        keychain);

        final StringProcessHandler handler = new StringProcessHandler();
        handler.setThrowOnNonZeroExit(false);
        ExternalProcess externalProcess = new com.atlassian.utils.process.ExternalProcessBuilder().command(command).handler(handler).build();
        externalProcess.execute();

        final String error = handler.getError();
        if (StringUtils.isNotEmpty(error))
        {
            buildLogger.addErrorLogEntry(error);
        }

        return TaskResultBuilder.newBuilder(taskContext).checkReturnCode(externalProcess).build();
    }

    // -------------------------------------------------------------------------------------------------- Action Methods
    // -------------------------------------------------------------------------------------------------- Public Methods
    // -------------------------------------------------------------------------------------- Basic Accessors / Mutators
}
