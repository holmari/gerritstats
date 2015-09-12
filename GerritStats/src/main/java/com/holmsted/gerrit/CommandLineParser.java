package com.holmsted.gerrit;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("unused")
public class CommandLineParser {

    private static final String DEFAULT_OUTPUT_DIR = "out";

    public static class OutputTypeConverter implements IStringConverter<OutputType> {
        @Override
        public OutputType convert(String value) {
            return OutputType.fromTypeString(value);
        }
    }

    public static class OutputConverter implements IStringConverter<Output> {
        @Override
        public Output convert(String value) {
            return Output.fromString(value);
        }
    }

    @Parameter(names = "--file",
            description = "Read output from file. The file must be a json file created by GerritStatsDownloader.",
            required = true)
    private String filename;

    @Parameter(names = "--project",
            description = "specifies the Gerrit project from which to retrieve stats. "
            + "If omitted, stats will be retrieved from all projects.")
    private String projectName;

    @Parameter(names = "--exclude",
            description = "If specified, the comma-separated list of identities "
                    + " will be excluded from all generated statistics.")
    @Nonnull
    private final List<String> excludedEmails = new ArrayList<>();
    @Parameter(names = "--branches",
            description = "If specified, only the comma-separated list of branches "
            + "will be included for analysis. If omitted, all available branches will be inspected.")
    @Nonnull
    private final List<String> includedBranches = new ArrayList<>();

    @Parameter(names = "--include",
            description = "If specified, only the comma-separated list of identities "
            + "will be included in generated statistics. If both --include and --exclude are "
            + "specified, --exclude is ignored.")
    @Nonnull
    private final List<String> includedEmails = new ArrayList<>();

    @Parameter(names = "--output-type",
            description = "If specified, the output will be provided in the specified format. Defaults to HTML.",
            arity = 1,
            converter = OutputTypeConverter.class)
    @Nonnull
    private OutputType outputType = OutputType.HTML;

    @Parameter(names = "--commit-patch-set-count-threshold",
            description = "If specified, all commit URLs exceeding the given patch set count will be listed "
            + "in the per-person data. If -1 is set, no listing is provided.")
    private int commitPatchSetCountThreshold = 5;

    @Parameter(names = "--output",
            description = "If specified, the output will be either a list of all review comments in CSV format, "
            + "or a per-person data set.",
            arity = 1,
            converter = OutputConverter.class)
    private Output output = Output.PER_PERSON_DATA;

    @Parameter(names = "--output-dir",
            description = "The output will be generated into the given directory.")
    @Nonnull
    private String outputDir = DEFAULT_OUTPUT_DIR;

    @Nonnull
    private final JCommander jCommander = new JCommander(this);

    public boolean parse(String[] args) {
        try {
            jCommander.parse(args);
        } catch (ParameterException e) {
            return false;
        }

        return filename != null;
    }

    @Nonnull
    private static String resolveOutputDir(@Nonnull String path) {
        if (path.startsWith("~" + File.separator)) {
            path = System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }

    @Nonnull
    public String getFilename() {
        return checkNotNull(filename);
    }

    @Nullable
    public String getProjectName() {
        return projectName;
    }

    @Nonnull
    public List<String> getExcludedEmails() {
        return excludedEmails;
    }

    @Nonnull
    public List<String> getIncludedEmails() {
        return includedEmails;
    }

    @Nonnull
    public List<String> getIncludeBranches() {
        return includedBranches;
    }

    @Nonnull
    public OutputType getOutputType() {
        return outputType;
    }

    @Nonnull
    public Output getOutput() {
        return output;
    }

    @Nonnull
    public String getOutputDir() {
        return outputDir;
    }

    public int getCommitPatchSetCountThreshold() {
        return commitPatchSetCountThreshold;
    }

    public void printUsage() {
        jCommander.usage();
    }
}
