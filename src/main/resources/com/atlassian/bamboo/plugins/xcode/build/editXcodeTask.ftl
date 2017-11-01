[#assign addExecutableLink][@ui.displayAddExecutableInline executableKey='xcode' /][/#assign]

[@ww.textfield labelKey='xcode.sdk' name='label' cssClass="long-field" required='true'/]

[@ui.bambooSection titleKey="xcode.general"]
    [@ww.checkbox labelKey="xcode.buildall" name="alltargets"/]
    [@ui.bambooSection dependsOn='alltargets']
        [@ww.textfield labelKey="xcode.target" name="target" cssClass="long-field"/]
    [/@ui.bambooSection]
    [@ww.checkbox labelKey="xcode.clean" name="clean"/]
    [@ww.checkbox labelKey="xcode.ocunit" name="ocunit"/]
    [@ww.checkbox labelKey="xcode.xctest" name="xcunit"/]
[/@ui.bambooSection]

[@ui.bambooSection titleKey="xcode.project" collapsible=true isCollapsed=!(projectname?has_content || configuration?has_content)]
    [@ww.textfield labelKey='xcode.project' name='projectname' cssClass="long-field"/]
    [@ww.textfield labelKey="xcode.configuration" name="configuration" cssClass="long-field"/]
[/@ui.bambooSection]

[@ui.bambooSection titleKey='xcode.workspace' collapsible=true isCollapsed=!(workspacename?has_content || schemename?has_content)]
    [@ww.textfield labelKey='xcode.workspace' name='workspacename' cssClass="long-field"/]
    [@ww.textfield labelKey='xcode.scheme' name='schemename' cssClass="long-field"/]
[/@ui.bambooSection]

[@ui.bambooSection titleKey='xcode.ios' collapsible=true isCollapsed=!(run_in_ios_sim?has_content || workspacename?has_content || schemename?has_content)]
    [@ww.checkbox labelKey='xcode.run_in_ios_sim' name='run_in_ios_sim' toggle='true'/]
    [@ww.checkbox labelKey='xcode.reset_simulator' name='reset_simulator' toggle='true'/]
    [@ww.textfield labelKey='xcode.test_sim' name='test_sim' cssClass="long-field"  /]
    [@ww.checkbox labelKey='xcode.build_ipa' name='build_ipa' toggle='true'/]
    [@ui.bambooSection dependsOn='build_ipa' showOn='true']
        [@ww.textfield labelKey="xcode.app_path" name="app_path" required="true" cssClass="long-field"/]
        [@ww.textfield labelKey="xcode.identity" name="identity" cssClass="long-field"/]
        [@ww.textfield labelKey="xcode.bundle_identifier" name="bundle_identifier" required="true" cssClass="long-field"/]
        [@ww.textfield labelKey="xcode.provisioning_profile" name="provisioning_profile" required="true" cssClass="long-field"/]
        [@ww.textfield labelKey="xcode.development_team" name="development_team" required="true" cssClass="long-field"/]
        [@ww.select labelKey="xcode.distribution_method" name="distributionMethod" list="distributionMethods" required="true" toggle="true"/]
        [@ww.checkbox labelKey='xcode.upload_symbols' name='includeSymbols' toggle='true'/]
        [@ww.checkbox labelKey='xcode.upload_bitcode' name='includeBitcode' toggle='true'/]
    [/@ui.bambooSection]
[/@ui.bambooSection]

[@ui.bambooSection titleKey='xcode.advanced' collapsible=true isCollapsed=!(archname?has_content || customParameters?has_content || logfile?has_content || environmentVariables?has_content || workingSubDirectory?has_content )]
    [@ww.textfield labelKey='builder.common.env' name='environmentVariables' cssClass="long-field"  /]
    [@ww.textfield labelKey='builder.common.sub' name='workingSubDirectory' helpUri='working-directory.ftl' cssClass="long-field" /]
    [@ww.textfield labelKey='xcode.arch' name='archname' cssClass="long-field"/]
    [@ww.textfield labelKey='xcode.logfile' name='logfile' cssClass="long-field" /]
    [@ww.checkbox labelKey="xcode.cleanLogfile" name="cleanLogfile" /]
    [@ww.textfield labelKey='xcode.custom.parameters' name='customParameters' cssClass="long-field"  /]
[/@ui.bambooSection]

[#-- this is a patch to fix the inability for negative toggle --]
<script type="text/javascript">
    AJS.$('#alltargets').change(function() {
       var section = AJS.$('#fieldLabelArea_target').parent().parent();
       if (AJS.$(this).is(':checked')) {
           section.attr('style', 'display: none;');
       } else {
           section.attr('style', 'display: block;');
       }
    }).change();
</script>
