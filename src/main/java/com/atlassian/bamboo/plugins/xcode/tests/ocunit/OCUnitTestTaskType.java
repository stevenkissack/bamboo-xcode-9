package com.atlassian.bamboo.plugins.xcode.tests.ocunit;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.build.test.TestCollationService;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.plugins.xcode.tests.api.XcodeTestParser;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class OCUnitTestTaskType implements TaskType {

    private final TestCollationService testCollationService;

    public OCUnitTestTaskType(final TestCollationService testCollationService)
    {
        this.testCollationService = testCollationService;
    }

    @NotNull
    @Override
    public TaskResult execute(@NotNull TaskContext taskContext) throws TaskException
    {
        final TaskResultBuilder taskResultBuilder = TaskResultBuilder.newBuilder(taskContext);
        final ConfigurationMap config = taskContext.getConfigurationMap();
        final BuildLogger buildLogger = taskContext.getBuildLogger();

        final String logFileStr = config.get(OCUnitTestTaskConfigurator.LOG_FILE);
        final File logFile = new File(logFileStr);

        if (!logFile.exists()) {
            buildLogger.addBuildLogEntry("Could not find specified log file '" + logFile.getAbsolutePath() + "'");
            return taskResultBuilder.failed().build();
        }

        XcodeTestParser parser = new OCUnitTestParser();
        InputStream is = null;

        try
        {
            is = new FileInputStream(logFile);

            for (String line : IOUtils.readLines(is))
            {
                parser.processLine(line);
            }

            taskContext.getBuildContext().getBuildResult().setTestResults(Sets.newHashSet(parser.getSuccessfulTestResults()), Sets.newHashSet(parser.getFailingTestResults()));
            taskResultBuilder.checkTestFailures();
        }
        catch (FileNotFoundException e)
        {
            buildLogger.addErrorLogEntry("Could not find specified log file '" + logFile.getAbsolutePath() + "'", e);
            taskResultBuilder.failedWithError();
        }
        catch (IOException e)
        {
            buildLogger.addErrorLogEntry("I/O Exception when accessing log file '" + logFile.getAbsolutePath() + "'", e);
            taskResultBuilder.failedWithError();
        }
        finally
        {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        return taskResultBuilder.build();
    }

}
