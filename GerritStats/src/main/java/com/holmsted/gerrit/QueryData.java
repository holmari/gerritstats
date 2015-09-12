package com.holmsted.gerrit;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

public class QueryData {
    private final CommandLineParser commandLine;
    private final List<Commit> commits = new ArrayList<>();

    QueryData(@Nonnull CommandLineParser commandLine, @Nonnull List<Commit> commits) {
        this.commandLine = commandLine;
        this.commits.addAll(commits);
    }

    public String getDisplayableProjectName() {
        return String.format("all data from file %s", commandLine.getFilename());
    }

    public String getDisplayableBranchList() {
        List<String> includeBranches = commandLine.getIncludeBranches();
        if (includeBranches.isEmpty()) {
            return "(all branches)";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < includeBranches.size(); ++i) {
            builder.append(includeBranches.get(i));
            if (i < includeBranches.size() - 1) {
                builder.append(", ");
            }
        }

        return builder.toString();
    }

    public List<Commit> getCommits() {
        return commits;
    }
}