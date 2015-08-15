package com.holmsted.gerrit.processors.perperson;

import java.util.Comparator;

class CommentsWrittenComparator implements Comparator<IdentityRecord> {
    @Override
    public int compare(IdentityRecord left, IdentityRecord right) {
        if (left.commentsWritten.size() < right.commentsWritten.size()) {
            return -1;
        } else if (left.commentsWritten.size() > right.commentsWritten.size()) {
            return 1;
        } else {
            return 1;
        }
    }
}
