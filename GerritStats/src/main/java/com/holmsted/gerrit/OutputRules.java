package com.holmsted.gerrit;

import javax.annotation.Nonnull;

public class OutputRules {

    public static final int INVALID_PATCH_COUNT = -1;

    private CommandLineParser commandLine;

    OutputRules(@Nonnull CommandLineParser commandLine) {
        this.commandLine = commandLine;
    }

    public int getCommitPatchSetCountThreshold() {
        return commandLine.getCommitPatchSetCountThreshold();
    }

    public String getOutputDir() {
        return commandLine.getOutputDir();
    }

    public boolean getAnonymizeData() {
        return commandLine.getAnonymizeData();
    }
}
