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
            System.out.println("Reads and outputs Gerrit statistics.");
            commandLine.printUsage();
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
