package com.holmsted.gerrit.processors.perperson;

import com.holmsted.gerrit.Commit;
import com.holmsted.gerrit.DateTimeProvider;
import com.holmsted.gerrit.DatedPatchSetCommentList;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.annotation.Nonnull;

public class PatchSetCommentTable extends Hashtable<Commit, List<Commit.PatchSetComment>> {

    @Nonnull
    private final DateTimeProvider<Commit.PatchSetComment> dateTimeProvider =
            new DateTimeProvider<Commit.PatchSetComment>() {
        @Override
        public long getDate(@Nonnull Commit.PatchSetComment comment) {
            // This can be a bit dangerous if
            return commentToCommit.get(comment).createdOnDate;
        }
    };

    private final Hashtable<Commit.PatchSetComment, Commit> commentToCommit = new Hashtable<>();
    private final DatedPatchSetCommentList allComments = new DatedPatchSetCommentList(dateTimeProvider);

    public void addCommentForCommit(@Nonnull Commit commit, @Nonnull Commit.PatchSetComment patchSetComment) {
        List<Commit.PatchSetComment> patchSetComments = get(commit);
        if (patchSetComments == null) {
            patchSetComments = new ArrayList<>();
        }
        patchSetComments.add(patchSetComment);

        commentToCommit.put(patchSetComment, commit);
        allComments.add(patchSetComment);

        put(commit, patchSetComments);
    }

    public DatedPatchSetCommentList getAllComments() {
        return allComments;
    }
}
