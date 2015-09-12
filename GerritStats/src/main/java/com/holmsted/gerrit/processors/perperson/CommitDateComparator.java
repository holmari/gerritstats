package com.holmsted.gerrit.processors.perperson;

import com.holmsted.gerrit.Commit;

import java.util.Comparator;

public class CommitDateComparator implements Comparator<Commit> {
    @Override
    public int compare(Commit left, Commit right) {
        if (left.createdOnDate < right.createdOnDate) {
            return -1;
        } else if (left.createdOnDate > right.createdOnDate) {
            return 1;
        } else {
            return 0;
        }
    }
}
