package com.holmsted.gerrit;

import javax.annotation.Nonnull;

public class OutputRules {

    public static final int INVALID_PATCH_COUNT = -1;

    @Nonnull
    private final CommandLineParser commandLine;

    OutputRules(@Nonnull CommandLineParser commandLine) {
        this.commandLine = commandLine;
    }

    public String getOutputDir() {
        return commandLine.getOutputDir();
    }

    public boolean isAnonymizeDataEnabled() {
        return commandLine.isAnonymizeDataEnabled();
    }
}
