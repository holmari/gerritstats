package com.holmsted.gerrit;

import com.holmsted.gerrit.processors.perperson.PerPersonDataProcessor;
import com.holmsted.gerrit.processors.reviewers.ReviewerProcessor;

import java.util.List;

import file.FileReader;
import file.FileWriter;

public class GerritStatsMain {

    public static void main(String[] args) {
        CommandLineParser commandLine = new CommandLineParser();
        if (!commandLine.parse(args)) {
            System.err.println("Reads and outputs Gerrit statistics.");
            System.err.println("Usage: GerritStats.jar [--file file]|"
                    + "[--server server:port [--project project] [--limit n] [--query-output-file file]]");
            System.err.println("      [--branches master,feature1]");
            System.err.println("      [--include joe@root.com,jeff@foo.bar..,xyzzy@inter.net]");
            System.err.println("      [--exclude joe@root.com,jeff@foo.bar..,xyzzy@inter.net]");
            System.err.println("      [--output-type plain|csv|html]");
            System.err.println("      [--output review-comment-csv|per-person-data]");
            System.err.println("      [--commit-patch-set-count-threshold n]");
            System.err.println("      [--list-comments]");
            System.err.println();
            System.err.println(" --file file: read output from file");
            System.err.println(String.format(" --server url:port: read output from Gerrit server url and given port."
                    + " If port is omitted, defaults to %s.",
                    GerritStatReader.GERRIT_DEFAULT_PORT));
            System.err.println(" --project project: specifies the Gerrit project from which to retrieve stats. "
                    + "If omitted, stats will be retrieved from all projects.");
            System.err.println(" --limit n: The number of commits which to retrieve from the server. "
                    + "If omitted, stats will be retrieved until no further records are available.");
            System.err.println(" --query-output-file file: if specified, the output of the query will be written into "
                    + "the specified file, to be used later with e.g. --file switch.");
            System.err.println(" --branches branches: if specified, only the comma-separated list of branches "
                    + "will be included for analysis. If omitted, all available branches will be inspected.");
            System.err.println(" --include list-of-people: if specified, only the comma-separated list of identities "
                    + " will be included in generated statistics. If both --include and --exclude are "
                    + "specified, --exclude is ignored.");
            System.err.println(" --exclude list-of-people: if specified, the comma-separated list of identities "
                    + " will be excluded from all generated statistics.");
            System.err.println(" --output-type: if specified, the output will be provided in the specified format."
                    + "Defaults to HTML.");
            System.err.println(" --output: If specified, the output will be either a list of all review comments in "
                    + "CSV format, or a per-person data set. Defaults to per-person-data.");
            System.err.println("--commit-patch-set-count-threshold: If specified, all commit URLs "
                    + "exceeding the given patch set count will be listed in the per-person data. Defaults to 5."
                    + "If -1 is set, no listing is provided.");
            System.err.println("--list-comments: If specified, the per-person data will show a list of all "
                    + " code review comments written by a person.");
            System.exit(1);
            return;
        }

        OutputRules outputRules = new OutputRules(commandLine);

        CommitFilter filter = new CommitFilter();
        filter.setIncludeEmptyEmails(false);
        filter.setIncludedEmails(commandLine.getIncludedEmails());
        filter.setExcludedEmails(commandLine.getExcludedEmails());
        filter.setIncludeBranches(commandLine.getIncludeBranches());

        String serverName = commandLine.getServerName();
        String data;
        if (serverName != null) {
            GerritStatReader reader = GerritStatReader.fromCommandLine(serverName, commandLine.getServerPort());
            reader.setCommitLimit(commandLine.getCommitLimit());
            String projectName = commandLine.getProjectName();
            if (projectName != null) {
                reader.setProjectName(projectName);
            }
            data = reader.readData();

            String outputFile = commandLine.getOutputFile();
            if (outputFile != null) {
                FileWriter.writeFile(outputFile, data);
            }
        } else {
            data = FileReader.readFile(commandLine.getFilename());
        }

        GerritStatParser commitDataParser = new GerritStatParser();
        List<Commit> commits = commitDataParser.parseCommits(data);

        QueryData queryData = new QueryData(commandLine, commits);
        switch (commandLine.getOutput()) {
            case REVIEW_COMMENT_CSV:
                ReviewerProcessor reviewerFormatter = new ReviewerProcessor(filter, outputRules);
                reviewerFormatter.invoke(queryData);
                break;
            case PER_PERSON_DATA:
            default:
                PerPersonDataProcessor perPersonFormatter = new PerPersonDataProcessor(filter, outputRules);
                perPersonFormatter.invoke(queryData);
                break;
        }
    }
}
