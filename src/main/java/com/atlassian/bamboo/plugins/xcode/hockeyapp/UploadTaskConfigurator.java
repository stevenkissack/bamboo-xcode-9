package com.atlassian.bamboo.plugins.xcode.hockeyapp;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.struts.TextProvider;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class UploadTaskConfigurator extends AbstractTaskConfigurator
{
    private TextProvider textProvider;

    private static final String CFG_API_TOKEN = "apitoken";
    private static final String CFG_DOWNLOAD = "download";
    private static final String CFG_DSYM = "dsym";
    private static final String CFG_IPA = "ipa";
    private static final String CFG_NOTES = "notes";
    private static final String CFG_NOTES_TYPE = "notes_type";
    private static final String CFG_NOTIFY = "notify";
    private static final String CFG_TAGS = "tags";

    private static final List<String> FIELDS_TO_COPY = ImmutableList.of(
            CFG_API_TOKEN,
            CFG_DOWNLOAD,
            CFG_DSYM,
            CFG_IPA,
            CFG_NOTES,
            CFG_NOTES_TYPE,
            CFG_NOTIFY,
            CFG_TAGS
    );

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull final ActionParametersMap params, @Nullable final TaskDefinition previousTaskDefinition)
    {
        Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
        taskConfiguratorHelper.populateTaskConfigMapWithActionParameters(config, params, FIELDS_TO_COPY);

        return config;
    }

    @Override
    public void populateContextForCreate(@NotNull final Map<String, Object> context)
    {
        super.populateContextForCreate(context);
    }

    @Override
    public void populateContextForEdit(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition)
    {
        super.populateContextForEdit(context, taskDefinition);

        taskConfiguratorHelper.populateContextWithConfiguration(context, taskDefinition, FIELDS_TO_COPY);
    }

    @Override
    public void validate(@NotNull final ActionParametersMap params, @NotNull final ErrorCollection errorCollection)
    {
        super.validate(params, errorCollection);

        String apiToken = params.getString(CFG_API_TOKEN);
        if (StringUtils.isEmpty(apiToken))
        {
            errorCollection.addError(CFG_API_TOKEN, textProvider.getText("hockey.apitoken.error"));
        }
        
        String ipa = params.getString(CFG_IPA);
        if (StringUtils.isEmpty(ipa))
        {
            errorCollection.addError(CFG_IPA, textProvider.getText("hockey.ipa.error"));
        }
    }

    public void setTextProvider(final TextProvider textProvider)
    {
        this.textProvider = textProvider;
    }
}
