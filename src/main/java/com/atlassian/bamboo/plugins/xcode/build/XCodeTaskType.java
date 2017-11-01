package com.atlassian.bamboo.plugins.xcode.build;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.build.logger.interceptors.ErrorMemorisingInterceptor;
import com.atlassian.bamboo.build.test.TestCollationService;
import com.atlassian.bamboo.build.test.TestCollectionResult;
import com.atlassian.bamboo.build.test.TestCollectionResultBuilder;
import com.atlassian.bamboo.build.test.TestReportProvider;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.plugins.xcode.tests.api.TestParserLogInterceptor;
import com.atlassian.bamboo.plugins.xcode.tests.ocunit.OCUnitTestParser;
import com.atlassian.bamboo.plugins.xcode.tests.xctest.XCTestParser;
import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.process.ExternalProcessBuilder;
import com.atlassian.bamboo.process.ProcessService;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskState;
import com.atlassian.bamboo.task.TaskType;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityDefaultsHelper;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XCodeTaskType implements TaskType
{
    public static final String XCODE_CAPABILITY_PREFIX = CapabilityDefaultsHelper.CAPABILITY_BUILDER_PREFIX + ".xcode";

    public static final String XCRUN_PATH = "/usr/bin/xcrun";
    public static final String XCTEST_PATH = "/usr/bin/xcodebuild";
    private static final String XCWORKSPACE_EXTENSION = ".xcworkspace";
    private static final String XCODEPROJ_EXTENSION =  ".xcodeproj";

    private final ProcessService processService;
    private final EnvironmentVariableAccessor environmentVariableAccessor;
    private final CapabilityContext capabilityContext;
    private final TestCollationService testCollationService;

    public XCodeTaskType(ProcessService processService, EnvironmentVariableAccessor environmentVariableAccessor, CapabilityContext capabilityContext, TestCollationService testCollationService)
    {
        this.processService = processService;
        this.environmentVariableAccessor = environmentVariableAccessor;
        this.capabilityContext = capabilityContext;
        this.testCollationService = testCollationService;
    }

    @NotNull
    public TaskResult execute(@NotNull TaskContext taskContext) throws TaskException
    {
        final TestParserLogInterceptor ocUnitLogInterceptor = new TestParserLogInterceptor(new OCUnitTestParser());
        final TestParserLogInterceptor xcTestParserLogInterceptor = new TestParserLogInterceptor(new XCTestParser());
        final ErrorMemorisingInterceptor errorLogInterceptor = ErrorMemorisingInterceptor.newInterceptor();
        final XCodeBuildLogInterceptor xcodebuildLogger = new XCodeBuildLogInterceptor();

        final TaskResultBuilder resultBuilder = TaskResultBuilder.newBuilder(taskContext);
        final BuildLogger buildLogger = taskContext.getBuildLogger();
        try
        {
            final ConfigurationMap configurationMap = taskContext.getConfigurationMap();
            String project = configurationMap.get(XCodeConfigurator.PROJECT);
            String workspace = configurationMap.get(XCodeConfigurator.WORKSPACE);
            final String scheme = configurationMap.get(XCodeConfigurator.SCHEME);
            final String arch = configurationMap.get(XCodeConfigurator.ARCH);
            final boolean shouldClean = configurationMap.getAsBoolean(XCodeConfigurator.CLEAN);
            final File workingDirectory = taskContext.getWorkingDirectory();

            final boolean buildAllTargets = configurationMap.getAsBoolean(XCodeConfigurator.ALL_TARGETS);
            final String target = configurationMap.get(XCodeConfigurator.TARGET);
            final String configuration = configurationMap.get(XCodeConfigurator.CONFIGURATION);
            final String sdk = configurationMap.get(XCodeConfigurator.SDK);
            final String customParameters = configurationMap.get(XCodeConfigurator.CUSTOM_PARAMETERS);
            final boolean parseOcUnitResults = configurationMap.getAsBoolean(XCodeConfigurator.OCUNIT);
            final boolean parseXcUnitResults = configurationMap.getAsBoolean(XCodeConfigurator.XCUNIT);

            final String runInIosSimStr = MoreObjects.firstNonNull(configurationMap.get(XCodeConfigurator.RUN_TESTS_IN_IOS_SIM), "true");
            final boolean runInSim = Boolean.parseBoolean(runInIosSimStr);
            final String testSim = "\'" + configurationMap.get(XCodeConfigurator.TEST_SIM) + "\'";
            final boolean resetSimulator = configurationMap.getAsBoolean(XCodeConfigurator.RESET_SIMULATOR);

            final String logfile = configurationMap.get(XCodeConfigurator.LOGFILE);
            final boolean cleanLogfile = configurationMap.getAsBoolean(XCodeConfigurator.CLEAN_LOGFILE);

            final List<String> arguments = Lists.newLinkedList();

            final String sdkLabel = capabilityContext.getCapabilityValue(XCODE_CAPABILITY_PREFIX  + "." + sdk);
            Preconditions.checkNotNull(sdkLabel, "The Xcode SDK capability is missing");

            arguments.add(XCodeBuild.getXcodebuildPath());

            if (StringUtils.isNotEmpty(logfile))
            {
                String logfilePath = workingDirectory.getPath() + "/" + logfile;

                try
                {
                    xcodebuildLogger.open(logfilePath, !cleanLogfile);
                    buildLogger.getInterceptorStack().add(xcodebuildLogger);
                }
                catch (IOException e)
                {
                    buildLogger.addBuildLogEntry("Cannot open logfile: " + e);
                }
            }

            if (shouldClean)
            {
                arguments.add("clean");
                arguments.add("build");
            }

            arguments.add("-sdk");
            arguments.add(sdkLabel);

            addBuildConfigurationArgs(arguments, project, workspace, scheme);

            if (StringUtils.isNotEmpty(arch))
            {
                arguments.add("-arch");
                arguments.add(arch);
            }

            if (StringUtils.isEmpty(scheme))
            {
                if (buildAllTargets)
                {
                    arguments.add("-alltargets");
                }
                else if (StringUtils.isNotEmpty(target))
                {
                    arguments.add("-target");
                    arguments.add(target);
                }
            }

            if (StringUtils.isNotEmpty(configuration))
            {
                arguments.add("-configuration");
                arguments.add(configuration);
            }

            if (StringUtils.isNotEmpty(customParameters))
            {
                addArgumentsSplit(arguments, customParameters);
            }

            if (resetSimulator)
            {
                arguments.add("RESET_IOS_SIMULATOR=YES");
            }

            final Map<String, String> environment = environmentVariableAccessor.splitEnvironmentAssignments(configurationMap.get(XCodeConfigurator.ENVIRONMENT));

            if (parseOcUnitResults)
            {
                buildLogger.getInterceptorStack().add(ocUnitLogInterceptor);
            }

            buildLogger.getInterceptorStack().add(errorLogInterceptor);

            resultBuilder.checkReturnCode(processService.executeExternalProcess(
                    taskContext,
                    new ExternalProcessBuilder()
                            .command(arguments)
                            .env(environment)
                            .workingDirectory(workingDirectory)));

            if (resultBuilder.getTaskState() == TaskState.SUCCESS && parseXcUnitResults)
            {
                buildLogger.getInterceptorStack().add(xcTestParserLogInterceptor);
                final List<String> testCommand = Lists.newArrayList(XCTEST_PATH, "test");
                addBuildConfigurationArgs(testCommand, project, workspace, scheme);
                if (runInSim)
                {
                    testCommand.add("-destination");
                    testCommand.add(testSim);
                }

                if (StringUtils.isNotEmpty(customParameters))
                {
                    addArgumentsSplit(testCommand, customParameters);
                }

                resultBuilder.checkReturnCode(processService.executeExternalProcess(
                        taskContext,
                        new ExternalProcessBuilder()
                                .workingDirectory(workingDirectory)
                                .command(testCommand)));

                testCollationService.collateTestResults(taskContext, new TestReportProvider() {
                    @NotNull
                    @Override
                    public TestCollectionResult getTestCollectionResult()
                    {
                        return new TestCollectionResultBuilder()
                                .addFailedTestResults(Sets.union(ocUnitLogInterceptor.getFailingTestResults(), xcTestParserLogInterceptor.getFailingTestResults()))
                                .addSuccessfulTestResults(Sets.union(ocUnitLogInterceptor.getSuccessfulTestResults(), xcTestParserLogInterceptor.getSuccessfulTestResults()))
                                .build();
                    }
                });

                resultBuilder.checkTestFailures();
            }

            if (resultBuilder.getTaskState() == TaskState.SUCCESS && configurationMap.getAsBoolean(XCodeConfigurator.BUILD_IPA))
            {
                final String appPath = configurationMap.get(XCodeConfigurator.APP_PATH);
                final String identity = configurationMap.get(XCodeConfigurator.IDENTITY);
                final String devTeam = configurationMap.get(XCodeConfigurator.DEVELOPMENT_TEAM);
                final String distribMethod = configurationMap.get(XCodeConfigurator.DISTRIBUTION_METHOD);
                final boolean uploadSymbols = configurationMap.getAsBoolean(XCodeConfigurator.INCLUDE_SYMBOLS);
                final boolean uploadBitcode = configurationMap.getAsBoolean(XCodeConfigurator.INCLUDE_BITCODE);
                final String provisioningProfile = configurationMap.get(XCodeConfigurator.PROVISIONING_PROFILE);
                final String bundleIdentifier = configurationMap.get(XCodeConfigurator.BUNDLE_IDENTIFIER);

                final File appFile = new File(appPath);

                final String xcarchivePath = FilenameUtils.removeExtension(appFile.getAbsolutePath()) + ".xcarchive";
                final String exportOptionsPlist = FilenameUtils.getFullPath(appFile.getAbsolutePath()) + "exportOptions.plist";
                final String outputPath = FilenameUtils.removeExtension(appFile.getAbsolutePath()) + ".ipa";

                final List<String> archiveCommand = Lists.newArrayList(XCRUN_PATH,
                                                                       "-sdk", sdkLabel,
                                                                       "xcodebuild",
                                                                       "-workspace", workspace,
                                                                       "-scheme", scheme, 
                                                                       "-sdk", "iphoneos");
                if (StringUtils.isNotEmpty(configuration))
                {
                    archiveCommand.add("-configuration");
                    archiveCommand.add(configuration);
                }
                
                archiveCommand.add("archive");
                archiveCommand.add("-archivePath");
                archiveCommand.add(xcarchivePath);

                if (StringUtils.isNotEmpty(identity)) { 
                    archiveCommand.add("CODE_SIGN_IDENTITY=" + identity);
                }

                // TODO: Not sure if this is needed anymore
                archiveCommand.add("PROVISIONING_PROFILE=" + provisioningProfile);

                final List<String> exportCommand = Lists.newArrayList(XCRUN_PATH, 
                                                                        "-sdk", sdkLabel, 
                                                                        "xcodebuild",
                                                                        "-exportArchive", 
                                                                        "-archivePath", xcarchivePath, 
                                                                        "-exportOptionsPlist", exportOptionsPlist,
                                                                        "-exportPath", outputPath);
                
                resultBuilder.checkReturnCode(processService.executeExternalProcess( 
                    taskContext, 
                    new ExternalProcessBuilder()
                        .workingDirectory(workingDirectory)
                        .command(archiveCommand)));

                if (resultBuilder.getTaskState() == TaskState.SUCCESS) 
                {
                    buildLogger.addBuildLogEntry("xcarchive created at'" + xcarchivePath + "'");
                    
                    writeExportOptionsPlist(exportOptionsPlist, devTeam, distribMethod, uploadSymbols, uploadBitcode, provisioningProfile, bundleIdentifier);

                    resultBuilder.checkReturnCode(processService.executeExternalProcess(
                            taskContext,
                            new ExternalProcessBuilder()
                                    .workingDirectory(workingDirectory)
                                    .command(exportCommand)));

                    if (resultBuilder.getTaskState() == TaskState.SUCCESS)
                    {
                        buildLogger.addBuildLogEntry("IPA created at '" + outputPath + "'");
                    }
                }

                //TODO: make this checkable via the Sdk type
                if (StringUtils.isNotEmpty(sdk) && StringUtils.contains(sdk, "simulator"))
                {
                    processService.executeExternalProcess(
                            taskContext,
                            new ExternalProcessBuilder()
                                    .command(Lists.newArrayList("osascript", "-e", "'tell app \"iPhone Simulator\" to quit'")));
                }
            }

            return resultBuilder.build();
        }
        finally
        {
            buildLogger.getInterceptorStack().remove(ocUnitLogInterceptor);
            buildLogger.getInterceptorStack().remove(xcodebuildLogger);
            xcodebuildLogger.close();
        }
    }

    private static final Pattern PAT_ARGS = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");

    private static void addArgumentsSplit(final List<String> arguments, final String customParameters)
    {
        final Matcher matcher = PAT_ARGS.matcher(customParameters);
        while (matcher.find()) {
            arguments.add(matcher.group());
        }
    }

    private static void addBuildConfigurationArgs(final  List<String> arguments, String project, String workspace, final String scheme)
    {
        if (StringUtils.isNotEmpty(project))
        {
            arguments.add("-project");

            if (!StringUtils.endsWith(project, XCODEPROJ_EXTENSION))
            {
                project = project + XCODEPROJ_EXTENSION;
            }

            arguments.add(project);
        }

        if (StringUtils.isNotEmpty(workspace))
        {
            arguments.add("-workspace");

            if (!StringUtils.endsWith(workspace, XCWORKSPACE_EXTENSION))
            {
                workspace = workspace + XCWORKSPACE_EXTENSION;
            }

            arguments.add(workspace);
        }

        if (StringUtils.isNotEmpty(scheme))
        {
            arguments.add("-scheme");
            arguments.add(scheme);
        }
    }

    private static void writeExportOptionsPlist(String plistPath, String devTeam, String distribMethod, boolean uploadSymbols, boolean uploadBitcode, String provisioningProfile, String bundleIdentifier) throws TaskException 
    {
        StringBuilder builder = new StringBuilder(); 

        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        builder.append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">");
        builder.append("<plist version=\"1.0\">");
        builder.append("<dict>");
        
        builder.append("<key>method</key>");
        builder.append("<string>").append(distribMethod).append("</string>");
        
        builder.append("<key>teamID</key>");
        builder.append("<string>").append(devTeam).append("</string>");
        
        builder.append("<key>uploadSymbols</key>");
        builder.append(uploadSymbols ? "<true/>" : "<false/>"); 
        
        builder.append("<key>uploadBitcode</key>");
        builder.append(uploadBitcode ? "<true/>" : "<false/>");
        
        builder.append("<key>provisioningProfiles</key>");
        builder.append("<dict>");
            builder.append("<key>").append(bundleIdentifier).append("</key>");
            builder.append("<string>").append(provisioningProfile).append("</string>");
        builder.append("</dict>");

        builder.append("</dict>");
        builder.append("</plist>");

        try { 
            Files.write(Paths.get(plistPath), builder.toString().getBytes());
        } catch(IOException e) { 
            throw new TaskException("failed to generate and/or write xcodebuild export options plist", e);
        }
    }
}
