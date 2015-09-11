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

@SuppressWarnings("unused")
public class CommandLineParser {

    private static final String DEFAULT_OUTPUT_DIR = "out";

    public static class ServerAndPort {
        private String serverName;
        private int serverPort;

        @Override
        public String toString() {
            return "None. Required if no filename is specified.";
        }

        public static class Converter implements IStringConverter<ServerAndPort> {
            @Override
            public ServerAndPort convert(String value) {
                ServerAndPort result = new ServerAndPort();
                result.serverName = value;
                int portSeparator = result.serverName.indexOf(':');
                if (portSeparator != -1) {
                    result.serverPort = Integer.valueOf(result.serverName.substring(portSeparator + 1));
                    result.serverName = result.serverName.substring(0, portSeparator);
                }
                return result;
            }
        }
    }

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
            description = "read output from file. The file is expected to be a json file as output by this tool.")
    private String filename;

    @Parameter(names = "--server",
            description = "read output from Gerrit server url and given port. If port is omitted, defaults to 29418.",
            arity = 1,
            converter = ServerAndPort.Converter.class)
    @Nonnull
    private final ServerAndPort serverAndPort = new ServerAndPort();

    @Parameter(names = "--project", description = "specifies the Gerrit project from which to retrieve stats. "
            + "If omitted, stats will be retrieved from all projects.")
    private String projectName;

    @Parameter(names = "--query-output-file", description = "if specified, the output of the query"
            + "will be written into  the specified file, to be used later with e.g. --file switch.")
    private String outputFile;

    @Parameter(names = "--limit", description = "The number of commits which to retrieve from the server. "
            + "If omitted, stats will be retrieved until no further records are available.")
    private int limit = GerritStatReader.NO_COMMIT_LIMIT;

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
            description = "If specified, the output will be generated into the given directory.")
    @Nonnull
    private String outputDir = DEFAULT_OUTPUT_DIR;

    @Nonnull
    private final JCommander jCommander = new JCommander(this);

    public boolean parse(String[] args) {
        try {
            jCommander.parse(args);
        } catch (ParameterException e) {
            e.printStackTrace();
            return false;
        }

        return (filename != null || serverAndPort.serverName != null);
    }

    @Nonnull
    private static String resolveOutputDir(@Nonnull String path) {
        if (path.startsWith("~" + File.separator)) {
            path = System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }

    @Nullable
    public String getFilename() {
        return filename;
    }

    @Nullable
    public String getOutputFile() {
        return outputFile;
    }

    @Nullable
    public String getServerName() {
        return serverAndPort.serverName;
    }

    @Nullable
    public String getProjectName() {
        return projectName;
    }

    public int getServerPort() {
        return serverAndPort.serverPort;
    }

    public int getCommitLimit() {
        return limit;
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
