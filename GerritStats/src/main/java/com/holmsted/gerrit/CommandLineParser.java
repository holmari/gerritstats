package com.holmsted.gerrit;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.annotation.Nonnull;

@SuppressWarnings("unused")
public class CommandLineParser {

    private static final String DEFAULT_OUTPUT_DIR = "out";

    public static class OutputConverter implements IStringConverter<Output> {
        @Override
        public Output convert(String value) {
            return Output.fromString(value);
        }
    }

    @Parameter(names = {"-f", "--file", "--files"},
            description = "Read output from comma-separated list of files or directories. "
                    + "The files must be in json format, created by GerritStatsDownloader.",
            required = true)
    @Nonnull
    private List<String> filenames = new ArrayList<>();

    @Parameter(names = "--exclude",
            description = "If specified, the comma-separated list of identities "
                    + "will be excluded from all generated statistics.")
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

    @Parameter(names = "--commit-patch-set-count-threshold",
            description = "If specified, all commit URLs exceeding the given patch set count will be listed "
            + "in the per-person data. If -1 is set, no listing is provided.")
    private int commitPatchSetCountThreshold = 5;

    @Parameter(names = {"-o", "--output-dir"},
            description = "The output will be generated into the given directory.")
    @Nonnull
    private String outputDir = DEFAULT_OUTPUT_DIR;

    @Parameter(names = {"-a", "--anonymize"},
            description = "Replace real data, like user name and email, with generated identities, "
                    + "and replace all other identifiable data with similarly generated names. "
                    + "Lorem ipsumizes all review comments. The statistic numbers are kept intact. "
                    + "Useful for demonstration purposes outside an organization.")
    private boolean anonymizeData;

    @Nonnull
    private final JCommander jCommander = new JCommander(this);

    public CommandLineParser() {
        URLClassLoader loader = (URLClassLoader) getClass().getClassLoader();
        URL url = loader.findResource("META-INF/MANIFEST.MF");
        try {
            Manifest manifest = new Manifest(url.openStream());
            Attributes attr = manifest.getMainAttributes();
            String mainClass = attr.getValue(Attributes.Name.MAIN_CLASS);
            jCommander.setProgramName(mainClass);
        } catch (IOException e) {
            // Ignore.
        }
    }

    public boolean parse(String[] args) {
        try {
            jCommander.parse(args);
        } catch (ParameterException e) {
            return false;
        }

        return !filenames.isEmpty();
    }

    @Nonnull
    private static String resolveOutputDir(@Nonnull String path) {
        if (path.startsWith("~" + File.separator)) {
            path = System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }

    @Nonnull
    public List<String> getFilenames() {
        return filenames;
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
    public String getOutputDir() {
        return outputDir;
    }

    public int getCommitPatchSetCountThreshold() {
        return commitPatchSetCountThreshold;
    }

    public boolean getAnonymizeData() {
        return anonymizeData;
    }

    public void printUsage() {
        jCommander.usage();
        System.out.println("Options preceded by an asterisk are required.");
    }
}
