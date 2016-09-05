package com.holmsted.gerrit;

import javax.annotation.Nonnull;

public enum Output {
    /**
     * Code reviews listed in CSV format, including date, and reviewer and author identities.
     */
    REVIEW_COMMENT_CSV("review-comment-csv"),
    /**
     * Plain text, human readable format.
     */
    PER_PERSON_DATA("per-person-data");

    @Nonnull
    private final String name;

    Output(@Nonnull String name) {
        this.name = name;
    }

    @Nonnull
    public static Output fromString(String formatName) {
        if (formatName.equals(REVIEW_COMMENT_CSV.name)) {
            return REVIEW_COMMENT_CSV;
        } else {
            return PER_PERSON_DATA;
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
