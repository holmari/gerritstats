package com.holmsted.gerrit;

import javax.annotation.Nonnull;

public class DatedCommitList extends DatedList<Commit> {

    public DatedCommitList() {
        super(new DateTimeProvider<Commit>() {
            @Override
            public long getDate(@Nonnull Commit commit) {
                return commit.createdOnDate;
            }
        });
    }
}
