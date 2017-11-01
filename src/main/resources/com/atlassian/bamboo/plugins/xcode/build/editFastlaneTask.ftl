[#-- @ftlvariable name="uiConfigSupport" type="com.atlassian.bamboo.ww2.actions.build.admin.create.UIConfigSupport" --]

[#assign addExecutableLink][@ui.displayAddExecutableInline executableKey="fastlane" /][/#assign]

[@ww.select cssClass="builderSelectWidget" labelKey="executable.type" required="true" name="label"
list=uiConfigSupport.getExecutableLabels("fastlane")
extraUtility=addExecutableLink /]

[@ww.textfield labelKey="fastlane.lane" name="lane" required="true" cssClass="long-field"/]
[@ww.textfield labelKey="fastlane.env" name="environmentVariables" cssClass="long-field"/]
[@ww.textfield labelKey="builder.common.sub" name="workingSubDirectory" cssClass="long-field"/]

<div class="edit-fastlane-task aui-help aui-help-text">
    <div class="aui-help-content">
        [@ww.text name="fastlane.junit.report.info" /]
        <code>
            output_types "junit"
        </code>
        <ul class="aui-nav-actions-list">
            <li>
            [@help.url pageKey="fastlane.junit.learn.more.link"][@ww.text name="fastlane.junit.learn.more"/][/@help.url]
            </li>
        </ul>
    </div>
</div>