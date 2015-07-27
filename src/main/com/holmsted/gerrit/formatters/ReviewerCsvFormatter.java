package com.holmsted.gerrit.formatters;

import com.holmsted.gerrit.Commit;
import com.holmsted.gerrit.CommitFilter;
import com.holmsted.gerrit.OutputRules;
import com.holmsted.gerrit.OutputType;
import com.holmsted.gerrit.QueryData;

import java.text.SimpleDateFormat;
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
public class ReviewerCsvFormatter extends CommitDataFormatter {

    public ReviewerCsvFormatter(@Nonnull CommitFilter filter, @Nonnull OutputRules outputRules) {
        super(filter, outputRules);
    }

    @Override
    public String invoke(@Nonnull QueryData queryData) {
        final StringBuilder builder = new StringBuilder();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        CommitVisitor visitor = new CommitVisitor(getCommitFilter()) {
            @Override public void visitCommit(@Nonnull Commit commit) {}
            @Override public void visitPatchSet(@Nonnull Commit.PatchSet patchSet) {}
            @Override public void visitApproval(@Nonnull Commit.PatchSet patchSet,
                                                @Nonnull Commit.Approval approval) {}

            @Override
            public void visitPatchSetComment(@Nonnull Commit.PatchSet patchSet,
                                             @Nonnull Commit.PatchSetComment patchSetComment) {
                Date date = new Date(patchSet.createdOnDate);
                String outComment = String.format("%10s %40s %40s",
                        dateFormat.format(date),
                        patchSetComment.reviewer.email,
                        patchSet.author.email);
                builder.append(outComment).append('\n');
            }
        };
        visitor.visit(queryData.getCommits());

        return builder.toString();
    }
}
