package com.holmsted.gerrit;

public class DatedCommitList extends DatedList<Commit> {

    public DatedCommitList() {
        super(commit -> commit.createdOnDate);
    }
}
