package com.atlassian.bamboo.plugins.xcode.testflight;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.logger.ErrorUpdateHandler;
import com.atlassian.bamboo.task.CommonTaskContext;
import com.atlassian.bamboo.task.CommonTaskType;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.google.common.collect.Sets;
import com.opensymphony.webwork.dispatcher.json.JSONException;
import com.opensymphony.webwork.dispatcher.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

public class TestFlightTask implements CommonTaskType
{
    public static final String TEST_FLIGHT_URL = "http://testflightapp.com/api/builds.json";

    private final ErrorUpdateHandler errorUpdateHandler;

    public TestFlightTask(ErrorUpdateHandler errorUpdateHandler)
    {
        this.errorUpdateHandler = errorUpdateHandler;
    }

    @Override
    public TaskResult execute(@NotNull CommonTaskContext commonTaskContext) throws TaskException
    {

        final TaskResultBuilder taskResultBuilder = TaskResultBuilder.newBuilder(commonTaskContext);
        final ConfigurationMap config = commonTaskContext.getConfigurationMap();
        final BuildLogger buildLogger = commonTaskContext.getBuildLogger();

        final String file = config.get(TestFlightTaskConfigurator.FILE);
        final String apiToken = config.get(TestFlightTaskConfigurator.API_TOKEN);
        final String teamToken = config.get(TestFlightTaskConfigurator.TEAM_TOKEN);
        final String notes = config.get(TestFlightTaskConfigurator.NOTES);
        final boolean notify = config.getAsBoolean(TestFlightTaskConfigurator.NOTIFY);
        final String distributionLists = config.get(TestFlightTaskConfigurator.DISTRIBUTION_LISTS);

        final File pathToFile = new File(commonTaskContext.getWorkingDirectory(), file);

        final HttpClient client = new HttpClient();

        final PostMethod method = new PostMethod(TEST_FLIGHT_URL);

        final HttpMethodParams params  = new HttpMethodParams();

        final Set<Part> parts = Sets.newHashSet();
        try
        {
            parts.add(new FilePart("file", pathToFile));
        }
        catch (FileNotFoundException e)
        {
            buildLogger.addBuildLogEntry("Could not find specified IPA file '" + pathToFile.getAbsolutePath() + "'");
            return taskResultBuilder.failed().build();
        }

        String dsym = config.get(TestFlightTaskConfigurator.DSYM);
        if (StringUtils.isNotEmpty(dsym))
        {
            final File pathToDsym = new File(commonTaskContext.getWorkingDirectory(), dsym);
            try
            {
                parts.add(new FilePart("dsym", pathToDsym));
            }
            catch (FileNotFoundException e)
            {
                buildLogger.addBuildLogEntry("Could not find specified dSYM file '" + pathToDsym.getAbsolutePath() + "'");
                return taskResultBuilder.failed().build();
            }
        }

        parts.add(new StringPart("api_token", apiToken));
        parts.add(new StringPart("team_token", teamToken));
        parts.add(new StringPart("notes", notes));
        parts.add(new StringPart("notify", Boolean.toString(notify).toLowerCase()));
        parts.add(new StringPart("distribution_lists", distributionLists));

        final MultipartRequestEntity multipartRequestEntity = new MultipartRequestEntity(parts.toArray(new Part[parts.size()]), params);

        method.setRequestEntity(multipartRequestEntity);

        try
        {
            buildLogger.addBuildLogEntry("Uploading '" + pathToFile + "' to TestFlightApp...");
            final int status = client.executeMethod(method);
            if (status == HttpStatus.SC_OK)
            {
                buildLogger.addBuildLogEntry("Upload completed");

                final String body = method.getResponseBodyAsString();
                try
                {
                    final JSONObject jsonObject = new JSONObject(body);

                    buildLogger.addBuildLogEntry("Bundle version: " + jsonObject.getString("bundle_version"));
                    buildLogger.addBuildLogEntry("Install URL: " + jsonObject.getString("install_url"));
                    buildLogger.addBuildLogEntry("Config URL: " + jsonObject.getString("config_url"));
                    buildLogger.addBuildLogEntry("Created at: " + jsonObject.getString("created_at"));
                    buildLogger.addBuildLogEntry("Device Family: " + jsonObject.getString("device_family"));
                    buildLogger.addBuildLogEntry("Notify team members: " + jsonObject.getString("notify"));
                    buildLogger.addBuildLogEntry("Team: " + jsonObject.getString("team"));
                    buildLogger.addBuildLogEntry("Minimum OS Version: " + jsonObject.getString("minimum_os_version"));
                    buildLogger.addBuildLogEntry("Release Notes: " + jsonObject.getString("release_notes"));
                    buildLogger.addBuildLogEntry("Binary Size: " + jsonObject.getString("binary_size"));

                    taskResultBuilder.success();
                }
                catch (JSONException e)
                {
                    reportError("Could not parse body response as JSON:" + body, e, commonTaskContext);
                    taskResultBuilder.failedWithError();
                }
            }
            else
            {
                buildLogger.addErrorLogEntry("POST response to '" + TEST_FLIGHT_URL + "' was " + status);
                taskResultBuilder.failed();
            }
        }
        catch (IOException e)
        {
            reportError("Could not contact TestFlightApp.com: " + e.getMessage(), e, commonTaskContext);
            taskResultBuilder.failedWithError();
        }

        return taskResultBuilder.build();
    }

    private void reportError(String error, Throwable e, CommonTaskContext commonTaskContext)
    {
        errorUpdateHandler.recordError(commonTaskContext.getCommonContext().getResultKey(), "Could not contact TestFlightApp.com: " + e.getMessage(), e);
        commonTaskContext.getBuildLogger().addErrorLogEntry("Could not contact TestFlightApp.com: " + e.getMessage(), e);
    }
}
