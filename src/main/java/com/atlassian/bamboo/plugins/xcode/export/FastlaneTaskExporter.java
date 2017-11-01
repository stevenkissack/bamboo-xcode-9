package com.atlassian.bamboo.plugins.xcode.export;

import com.atlassian.bamboo.plugins.xcode.build.FastlaneCapabilityDefaultsHelper;
import com.atlassian.bamboo.plugins.xcode.build.FastlaneConfigurator;
import com.atlassian.bamboo.specs.api.builders.task.Task;
import com.atlassian.bamboo.specs.api.model.task.TaskProperties;
import com.atlassian.bamboo.specs.api.validators.common.ValidationContext;
import com.atlassian.bamboo.specs.api.validators.common.ValidationProblem;
import com.atlassian.bamboo.specs.builders.task.FastlaneTask;
import com.atlassian.bamboo.specs.model.task.FastlaneTaskProperties;
import com.atlassian.bamboo.task.TaskConfigConstants;
import com.atlassian.bamboo.task.TaskContainer;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.task.export.TaskDefinitionExporter;
import com.atlassian.bamboo.task.export.TaskValidationContext;
import com.atlassian.bamboo.util.Narrow;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FastlaneTaskExporter implements TaskDefinitionExporter {

    static final ValidationContext FASTLANE_CONTEXT = ValidationContext.of("Fastlane task");

    private final CapabilityContext capabilityContext;

    public FastlaneTaskExporter(CapabilityContext capabilityContext) {
        this.capabilityContext = capabilityContext;
    }

    @NotNull
    @Override
    public Map<String, String> toTaskConfiguration(@NotNull final TaskContainer taskContainer, @NotNull final TaskProperties taskProperties) {

        final FastlaneTaskProperties fastlaneTaskProperties = Narrow.downTo(taskProperties, FastlaneTaskProperties.class);
        if (fastlaneTaskProperties != null) {
            final Map<String, String> cfg = new HashMap<>();
            cfg.put(FastlaneConfigurator.FASTLANE_LANE, fastlaneTaskProperties.getLane());
            cfg.put(TaskConfigConstants.CFG_BUILDER_LABEL, fastlaneTaskProperties.getExecutableLabel());
            cfg.put(TaskConfigConstants.CFG_ENVIRONMENT_VARIABLES, fastlaneTaskProperties.getEnvironmentVariables());
            cfg.put(TaskConfigConstants.CFG_WORKING_SUBDIRECTORY, fastlaneTaskProperties.getWorkingSubdirectory());
            return cfg;
        }
        throw new IllegalStateException("Don't know how to import task properties of type: " + taskProperties.getClass().getName());
    }

    @NotNull
    @Override
    public Task toSpecsEntity(@NotNull final TaskDefinition taskDefinition) {
        final Map<String, String> configuration = taskDefinition.getConfiguration();

        final FastlaneTask fastlaneTask = new FastlaneTask();

        return fastlaneTask
                .lane(configuration.get(FastlaneConfigurator.FASTLANE_LANE))
                .executableLabel(configuration.get(TaskConfigConstants.CFG_BUILDER_LABEL))
                .environmentVariables(configuration.getOrDefault(TaskConfigConstants.CFG_ENVIRONMENT_VARIABLES, ""))
                .workingSubdirectory(configuration.getOrDefault(TaskConfigConstants.CFG_WORKING_SUBDIRECTORY, ""));
    }

    @Override
    public List<ValidationProblem> validate(@NotNull TaskValidationContext taskValidationContext,
                                            @NotNull TaskProperties taskProperties) {
        final List<ValidationProblem> result = new ArrayList<>();
        final FastlaneTaskProperties properties = Narrow.downTo(taskProperties, FastlaneTaskProperties.class);
        if (properties != null) {
            final String executableLabel = properties.getExecutableLabel();
            if (StringUtils.isEmpty(executableLabel)) {
                result.add(new ValidationProblem(FASTLANE_CONTEXT, "Executable can't be empty"));
            }
            if (StringUtils.isEmpty(properties.getLane())) {
                result.add(new ValidationProblem(FASTLANE_CONTEXT, "Lane can't be empty"));
            } else {
                final String capabilityKey = String.format("%s.%s", FastlaneCapabilityDefaultsHelper.FASTLANE_CAPABILITY_PREFIX, properties.getExecutableLabel());
                if (StringUtils.isBlank(capabilityContext.getCapabilityValue(capabilityKey))) {
                    result.add(new ValidationProblem(FASTLANE_CONTEXT, "Executable " + properties.getExecutableLabel() + " doesn't exist."));
                }
            }
        }
        return result;
    }
}
