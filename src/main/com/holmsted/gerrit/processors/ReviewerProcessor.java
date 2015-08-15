package com.holmsted.gerrit.processors;

import com.holmsted.gerrit.Commit;
import com.holmsted.gerrit.CommitFilter;
import com.holmsted.gerrit.OutputRules;
import com.holmsted.gerrit.QueryData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;

/**
 * Walks through all code review comments in patch sets, making a printable list
 * in the format:
 * <p>
 * date reviewer-email commit-author-email
 */
public class ReviewerProcessor extends CommitDataProcessor {

    static class PatchSetCommentData {
        long patchSetDate;
        String reviewerEmail;
        String authorEmail;

        PatchSetCommentData(long patchSetDate) {
            this.patchSetDate = patchSetDate;
        }
    }

    public ReviewerProcessor(@Nonnull CommitFilter filter, @Nonnull OutputRules outputRules) {
        super(filter, outputRules);
    }

    @Override
    public String invoke(@Nonnull QueryData queryData) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        final List<PatchSetCommentData> dataList = new ArrayList<PatchSetCommentData>();

        CommitVisitor visitor = new CommitVisitor(getCommitFilter()) {
            @Override public void visitCommit(@Nonnull Commit commit) {}
            @Override public void visitPatchSet(@Nonnull Commit.PatchSet patchSet) {}
            @Override public void visitApproval(@Nonnull Commit.PatchSet patchSet,
                                                @Nonnull Commit.Approval approval) {}

            @Override
            public void visitPatchSetComment(@Nonnull Commit.PatchSet patchSet,
                                             @Nonnull Commit.PatchSetComment patchSetComment) {

                PatchSetCommentData data = new PatchSetCommentData(patchSet.createdOnDate);
                data.reviewerEmail = patchSetComment.reviewer.email;
                data.authorEmail = patchSet.author.email;
                dataList.add(data);
            }
        };
        visitor.visit(queryData.getCommits());

        final StringBuilder builder = new StringBuilder();
        for (PatchSetCommentData data : dataList) {
            String outComment = String.format("%10s %40s %40s",
                    dateFormat.format(new Date(data.patchSetDate)),
                    data.reviewerEmail,
                    data.authorEmail);
            builder.append(outComment).append('\n');
        }
        return builder.toString();
    }
}
