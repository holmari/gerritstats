package com.holmsted.gerrit.processors.perperson;

import com.holmsted.gerrit.Commit;

import java.util.Comparator;
import java.util.Hashtable;

class ReviewComparator implements Comparator<Commit.Identity> {
    private Hashtable<Commit.Identity, Integer> reviewsForIdentity;
    public ReviewComparator(Hashtable<Commit.Identity, Integer> reviewsForIdentity) {
        this.reviewsForIdentity = reviewsForIdentity;
    }

    @Override
    public int compare(Commit.Identity left, Commit.Identity right) {
        Integer reviewCountLeft = reviewsForIdentity.get(left);
        Integer reviewCountRight = reviewsForIdentity.get(right);
        if (reviewCountLeft < reviewCountRight) {
            return 1;
        } else if (reviewCountLeft > reviewCountRight) {
            return -1;
        } else {
            return left.email.compareTo(right.email);
        }
    }
}
