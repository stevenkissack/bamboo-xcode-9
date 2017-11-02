This is a fix for XCode 9 code signing error:

```
Error Domain=IDEProvisioningErrorDomain Code=9 ""myapp.app" requires a provisioning profile." UserInfo={NSLocalizedDescription="myapp.app" requires a provisioning profile., NSLocalizedRecoverySuggestion=Add a profile to the "provisioningProfiles" dictionary in your Export Options property list.}
```

I've submitted this fix to Atlassian as [BAM-18804](https://jira.atlassian.com/browse/BAM-18804)

The fix consists of:
- Requiring bundle ID & provisioning profile UUID
- Changing the UI to enforce this
- adding these values in the exportOptions.plist file

You can either build from source following Atlassians plugin documentation (atlas), or download the `jar` file and manually upload it into your Bamboo server instance.

Best of luck! 