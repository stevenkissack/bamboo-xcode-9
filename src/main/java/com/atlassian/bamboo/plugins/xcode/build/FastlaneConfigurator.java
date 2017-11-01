package com.atlassian.bamboo.plugins.xcode.build;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskConfigConstants;
import com.atlassian.bamboo.task.TaskConfiguratorHelper;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.task.TaskRequirementSupport;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.v2.build.agent.capability.Requirement;
import com.atlassian.bamboo.ww2.actions.build.admin.create.UIConfigSupport;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.opensymphony.xwork2.TextProvider;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class FastlaneConfigurator extends AbstractTaskConfigurator implements TaskRequirementSupport
{
    protected static final String FASTLANE_LANE_ERROR_KEY = "fastlane.lane.error";
    public static final String FASTLANE_LANE = "lane";
    private static final List<String> FIELDS_TO_COPY = ImmutableList.of(FASTLANE_LANE,
                                                                        TaskConfigConstants.CFG_WORKING_SUBDIRECTORY,
                                                                        TaskConfigConstants.CFG_BUILDER_LABEL,
                                                                        TaskConfigConstants.CFG_ENVIRONMENT_VARIABLES);
    private static final String CTX_UI_CONFIG_SUPPORT = "uiConfigSupport";

    private final TextProvider textProvider;
    private final UIConfigSupport uiConfigSupport;

    public FastlaneConfigurator(@ComponentImport TextProvider textProvider,
                                @ComponentImport UIConfigSupport uiConfigSupport,
                                @ComponentImport TaskConfiguratorHelper taskConfiguratorHelper)
    {
        this.uiConfigSupport = uiConfigSupport;
        this.taskConfiguratorHelper = taskConfiguratorHelper;
        this.textProvider = textProvider;
    }

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull ActionParametersMap params, @Nullable TaskDefinition previousTaskDefinition)
    {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
        taskConfiguratorHelper.populateTaskConfigMapWithActionParameters(config, params, FIELDS_TO_COPY);
        return config;
    }

    @Override
    public void validate(@NotNull ActionParametersMap params, @NotNull ErrorCollection errorCollection)
    {
        super.validate(params, errorCollection);
        taskConfiguratorHelper.validateBuilderLabel(params, errorCollection);


        final String lane = params.getString(FASTLANE_LANE);

        if (StringUtils.isBlank(lane))
        {
            errorCollection.addError(FASTLANE_LANE, textProvider.getText(FASTLANE_LANE_ERROR_KEY));
        }
    }

    @Override
    @NotNull
    public Set<Requirement> calculateRequirements(@NotNull TaskDefinition taskDefinition)
    {
        final Set<Requirement> requirements = Sets.newHashSet();
        taskConfiguratorHelper
                .addSystemRequirementFromConfiguration(requirements,
                                                       taskDefinition,
                                                       TaskConfigConstants.CFG_BUILDER_LABEL,
                                                       FastlaneCapabilityDefaultsHelper.FASTLANE_CAPABILITY_PREFIX);

        return requirements;
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition)
    {
        super.populateContextForEdit(context, taskDefinition);
        populateContextForAllOperations(context);
        taskConfiguratorHelper.populateContextWithConfiguration(context, taskDefinition, FIELDS_TO_COPY);
    }

    @Override
    public void populateContextForCreate(@NotNull Map<String, Object> context)
    {
        super.populateContextForCreate(context);
        populateContextForAllOperations(context);
    }

    private void populateContextForAllOperations(@NotNull Map<String, Object> context)
    {
        context.put(CTX_UI_CONFIG_SUPPORT, uiConfigSupport);
    }
}
