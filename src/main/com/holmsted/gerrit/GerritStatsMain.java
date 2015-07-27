package com.holmsted.gerrit;

import com.holmsted.gerrit.formatters.PerPersonDataFormatter;
import com.holmsted.gerrit.formatters.ReviewerCsvFormatter;

import java.util.List;

import file.FileReader;
import file.FileWriter;

public class GerritStatsMain {

    public static void main(String[] args) {
        CommandLineParser parser = new CommandLineParser();
        if (!parser.parse(args)) {
            System.err.println("Reads and outputs Gerrit statistics.");
            System.err.println("Usage: gerritstats.jar [--file file]|"
                    + "[--server server:port [--project project] [--limit n] [--output-file file]]");
            System.err.println("      [--branches master,feature1]");
            System.err.println("      [--include joe@root.com,jeff@foo.bar..,xyzzy@inter.net]");
            System.err.println("      [--exclude joe@root.com,jeff@foo.bar..,xyzzy@inter.net]");
            System.err.println("      [--output-format plain|csv]");
            System.err.println();
            System.err.println(" --file file: read output from file");
            System.err.println(String.format(" --server url:port: read output from Gerrit server url and given port."
                    + " If port is omitted, defaults to %s.",
                    GerritStatReader.GERRIT_DEFAULT_PORT));
            System.err.println(" --project project: specifies the Gerrit project from which to retrieve stats. "
                    + "If omitted, stats will be retrieved from all projects.");
            System.err.println(" --limit n: The number of commits which to retrieve from the server. "
                    + "If omitted, stats will be retrieved until no further records are available.");
            System.err.println(" --output-file file: if specified, the output of the query will be written into "
                    + "the specified file, to be used later with e.g. --file switch.");
            System.err.println(" --branches branches: if specified, only the comma-separated list of branches "
                    + "will be included for analysis. If omitted, all available branches will be inspected.");
            System.err.println(" --include list-of-people: if specified, only the comma-separated list of identities "
                    + " will be included in generated statistics. If both --include and --exclude are "
                    + "specified, --exclude is ignored.");
            System.err.println(" --exclude list-of-people: if specified, the comma-separated list of identities "
                    + " will be excluded from all generated statistics.");
            System.err.println(" --output-type: if specified, the output will be created in either plain " +
                    "or csv format. CSV is more suitable for processing with other tools.");
            System.exit(1);
            return;
        }

        CommitFilter filter = new CommitFilter();
        filter.setIncludeEmptyEmails(false);
        filter.setIncludedEmails(parser.getIncludedEmails());
        filter.setExcludedEmails(parser.getExcludedEmails());
        filter.setIncludeBranches(parser.getIncludeBranches());

        String serverName = parser.getServerName();
        String data;
        if (serverName != null) {
            GerritStatReader reader = GerritStatReader.fromCommandLine(serverName, parser.getServerPort());
            reader.setCommitLimit(parser.getCommitLimit());
            String projectName = parser.getProjectName();
            if (projectName != null) {
                reader.setProjectName(projectName);
            }
            data = reader.readData();

            String outputFile = parser.getOutputFile();
            if (outputFile != null) {
                FileWriter.writeFile(outputFile, data);
            }
        } else {
            data = FileReader.readFile(parser.getFilename());
        }

        GerritStatParser commitDataParser = new GerritStatParser();
        List<Commit> commits = commitDataParser.parseCommits(data);

        // TODO how to decide whether to output this?
        ReviewerCsvFormatter formatter = new ReviewerCsvFormatter(filter, parser.getOutputType());
        //System.out.print(formatter.invoke(commits).toString());

        PerPersonDataFormatter perPersonFormatter = new PerPersonDataFormatter(filter, parser.getOutputType());
        System.out.print(perPersonFormatter.invoke(commits));
    }
}
