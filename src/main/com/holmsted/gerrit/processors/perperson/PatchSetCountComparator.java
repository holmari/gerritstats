package com.holmsted.gerrit.processors.perperson;

import com.holmsted.gerrit.Commit;

import java.util.Comparator;

class PatchSetCountComparator implements Comparator<Commit> {
    @Override
    public int compare(Commit left, Commit right) {
        int leftCount = left.getPatchSetCountForKind(Commit.PatchSetKind.REWORK);
        int rightCount = right.getPatchSetCountForKind(Commit.PatchSetKind.REWORK);
        if (leftCount < rightCount) {
            return 1;
        } else if (leftCount > rightCount) {
            return -1;
        } else {
            return 0;
        }
    }
}
