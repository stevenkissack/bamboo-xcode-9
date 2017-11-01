package com.atlassian.bamboo.plugins.xcode.tests.xctest;

import org.junit.Assert;
import org.junit.Test;

public class QuickSpecTestResultsTest
{
    @Test
    public void testQuickSpecParsing()
    {
        Assert.assertTrue(QuickSpecTestResults.isQuickTestName("Display_IconTextFieldCell__with_Priority_field__has_icon_image_view_UsersjabernethybambooagenthomexmldatabuilddirMOBJMIJOB1JIRATestsViewModuleComponentFieldCellIconTextFieldCellTestsswift_44"));
        Assert.assertFalse(QuickSpecTestResults.isQuickTestName("Display_IconTextFieldCell__with_Priority_field__has_icon_image_view_UsersjabernethybambooagenthomexmldatabuilddirMOBJMIJOB1JIRATestsViewModuleComponentFieldCellIconTextFieldCellTestsswift"));
        Assert.assertFalse(QuickSpecTestResults.isQuickTestName("testCustomDateField"));
    }

    @Test
    public void testQuickNameParsing()
    {
        QuickSpecName testName = new QuickSpecName("Display_IconTextFieldCell__with_Priority_field__has_icon_image_view_UsersjabernethybambooagenthomexmldatabuilddirMOBJMIJOB1JIRATestsViewModuleComponentFieldCellIconTextFieldCellTestsswift_44");
        Assert.assertEquals("Describes correctly", "Display IconTextFieldCell", testName.describes);
        Assert.assertEquals("Behaves correctly", "with Priority field has icon image view", testName.behaviour);
    }

    @Test
    public void testQuickPrettyName()
    {
        QuickSpecTestResults results = new QuickSpecTestResults("IconTextFieldCellSpec", "Display_IconTextFieldCell__with_Priority_field__has_icon_image_view_UsersjabernethybambooagenthomexmldatabuilddirMOBJMIJOB1JIRATestsViewModuleComponentFieldCellIconTextFieldCellTestsswift_44", "10s");
        Assert.assertEquals("Pretty name is correct", "Display icon text field cell with priority field has icon image view", results.getMethodName());
    }

}
