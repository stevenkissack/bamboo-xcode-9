package com.atlassian.bamboo.plugins.xcode.export;

import com.atlassian.bamboo.plugins.xcode.build.FastlaneConfigurator;
import com.atlassian.bamboo.specs.api.util.EntityPropertiesBuilders;
import com.atlassian.bamboo.specs.api.validators.common.ValidationProblem;
import com.atlassian.bamboo.specs.builders.task.FastlaneTask;
import com.atlassian.bamboo.specs.model.task.FastlaneTaskProperties;
import com.atlassian.bamboo.task.TaskConfigConstants;
import com.atlassian.bamboo.task.TaskContainer;
import com.atlassian.bamboo.task.TaskDefinitionImpl;
import com.atlassian.bamboo.task.export.TaskValidationContext;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FastlaneTaskExporterTest {

    private static final String TASK_DESCRIPTION = "task description";
    private static final String ANDROID_TEST2 = "android test2";
    private static final String FASTLANE = "fastlane";
    private static final String ENV_VARIABLES = "HOME=/here";
    private static final String SUBDIRECTORY = "/home";
    private CapabilityContext capabilityContext;
    private FastlaneTaskExporter fastlaneTaskExporter;

    @Before
    public void setUp() throws Exception {
        capabilityContext = mock(CapabilityContext.class);
        fastlaneTaskExporter = new FastlaneTaskExporter(capabilityContext);
    }

    @Test
    public void testToTaskConfiguration() throws Exception {
        final FastlaneTask task = new FastlaneTask()
                .description(TASK_DESCRIPTION)
                .enabled(true)
                .lane(ANDROID_TEST2)
                .executableLabel(FASTLANE)
                .environmentVariables(ENV_VARIABLES)
                .workingSubdirectory(SUBDIRECTORY);
        final Map<String, String> taskConfiguration = fastlaneTaskExporter.toTaskConfiguration(mock(TaskContainer.class), EntityPropertiesBuilders.build(task));

        assertThat(taskConfiguration.get(FastlaneConfigurator.FASTLANE_LANE), equalTo(ANDROID_TEST2));
        assertThat(taskConfiguration.get(TaskConfigConstants.CFG_BUILDER_LABEL), equalTo(FASTLANE));
        assertThat(taskConfiguration.get(TaskConfigConstants.CFG_ENVIRONMENT_VARIABLES), equalTo(ENV_VARIABLES));
        assertThat(taskConfiguration.get(TaskConfigConstants.CFG_WORKING_SUBDIRECTORY), equalTo(SUBDIRECTORY));
    }

    @Test
    public void testToSpecsEntity() throws Exception {
        final TaskDefinitionImpl taskDefinition = new TaskDefinitionImpl(0, "", TASK_DESCRIPTION,
                ImmutableMap.of(FastlaneConfigurator.FASTLANE_LANE, ANDROID_TEST2,
                        TaskConfigConstants.CFG_BUILDER_LABEL, FASTLANE,
                        TaskConfigConstants.CFG_ENVIRONMENT_VARIABLES, ENV_VARIABLES,
                        TaskConfigConstants.CFG_WORKING_SUBDIRECTORY, SUBDIRECTORY));
        final FastlaneTask builder = ((FastlaneTask) fastlaneTaskExporter.toSpecsEntity(taskDefinition));
        final FastlaneTaskProperties task = EntityPropertiesBuilders.build(builder);

        assertThat(task.getLane(), equalTo(ANDROID_TEST2));
        assertThat(task.getExecutableLabel(), equalTo(FASTLANE));
        assertThat(task.getEnvironmentVariables(), equalTo(ENV_VARIABLES));
        assertThat(task.getWorkingSubdirectory(), equalTo(SUBDIRECTORY));
    }

    @Test
    public void testValidationOK() throws Exception {
        final FastlaneTask task = new FastlaneTask()
                .description(TASK_DESCRIPTION)
                .enabled(true)
                .lane(ANDROID_TEST2)
                .executableLabel(FASTLANE)
                .environmentVariables(ENV_VARIABLES)
                .workingSubdirectory(SUBDIRECTORY);
        when(capabilityContext.getCapabilityValue(any())).thenReturn("/bin/fastlane");

        final List<ValidationProblem> validationProblems = fastlaneTaskExporter.validate(mock(TaskValidationContext.class),
                EntityPropertiesBuilders.build(task));

        assertThat(validationProblems, empty());
    }

    @Test
    public void testValidationFail() throws Exception {
        final FastlaneTask task = new FastlaneTask()
                .description(TASK_DESCRIPTION)
                .enabled(true)
                .lane(ANDROID_TEST2)
                .executableLabel(FASTLANE)
                .environmentVariables(ENV_VARIABLES)
                .workingSubdirectory(SUBDIRECTORY);

        final List<ValidationProblem> validationProblems = fastlaneTaskExporter.validate(mock(TaskValidationContext.class),
                EntityPropertiesBuilders.build(task));

        assertThat(validationProblems, containsInAnyOrder(
                new ValidationProblem(FastlaneTaskExporter.FASTLANE_CONTEXT, "Executable fastlane doesn't exist.")
        ));
    }
}