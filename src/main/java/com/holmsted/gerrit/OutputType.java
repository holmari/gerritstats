package com.holmsted.gerrit;

public enum OutputType {
    /**
     * CSV format, suitable for processing data with other tools.
     */
    CSV("csv"),
    /**
     * Plain text, human readable format.
     */
    PLAIN("plain"),
    /**
     *
     */
    HTML("html");

    private final String formatName;

    OutputType(String formatName) {
        this.formatName = formatName;
    }

    public static OutputType fromTypeString(String formatName) {
        if (formatName.equals(CSV.formatName)) {
            return CSV;
        } else if (formatName.equals(PLAIN.formatName)) {
            return PLAIN;
        } else {
            return HTML;
        }
    }
}
