package com.holmsted.gerrit.processors.perperson;

import java.util.Comparator;

class AlphabeticalOrderComparator implements Comparator<IdentityRecord> {
    @Override
    public int compare(IdentityRecord left, IdentityRecord right) {
        return left.identity.compareTo(right.identity);
    }
}
