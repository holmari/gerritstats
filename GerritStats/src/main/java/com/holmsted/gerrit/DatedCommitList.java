package com.holmsted.gerrit;

import javax.annotation.Nonnull;

public class DatedCommitList extends DatedList<Commit> {

    public DatedCommitList() {
        super(commit -> commit.createdOnDate);
    }
}
