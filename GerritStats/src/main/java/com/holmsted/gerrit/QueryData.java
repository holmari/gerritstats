package com.holmsted.gerrit;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.holmsted.gerrit.anonymizer.CommitAnonymizer;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

public class QueryData {
    @Nonnull
    private final List<Commit> commits;
    @Nonnull
    private final List<String> filenames;
    @Nonnull
    private final GerritVersion minVersion;

    @Nonnull
    private final List<String> branches;

    private String datasetKey;

    QueryData(@Nonnull List<String> filenames,
              @Nonnull List<Commit> commits,
              @Nonnull GerritVersion minVersion) {

        this.filenames = ImmutableList.copyOf(filenames);
        this.commits = ImmutableList.copyOf(commits);
        this.minVersion = minVersion;
        this.branches = this.commits.stream().map(commit -> commit.branch)
                .distinct().collect(Collectors.toList());
    }

    public String getDisplayableProjectName() {
        return String.format("all data from file(s) %s", Joiner.on(", ").join(this.filenames));
    }

    @Nonnull
    public List<String> getFilenames() {
        return this.filenames;
    }

    public List<String> getBranches() {
        return this.branches;
    }

    @Nonnull
    public List<Commit> getCommits() {
        return commits;
    }

    /**
     * Returns a unique hashcode formed out of the filenames. This can be used as a rudimentary
     * check for whether the data was generated with the same files.
     */
    @Nonnull
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

    @Nonnull
    public QueryData anonymize() {
        CommitAnonymizer anonymizer = new CommitAnonymizer();
        List<Commit> anonymizedCommits = anonymizer.process(commits);
        return new QueryData(ImmutableList.of(), anonymizedCommits, minVersion);
    }

    public GerritVersion getMinGerritVersion() {
        return minVersion;
    }
}