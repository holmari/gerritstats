package com.holmsted.gerrit;

import javax.annotation.Nonnull;

public class DatedPatchSetCommentList extends DatedList<Commit.PatchSetComment> {

    public DatedPatchSetCommentList(@Nonnull DateTimeProvider<Commit.PatchSetComment> dateTimeProvider) {
        super(dateTimeProvider);
    }
}
