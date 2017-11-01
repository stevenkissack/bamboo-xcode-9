package com.atlassian.bamboo.plugins.xcode.build;

import com.atlassian.bamboo.build.BuildDefinitionManager;
import com.atlassian.bamboo.deployments.environments.service.EnvironmentService;
import com.atlassian.bamboo.task.TaskConfigConstants;
import com.atlassian.bamboo.task.TaskConfiguratorHelper;
import com.atlassian.bamboo.task.TaskConfiguratorHelperImpl;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.task.TaskDefinitionImpl;
import com.atlassian.bamboo.utils.error.SimpleErrorCollection;
import com.atlassian.bamboo.v2.build.agent.capability.Requirement;
import com.atlassian.bamboo.v2.build.agent.capability.RequirementImpl;
import com.atlassian.bamboo.webwork.util.ActionParametersMapImpl;
import com.atlassian.bamboo.ww2.actions.build.admin.create.UIConfigSupport;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.opensymphony.xwork2.TextProvider;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.atlassian.bamboo.plugins.xcode.build.FastlaneConfigurator.FASTLANE_LANE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnitParamsRunner.class)
public class FastlaneConfiguratorTest
{
    private static final Requirement REQUIREMENT = new RequirementImpl(FastlaneCapabilityDefaultsHelper.FASTLANE_CAPABILITY_PREFIX + ".testFastlane",
                                                                       true,
                                                                       ".*");
    private static final String LANE_REQUIRED = "lane required";
    private static final String LABEL_REQUIRED = "label required";

    private FastlaneConfigurator fastlaneConfigurator;

    @Before
    public void setUp() throws Exception
    {
        TextProvider textProvider = mock(TextProvider.class);
        when(textProvider.getText(FastlaneConfigurator.FASTLANE_LANE_ERROR_KEY)).thenReturn(LANE_REQUIRED);
        when(textProvider.getText("task.generic.validate.builderLabel.mandatory")).thenReturn(LABEL_REQUIRED);

        final UIConfigSupport uiConfigSupport = mock(UIConfigSupport.class);
        final TaskConfiguratorHelper taskConfigurationHelper = new TaskConfiguratorHelperImpl(mock(BuildDefinitionManager.class), mock(EnvironmentService.class), textProvider);

        fastlaneConfigurator = new FastlaneConfigurator(textProvider, uiConfigSupport, taskConfigurationHelper);
    }

    @Test
    public void testConfiguratorAddsFastlaneRequirements()
    {
        //having
        TaskDefinitionImpl taskDefinition = new TaskDefinitionImpl(-1, "", "",
                                                                   ImmutableMap.of(TaskConfigConstants.CFG_BUILDER_LABEL, "testFastlane"));

        //when
        Set<Requirement> requirements = fastlaneConfigurator.calculateRequirements(taskDefinition);

        //then
        assertThat(requirements, containsInAnyOrder(REQUIREMENT));
    }

    @Test
    public void testGenerateTaskConfigMap()
    {
        //having
        Map<String, String> paramMap = ImmutableMap.of(
                FASTLANE_LANE, "lane",
                TaskConfigConstants.CFG_WORKING_SUB_DIRECTORY, "directory",
                TaskConfigConstants.CFG_BUILDER_LABEL, "label",
                TaskConfigConstants.CFG_ENVIRONMENT_VARIABLES, "env");
        ActionParametersMapImpl parameters = new ActionParametersMapImpl(paramMap);

        //when
        Map<String, String> result = fastlaneConfigurator.generateTaskConfigMap(parameters, mock(TaskDefinition.class));

        //then
        assertThat("expected 4 properties in the map", result.size(), equalTo(4));
        assertThat(result, allOf(hasEntry(FASTLANE_LANE, "lane"),
                                 hasEntry(TaskConfigConstants.CFG_WORKING_SUB_DIRECTORY, "directory"),
                                 hasEntry(TaskConfigConstants.CFG_BUILDER_LABEL, "label"),
                                 hasEntry(TaskConfigConstants.CFG_ENVIRONMENT_VARIABLES, "env")));
    }

    @Test
    @Parameters(method = "validationParams")
    public void testValidation(Map<String, String> params, List<String> errors) throws Exception
    {
        //having
        ActionParametersMapImpl parameters = new ActionParametersMapImpl(params);
        SimpleErrorCollection errorCollection = new SimpleErrorCollection();

        //when
        fastlaneConfigurator.validate(parameters, errorCollection);

        //then
        assertThat(errorCollection.getAllErrorMessages(), containsInAnyOrder(errors.toArray()));
    }

    @SuppressWarnings("unused") //used by testValidation Parameters annotation
    private Object[] validationParams()
    {
        return new Object[]{
                new Object[]{
                        ImmutableMap.of(),
                        Lists.newArrayList(LABEL_REQUIRED, LANE_REQUIRED)
                },
                new Object[]{
                        ImmutableMap.of(TaskConfigConstants.CFG_BUILDER_LABEL, "", FASTLANE_LANE, ""),
                        Lists.newArrayList(LABEL_REQUIRED, LANE_REQUIRED)
                },
                new Object[]{
                        ImmutableMap.of(TaskConfigConstants.CFG_BUILDER_LABEL, "not empty"),
                        Lists.newArrayList(LANE_REQUIRED)
                },
                new Object[]{
                        ImmutableMap.of(FASTLANE_LANE, "not empty"),
                        Lists.newArrayList(LABEL_REQUIRED)
                },
                new Object[]{
                        ImmutableMap.of(TaskConfigConstants.CFG_BUILDER_LABEL, "not empty", FASTLANE_LANE, "not empty"),
                        Lists.newArrayList()
                }

        };
    }
}
