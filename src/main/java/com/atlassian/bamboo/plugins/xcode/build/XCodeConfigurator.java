package com.atlassian.bamboo.plugins.xcode.build;

import com.atlassian.bamboo.build.Job;
import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.BuildTaskRequirementSupport;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.task.TaskTestResultsSupport;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.v2.build.agent.capability.Requirement;
import com.atlassian.bamboo.v2.build.agent.capability.RequirementImpl;
import com.atlassian.bamboo.ww2.actions.build.admin.create.UIConfigSupport;
import com.atlassian.struts.TextProvider;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class XCodeConfigurator extends AbstractTaskConfigurator implements BuildTaskRequirementSupport, TaskTestResultsSupport
{
    public static final String TARGET = "target";
    public static final String ALL_TARGETS = "alltargets";
    public static final String CONFIGURATION = "configuration";
    public static final String SDK = "label";
    public static final String CLEAN = "clean";
    public static final String PROJECT = "projectname";
    public static final String WORKSPACE = "workspacename";
    public static final String SCHEME = "schemename";
    public static final String ARCH = "archname";
    public static final String WORKING_SUB_DIR = "workingSubDirectory";
    public static final String LOGFILE = "logfile";
    public static final String CLEAN_LOGFILE = "cleanLogfile";
    public static final String CUSTOM_PARAMETERS = "customParameters";
    public static final String ENVIRONMENT = "environmentVariables";
    public static final String OCUNIT = "ocunit";
    public static final String XCUNIT = "xcunit";
    public static final String BUILD_IPA = "build_ipa";
    public static final String APP_PATH = "app_path";
    public static final String IDENTITY = "identity";
    public static final String PROVISIONING_PROFILE = "provisioning_profile";
    public static final String BUNDLE_IDENTIFIER = "bundle_identifier";
    public static final String RUN_TESTS_IN_IOS_SIM = "run_in_ios_sim";
    public static final String TEST_SIM = "test_sim";
    public static final String DEVELOPMENT_TEAM = "development_team";
    public static final String RESET_SIMULATOR = "reset_simulator";
    public static final String DISTRIBUTION_METHOD = "distributionMethod";
    public static final String DISTRIBUTION_METHODS = "distributionMethods";
    public static final String DEFAULT_DISTRIBUTION_METHOD = "development";
    public static final String INCLUDE_SYMBOLS = "includeSymbols";
    public static final String INCLUDE_BITCODE = "includeBitcode";

    private static final String CTX_UI_CONFIG_BEAN = "uiConfigBean";

    private static final Set<String> FIELDS_TO_COPY = ImmutableSet.<String>builder()
            .add(TARGET)
            .add(ALL_TARGETS)
            .add(CONFIGURATION)
            .add(PROJECT)
            .add(WORKSPACE)
            .add(SCHEME)
            .add(ARCH)
            .add(SDK)
            .add(CLEAN)
            .add(WORKING_SUB_DIR)
            .add(LOGFILE)
            .add(CLEAN_LOGFILE)
            .add(CUSTOM_PARAMETERS)
            .add(ENVIRONMENT)
            .add(OCUNIT)
            .add(XCUNIT)
            .add(BUILD_IPA)
            .add(IDENTITY)
            .add(PROVISIONING_PROFILE) 
            .add(APP_PATH)
            .add(BUNDLE_IDENTIFIER)
            .add(RUN_TESTS_IN_IOS_SIM)
            .add(TEST_SIM)
            .add(DEVELOPMENT_TEAM)
            .add(RESET_SIMULATOR)
            .add(DISTRIBUTION_METHOD)
            .add(INCLUDE_SYMBOLS) 
            .add(INCLUDE_BITCODE)
            .build();

    protected TextProvider textProvider;
    protected UIConfigSupport uiConfigSupport;

    @Override
    public void populateContextForEdit(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition)
    {
        super.populateContextForEdit(context, taskDefinition);
        populateContextForAllOperations(context);
        taskConfiguratorHelper.populateContextWithConfiguration(context, taskDefinition, FIELDS_TO_COPY);
    }

    @Override
    public void populateContextForCreate(@NotNull final Map<String, Object> context)
    {
        super.populateContextForCreate(context);
        populateContextForAllOperations(context);
        context.put(DISTRIBUTION_METHOD, DEFAULT_DISTRIBUTION_METHOD);
        context.put(ALL_TARGETS, true);
        context.put(CLEAN, true);
    }

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull final ActionParametersMap params, @Nullable final TaskDefinition previousTaskDefinition)
    {
        final Map<String, String> map = super.generateTaskConfigMap(params, previousTaskDefinition);
        taskConfiguratorHelper.populateTaskConfigMapWithActionParameters(map, params, FIELDS_TO_COPY);
        return map;
    }

    @Override
    public void validate(@NotNull final ActionParametersMap params, @NotNull final ErrorCollection errorCollection)
    {
        super.validate(params, errorCollection);

        if (StringUtils.isEmpty(params.getString(SDK)))
        {
            errorCollection.addError(SDK, textProvider.getText("xcode.sdk.error"));
        }

        if (params.getBoolean(BUILD_IPA))
        {
            if (StringUtils.isEmpty(params.getString(APP_PATH)))
            {
                errorCollection.addError(APP_PATH, textProvider.getText("xcode.app_path.error"));
            }

            if (StringUtils.isEmpty(params.getString(DEVELOPMENT_TEAM))) 
            {
                errorCollection.addError(DEVELOPMENT_TEAM, textProvider.getText("xcode.development_team.error"));
            }
        }

        if (StringUtils.isNotEmpty(params.getString(WORKSPACE)) && StringUtils.isEmpty(params.getString(SCHEME)))
        {
            errorCollection.addError(SCHEME, textProvider.getText("xcode.scheme.error"));
        }
        
        if (StringUtils.isEmpty(params.getString(DISTRIBUTION_METHOD)))
        {
            errorCollection.addError(DISTRIBUTION_METHOD, textProvider.getText("xcode.distribution_method.error"));
        }

        if (StringUtils.isEmpty(params.getString(BUNDLE_IDENTIFIER)))
        {
            errorCollection.addError(BUNDLE_IDENTIFIER, textProvider.getText("xcode.bundle_identifier.error"));
        }

        if (StringUtils.isEmpty(params.getString(PROVISIONING_PROFILE)))
        {
            errorCollection.addError(PROVISIONING_PROFILE, textProvider.getText("xcode.provisioning_profile.error"));
        }
    }

    @NotNull
    @Override
    public Set<Requirement> calculateRequirements(@NotNull TaskDefinition taskDefinition, @NotNull Job job)
    {
        final String sdk = taskDefinition.getConfiguration().get(SDK);
        Preconditions.checkNotNull(sdk, "No SDK was selected");
        return Sets.<Requirement>newHashSet(new RequirementImpl(XCodeTaskType.XCODE_CAPABILITY_PREFIX + "." + sdk, true, ".*"));
    }

    private void populateContextForAllOperations(@NotNull Map<String, Object> context)
    {
        context.put(DISTRIBUTION_METHODS, getDistributionMap());
        context.put(CTX_UI_CONFIG_BEAN, uiConfigSupport);
    }

    public boolean taskProducesTestResults(@NotNull final TaskDefinition taskDefinition)
    {
        final String ocunit = taskDefinition.getConfiguration().get(OCUNIT);
        final String xcunit = taskDefinition.getConfiguration().get(XCUNIT);
        return Boolean.parseBoolean(ocunit) || Boolean.parseBoolean(xcunit);
    }

    public void setTextProvider(final TextProvider textProvider)
    {
        this.textProvider = textProvider;
    }

    public void setUiConfigSupport(final UIConfigSupport uiConfigSupport)
    {
        this.uiConfigSupport = uiConfigSupport;
    }

    private Map<String, String> getDistributionMap() 
    {
        Map<String, String> distributionMap = new LinkedHashMap<String, String>();
        distributionMap.put("app-store", "Application Store"); 
        distributionMap.put("enterprise", "Enterprise");
        distributionMap.put("ad-hoc", "Ad-Hoc");
        distributionMap.put("development", "Development");
        return distributionMap;
    }
}
