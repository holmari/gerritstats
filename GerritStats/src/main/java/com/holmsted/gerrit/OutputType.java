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
    JSON("json");

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
            return JSON;
        }
    }

    @Override
    public String toString() {
        return formatName;
    }
}
