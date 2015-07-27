package com.holmsted.gerrit.formatters;

import com.holmsted.gerrit.Commit;
import com.holmsted.gerrit.CommitFilter;
import com.holmsted.gerrit.OutputType;

import java.util.List;

import javax.annotation.Nonnull;

public abstract class CommitDataFormatter {

    @Nonnull
    private final CommitFilter filter;
    @Nonnull
    private final OutputType outputType;

    public CommitDataFormatter(@Nonnull CommitFilter filter, @Nonnull OutputType outputType) {
        this.filter = filter;
        this.outputType = outputType;
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
    protected OutputType getOutputType() {
        return outputType;
    }
}
