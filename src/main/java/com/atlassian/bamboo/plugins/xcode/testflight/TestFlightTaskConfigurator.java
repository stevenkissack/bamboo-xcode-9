package com.atlassian.bamboo.plugins.xcode.testflight;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public class TestFlightTaskConfigurator extends AbstractTaskConfigurator
{
    public static final String FILE = "file";
    public static final String API_TOKEN = "api_token";
    public static final String TEAM_TOKEN = "team_token";
    public static final String NOTES = "notes";
    public static final String NOTIFY = "notify";
    public static final String REPLACE = "replace";
    public static final String DSYM = "dsym";
    public static final String DISTRIBUTION_LISTS = "distribution_lists";

    private static final Set<String> FIELDS = Sets.newHashSet(FILE, DSYM, API_TOKEN, TEAM_TOKEN, NOTES, REPLACE, NOTIFY, DISTRIBUTION_LISTS);

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull final ActionParametersMap params, @Nullable final TaskDefinition previousTaskDefinition)
    {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
        taskConfiguratorHelper.populateTaskConfigMapWithActionParameters(config, params, FIELDS);
        return config;
    }

    @Override
    public void populateContextForCreate(@NotNull final Map<String, Object> context)
    {
        super.populateContextForCreate(context);
        context.put(NOTES, "Released with Atlassian Bamboo");
    }

    @Override
    public void populateContextForEdit(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition)
    {
        super.populateContextForEdit(context, taskDefinition);
        taskConfiguratorHelper.populateContextWithConfiguration(context, taskDefinition, FIELDS);
    }

    @Override
    public void validate(@NotNull final ActionParametersMap params, @NotNull final ErrorCollection errorCollection)
    {
        super.validate(params, errorCollection);

        if (StringUtils.isEmpty(params.getString(API_TOKEN)))
        {
            errorCollection.addError(API_TOKEN, getI18nBean().getText("testflight.api_token.error"));
        }

        if (StringUtils.isEmpty(params.getString(TEAM_TOKEN)))
        {
            errorCollection.addError(TEAM_TOKEN, getI18nBean().getText("testflight.team_token.error"));
        }

        if (StringUtils.isEmpty(params.getString(FILE)))
        {
            errorCollection.addError(FILE, getI18nBean().getText("testflight.file.error"));
        }

        if (StringUtils.isEmpty(params.getString(NOTES)))
        {
            errorCollection.addError(NOTES, getI18nBean().getText("testflight.notes.error"));
        }
    }
}
