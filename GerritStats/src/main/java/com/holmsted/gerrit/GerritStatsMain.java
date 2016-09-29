package com.holmsted.gerrit;

import com.holmsted.file.FileReader;
import com.holmsted.gerrit.GerritStatParser.GerritData;
import com.holmsted.gerrit.processors.perperson.PerPersonDataProcessor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class GerritStatsMain {

    public static void main(String[] args) {
        CommandLineParser commandLine = new CommandLineParser();
        if (!commandLine.parse(args)) {
            System.out.println("Reads and outputs Gerrit statistics.");
            commandLine.printUsage();
            System.exit(1);
            return;
        }

        CommitFilter filter = new CommitFilter();
        filter.setIncludeEmptyEmails(false);
        filter.setIncludedEmails(commandLine.getIncludedEmails());
        filter.setExcludedEmails(commandLine.getExcludedEmails());
        filter.setIncludeBranches(commandLine.getIncludeBranches());

        List<Commit> commits = new ArrayList<>();
        GerritStatParser commitDataParser = new GerritStatParser();

        List<String> filenames = processFilenames(commandLine.getFilenames());
        GerritVersion minVersion = GerritVersion.makeInvalid();

        for (String filename : filenames) {
            @Nullable
            String data = FileReader.readFile(checkNotNull(filename));
            if (data != null) {
                GerritData gerritData = commitDataParser.parseJsonData(data);
                commits.addAll(gerritData.commits);
                if (minVersion.isInvalid() || !gerritData.version.isAtLeast(minVersion)) {
                    minVersion = gerritData.version;
                }
            } else {
                System.err.println(String.format("Could not read file '%s'", filename));
            }
        }

        QueryData queryData = new QueryData(commandLine.getFilenames(),
                commits,
                minVersion);

        OutputRules outputRules = new OutputRules(commandLine);
        if (outputRules.isAnonymizeDataEnabled()) {
            System.out.println("Anonymizing data...");
            queryData = queryData.anonymize();
        }

        PerPersonDataProcessor perPersonFormatter = new PerPersonDataProcessor(filter, outputRules);
        perPersonFormatter.invoke(queryData);
    }

    @Nonnull
    private static List<String> processFilenames(@Nonnull List<String> filenames) {
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
                File[] subdirFiles = file.listFiles((dir, name) -> {
                    return name.endsWith(".json");
                });
                if (subdirFiles != null) {
                    for (File subdirFile : subdirFiles) {
                        result.add(subdirFile.getAbsolutePath());
                    }
                }
            }
        }

        return result;
    }

    private GerritStatsMain() {
    }
}
