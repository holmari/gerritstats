package com.holmsted.gerrit;

import com.google.common.base.Joiner;

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
        List<String> filenames = commandLine.getFilenames();
        return String.format("all data from file(s) %s", Joiner.on(", ").join(filenames));
    }

    public String getDisplayableBranchList() {
        List<String> includeBranches = commandLine.getIncludeBranches();
        if (includeBranches.isEmpty()) {
            return "(all branches)";
        }

        return Joiner.on(", ").join(includeBranches);
    }

    public List<Commit> getCommits() {
        return commits;
    }
}