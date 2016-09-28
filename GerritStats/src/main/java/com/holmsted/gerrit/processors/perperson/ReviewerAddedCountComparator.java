package com.holmsted.gerrit.processors.perperson;

import com.holmsted.gerrit.Commit;

import java.util.Comparator;
import java.util.Hashtable;

/**
 * Sorts reviewers in order of addition.
 */
class ReviewerAddedCountComparator implements Comparator<Commit.Identity> {
    private Hashtable<Commit.Identity, IdentityRecord.ReviewerData> reviewsForIdentity;

    public ReviewerAddedCountComparator(Hashtable<Commit.Identity, IdentityRecord.ReviewerData> reviewsForIdentity) {
        this.reviewsForIdentity = reviewsForIdentity;
    }

    @Override
    public int compare(Commit.Identity left, Commit.Identity right) {
        int reviewCountLeft = reviewsForIdentity.get(left).addedAsReviewerCount;
        int reviewCountRight = reviewsForIdentity.get(right).addedAsReviewerCount;
        if (reviewCountLeft < reviewCountRight) {
            return 1;
        } else if (reviewCountLeft > reviewCountRight) {
            return -1;
        } else {
            return left.email.compareTo(right.email);
        }
    }
}
