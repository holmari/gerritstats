package com.holmsted.gerrit;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CommitFilter {

    private final List<String> includedEmails = new ArrayList<String>();
    private final List<String> excludedEmails = new ArrayList<String>();
    private final List<String> includedBranches = new ArrayList<String>();

    private boolean includeEmptyEmails;

    CommitFilter() {
    }

    public void setIncludedEmails(@Nonnull List<String> excludeEmails) {
        this.includedEmails.addAll(excludeEmails);
    }

    public void setExcludedEmails(@Nonnull List<String> excludeEmails) {
        this.excludedEmails.addAll(excludeEmails);
    }

    public void setIncludeBranches(@Nonnull List<String> includeBranches) {
        this.includedBranches.addAll(includeBranches);
    }

    public void setIncludeEmptyEmails(boolean includeEmptyEmails) {
        this.includeEmptyEmails = includeEmptyEmails;
    }

    public boolean isIncluded(@Nonnull Commit commit) {
        return includedBranches.isEmpty() || includedBranches.contains(commit.branch);
    }

    public boolean isIncluded(@Nullable Commit.Identity identity) {
        if (identity == null || identity.email == null) {
            return includeEmptyEmails;
        } else {
            if (!includedEmails.isEmpty()) {
                return includedEmails.contains(identity.email);
            } else {
                return !excludedEmails.contains(identity.email);
            }
        }
    }
}
