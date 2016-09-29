package com.holmsted.gerrit.processors.perperson;

import java.io.Serializable;
import java.util.Comparator;

class AlphabeticalOrderComparator implements Comparator<IdentityRecord>, Serializable {

    static final long serialVersionUID = 1L;

    @Override
    public int compare(IdentityRecord left, IdentityRecord right) {
        return left.identity.compareTo(right.identity);
    }
}
