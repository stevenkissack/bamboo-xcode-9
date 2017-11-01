package com.atlassian.bamboo.plugins.xcode.keychain;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.ww2.actions.build.admin.create.UIConfigSupport;
import com.atlassian.struts.TextProvider;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public class UnlockKeychainConfigurator extends AbstractTaskConfigurator
{
    // ------------------------------------------------------------------------------------------------------- Constants

    public static final String PASSWORD = "password";
    public static final String KEYCHAIN = "keychain";
    private static final String PASSWORD_CHANGE = "passwordChange";
    private static final String NEW_PASSWORD = "newPassword";
    static final String SET_AS_DEFAULT_KEYCHAIN = "setAsDefaultKeychain";

    private static final String CTX_UI_CONFIG_BEAN = "uiConfigBean";


    private static final Set<String> FIELDS_TO_COPY = ImmutableSet.<String>builder()
            .add(PASSWORD)
            .add(KEYCHAIN)
            .add(SET_AS_DEFAULT_KEYCHAIN).build();

    private static final String INITIAL_KEYCHAIN_NAME = "login";

    // ------------------------------------------------------------------------------------------------- Type Properties
    // ---------------------------------------------------------------------------------------------------- Dependencies

    protected TextProvider textProvider;
    protected UIConfigSupport uiConfigSupport;

    // ---------------------------------------------------------------------------------------------------- Constructors
    // ----------------------------------------------------------------------------------------------- Interface Methods

    @Override
    public void populateContextForEdit(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition)
    {
        super.populateContextForEdit(context, taskDefinition);
        populateContextForAllOperations(context);
        taskConfiguratorHelper.populateContextWithConfiguration(context, taskDefinition, FIELDS_TO_COPY);
        context.put("mode", "edit");
    }

    @Override
    public void populateContextForCreate(@NotNull final Map<String, Object> context)
    {
        super.populateContextForCreate(context);
        populateContextForAllOperations(context);
        context.put("mode", "create");
        context.put(KEYCHAIN, INITIAL_KEYCHAIN_NAME);
        context.put(SET_AS_DEFAULT_KEYCHAIN, Boolean.TRUE);
    }

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull final ActionParametersMap params, @Nullable final TaskDefinition previousTaskDefinition)
    {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
        taskConfiguratorHelper.populateTaskConfigMapWithActionParameters(config, params, FIELDS_TO_COPY);

        String passwordChange = params.getString(PASSWORD_CHANGE);
        if ("true".equals(passwordChange))
        {
            final String password = params.getString(NEW_PASSWORD);
            config.put(PASSWORD, password);
        }
        else if (previousTaskDefinition != null)
        {
            config.put(PASSWORD, previousTaskDefinition.getConfiguration().get(PASSWORD));
        }
        else
        {
            final String password = params.getString(PASSWORD);
            config.put(PASSWORD, password);
        }

        return config;
    }

    @Override
    public void validate(@NotNull final ActionParametersMap params, @NotNull final ErrorCollection errorCollection)
    {
        super.validate(params, errorCollection);

        if (StringUtils.isEmpty(params.getString(KEYCHAIN)))
        {
            errorCollection.addError(KEYCHAIN, textProvider.getText("keychain.name.error"));
        }
    }

    // -------------------------------------------------------------------------------------------------- Action Methods

    private void populateContextForAllOperations(@NotNull Map<String, Object> context)
    {
        context.put(CTX_UI_CONFIG_BEAN, uiConfigSupport);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods
    // -------------------------------------------------------------------------------------- Basic Accessors / Mutators

    public void setTextProvider(final TextProvider textProvider)
    {
        this.textProvider = textProvider;
    }

    public void setUiConfigSupport(UIConfigSupport uiConfigSupport)
    {
        this.uiConfigSupport = uiConfigSupport;
    }

}
