package io.wadoon.keyci;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.*;

import javax.inject.Inject;

public abstract class KeYVerifyTask extends SourceTask {
    private final JavaExec exec = new JavaExec() {
    };

    private ObjectFactory objectFactory = getObjectFactory();

    /**
     *
     */
    private final RegularFileProperty junitXmlOutput = objectFactory.fileProperty();

    @OutputFile
    public RegularFileProperty getJunitXmlOutput() {
        return junitXmlOutput;
    }


    /**
     *
     */
    private final Property<Boolean> useColor = objectFactory.property(Boolean.class);

    /**
     * try to measure proof coverage
     */
    private final Property<Boolean> enableMeasuring = objectFactory.property(Boolean.class);

    /**
     * defines additional key files to be included
     */
    @InputFiles
    private final SourceDirectorySet includes = objectFactory.sourceDirectorySet("java", "java");

    /**
     * maximal amount of steps in auto-mode [default:10000]
     */
    private final Property<Integer> autoModeStep = objectFactory.property(Integer.class);

    {
        autoModeStep.set(10000);
    }

    /**
     * verbose
     */
    private final Property<Boolean> verbose = objectFactory.property(Boolean.class);

    private final Property<Boolean> debug = objectFactory.property(Boolean.class);

    /**
     * if set, the contract names are read from the proof file contents
     */
    @Input
    private final Property<Boolean> readContractNames = objectFactory.property(Boolean.class);

    /**
     * "If set, only contracts with a proof script or proof are replayed."
     */
    @Input
    private final Property<Boolean> disableAutoMode = objectFactory.property(Boolean.class);

    /**
     * if set, JSON files with proof statistics are written
     */
    @OutputFile
    private final RegularFileProperty statisticsFile = objectFactory.fileProperty();

    /**
     * Normally, the `statisticsFile' is overriden by the ci-too.
     * If set the statistics are appended to the JSON data structure.
     */
    @Input
    private final Property<Boolean> appendStatistics = objectFactory.property(Boolean.class);

    /**
     * "skipping the proof reloading, scripts execution and auto mode." +
     * " Useful for finding the contract names"
     */
    @Input
    private final Property<Boolean> dryRun = objectFactory.property(Boolean.class);

    @InputFiles
    private final ConfigurableFileCollection classpath = objectFactory.fileCollection();

    @InputDirectory
    private final DirectoryProperty bootClassPath = objectFactory.directoryProperty();

    /**
     * whitelist contracts by their names
     */
    @Input
    private final SetProperty<String> onlyContracts = objectFactory.setProperty(String.class);

    /**
     * help = "forbid contracts by their name
     */
    @Input
    private final SetProperty<String> forbidContracts = objectFactory.setProperty(String.class);

    @SkipWhenEmpty
    @IgnoreEmptyDirectories
    @

            PathSensitive(PathSensitivity.RELATIVE)

    @InputFiles
    private final ConfigurableFileCollection inputFile = objectFactory.fileCollection();

    /**
     * folders to look for proofs and script files
     */
    @InputFiles
    private final ConfigurableFileCollection proofPath = objectFactory.fileCollection();


    @TaskAction
    public void exec() {
        var config = getProject().getConfigurations().getByName("key");
        config.resolve();
    }

    @Inject
    protected ObjectFactory getObjectFactory() {
        throw new UnsupportedOperationException();
    }
}


