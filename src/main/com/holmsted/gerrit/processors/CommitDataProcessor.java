package com.holmsted.gerrit.processors;

import com.holmsted.gerrit.CommitFilter;
import com.holmsted.gerrit.OutputRules;
import com.holmsted.gerrit.QueryData;

import javax.annotation.Nonnull;

public abstract class CommitDataProcessor {

    @Nonnull
    private final CommitFilter filter;
    @Nonnull
    private final OutputRules outputRules;

    public CommitDataProcessor(@Nonnull CommitFilter filter, @Nonnull OutputRules outputRules) {
        this.filter = filter;
        this.outputRules = outputRules;
    }

    /**
     * Processes the list of commits and builds output for it,
     * returning it as a string.
     */
    public abstract String invoke(@Nonnull QueryData queryData);

    @Nonnull
    protected CommitFilter getCommitFilter() {
        return filter;
    }

    @Nonnull
    protected OutputRules getOutputRules() {
        return outputRules;
    }
}
