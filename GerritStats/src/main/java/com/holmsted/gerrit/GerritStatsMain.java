package com.holmsted.gerrit;

import com.holmsted.gerrit.GerritStatParser.GerritData;
import com.holmsted.gerrit.processors.perperson.PerPersonDataProcessor;
import com.holmsted.gerrit.processors.reviewers.ReviewerProcessor;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import com.holmsted.file.FileReader;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

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

        List<Commit> commits = new ArrayList<>();
        GerritStatParser commitDataParser = new GerritStatParser();

        List<String> filenames = processFilenames(commandLine.getFilenames());
        GerritVersion minVersion = null;

        for (String filename : filenames) {
            @Nullable
            String data = FileReader.readFile(checkNotNull(filename));
            if (data != null) {
                GerritData gerritData = commitDataParser.parseJsonData(data);
                commits.addAll(gerritData.commits);
                if (minVersion == null || !gerritData.version.isAtLeast(minVersion)) {
                    minVersion = gerritData.version;
                }
            } else {
                System.err.println(String.format("Could not read file '%s'" + filename));
            }
        }

        QueryData queryData = new QueryData(commandLine.getFilenames(),
                commandLine.getIncludeBranches(),
                commits,
                minVersion);

        if (outputRules.getAnonymizeData()) {
            queryData = queryData.anonymize();
        }

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

    private static List<String> processFilenames(List<String> filenames) {
        List<String> result = new ArrayList<>();
        for (String filename : filenames) {
            File file = new File(filename);
            if (!file.exists()) {
                System.err.println(String.format("Warning: file '%s' does not exist, skipping.", filename));
                continue;
            }
            if (file.isFile()) {
                result.add(filename);
            } else {
                File[] subdirFiles = file.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".json");
                    }
                });
                for (File subdirFile : subdirFiles) {
                    result.add(subdirFile.getAbsolutePath());
                }
            }
        }

        return result;
    }
}
