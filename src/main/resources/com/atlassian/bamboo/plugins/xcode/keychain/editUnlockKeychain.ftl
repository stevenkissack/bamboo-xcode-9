[@ww.textfield labelKey="keychain.name" name="keychain" required='true'/]

[#if mode == "create"]
    [@ww.password labelKey="keychain.password" name="password"/]
[#elseif mode == "edit"]
    [@ww.checkbox labelKey="keychain.changePassword" toggle='true' name='passwordChange'/]
    [@ui.bambooSection dependsOn='passwordChange' ]
        [@ww.password labelKey="keychain.password" name="newPassword"/]
    [/@ui.bambooSection]
[/#if]

[@ww.checkbox labelKey="keychain.setAsDefault" name="setAsDefaultKeychain"/]