package com.holmsted.gerrit.processors;

import com.holmsted.gerrit.CommitFilter;
import com.holmsted.gerrit.OutputRules;
import com.holmsted.gerrit.QueryData;

import javax.annotation.Nonnull;

public abstract class CommitDataProcessor<T> {

    @Nonnull
    private final CommitFilter filter;
    @Nonnull
    private final OutputRules outputRules;

    public interface OutputFormatter<T> {
        void format(@Nonnull T format);
    }

    public CommitDataProcessor(@Nonnull CommitFilter filter, @Nonnull OutputRules outputRules) {
        this.filter = filter;
        this.outputRules = outputRules;
    }

    /**
     * Processes the list of commits and builds output for it,
     * returning it as a string.
     */
    public void invoke(@Nonnull QueryData queryData) {
        OutputFormatter<T> formatter = createOutputFormatter();
        process(formatter, queryData);
    }

    protected abstract void process(@Nonnull OutputFormatter<T> formatter, @Nonnull QueryData queryData);

    @Nonnull
    protected abstract OutputFormatter<T> createOutputFormatter();

    @Nonnull
    protected CommitFilter getCommitFilter() {
        return filter;
    }

    @Nonnull
    protected OutputRules getOutputRules() {
        return outputRules;
    }
}
