package com.holmsted.gerrit;

public enum OutputType {
    /**
     * CSV format, suitable for processing data with other tools.
     */
    CSV("csv"),
    /**
     * Plain text, human readable format.
     */
    PLAIN("plain");

    private final String formatName;

    OutputType(String formatName) {
        this.formatName = formatName;
    }

    public static OutputType fromFormatName(String formatName) {
        if (formatName.equals(CSV.formatName)) {
            return CSV;
        } else {
            return PLAIN;
        }
    }
}
