package com.holmsted.gerrit.formatters;

import com.holmsted.gerrit.Commit;
import com.holmsted.gerrit.CommitFilter;
import com.holmsted.gerrit.OutputRules;
import com.holmsted.gerrit.OutputType;

import java.util.List;

import javax.annotation.Nonnull;

public abstract class CommitDataFormatter {

    @Nonnull
    private final CommitFilter filter;
    @Nonnull
    private final OutputRules outputRules;

    public CommitDataFormatter(@Nonnull CommitFilter filter, @Nonnull OutputRules outputRules) {
        this.filter = filter;
        this.outputRules = outputRules;
    }

    /**
     * Processes the list of commits and builds output for it,
     * returning it as a string.
     */
    public abstract String invoke(@Nonnull List<Commit> commits);

    @Nonnull
    protected CommitFilter getCommitFilter() {
        return filter;
    }

    @Nonnull
    protected OutputRules getOutputRules() {
        return outputRules;
    }
}
