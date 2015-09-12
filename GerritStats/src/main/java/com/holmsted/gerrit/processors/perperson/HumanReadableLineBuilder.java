package com.holmsted.gerrit.processors.perperson;

public class HumanReadableLineBuilder {

    private static final int INDENT_SIZE = 2;

    private StringBuilder builder;
    private String indentString;

    HumanReadableLineBuilder() {
        builder = new StringBuilder();
        indentString = new String(new char[INDENT_SIZE]).replace("\0", " ");
    }

    public HumanReadableLineBuilder addLine(String text) {
        builder.append(text).append('\n');
        return this;
    }

    public HumanReadableLineBuilder addIndentLine(String text) {
        builder.append(indentString).append(text).append('\n');
        return this;
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
