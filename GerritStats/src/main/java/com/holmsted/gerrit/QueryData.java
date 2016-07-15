package com.holmsted.gerrit;

import com.google.common.base.Joiner;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

public class QueryData {
    private final CommandLineParser commandLine;
    private final List<Commit> commits = new ArrayList<>();

    private String datasetKey;

    QueryData(@Nonnull CommandLineParser commandLine, @Nonnull List<Commit> commits) {
        this.commandLine = commandLine;
        this.commits.addAll(commits);
    }

    public String getDisplayableProjectName() {
        List<String> filenames = commandLine.getFilenames();
        return String.format("all data from file(s) %s", Joiner.on(", ").join(filenames));
    }

    public List<String> getFilenames() {
        return commandLine.getFilenames();
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

    /**
     * Returns a unique hashcode formed out of the filenames. This can be used as a rudimentary
     * check for whether the data was generated with the same files.
     */
    public String getDatasetKey() {
        if (this.datasetKey == null) {
            List<String> filenames = new ArrayList<>(getFilenames());
            Collections.sort(filenames);
            HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
            for (String filename : filenames) {
                hashCodeBuilder.append(filename);
            }
            this.datasetKey = hashCodeBuilder.build().toString();
        }

        return this.datasetKey;
    }
}