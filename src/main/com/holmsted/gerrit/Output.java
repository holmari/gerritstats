package com.holmsted.gerrit;

public enum Output {
    /**
     * Code reviews listed in CSV format, including date, and reviewer and author identities.
     */
    REVIEW_COMMENT_CSV("review-comment-csv"),
    /**
     * Plain text, human readable format.
     */
    PER_PERSON_DATA("per-person-data");

    private final String name;

    Output(String name) {
        this.name = name;
    }

    public static Output fromString(String formatName) {
        if (formatName.equals(REVIEW_COMMENT_CSV.name)) {
            return REVIEW_COMMENT_CSV;
        } else {
            return PER_PERSON_DATA;
        }
    }
}
