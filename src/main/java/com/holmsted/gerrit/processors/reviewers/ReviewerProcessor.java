package com.holmsted.gerrit.processors.reviewers;

import com.holmsted.gerrit.Commit;
import com.holmsted.gerrit.CommitFilter;
import com.holmsted.gerrit.OutputRules;
import com.holmsted.gerrit.QueryData;
import com.holmsted.gerrit.processors.CommitDataProcessor;
import com.holmsted.gerrit.processors.CommitVisitor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.annotation.Nonnull;

/**
 * Walks through all code review comments in patch sets, making a printable list
 * in the format:
 * <p>
 * date reviewer-email commit-author-email
 */
public class ReviewerProcessor extends CommitDataProcessor<PatchSetCommentList> {

    static class CsvFormatter implements OutputFormatter<PatchSetCommentList> {

        private SimpleDateFormat dateFormat;

        public CsvFormatter() {
            dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        }

        @Override
        public void format(@Nonnull PatchSetCommentList dataList) {
            final StringBuilder builder = new StringBuilder();
            builder.append(String.format("%10s %40s %40s",
                    "Date",
                    "Reviewed by",
                    "Authored by")).append('\n');
            for (PatchSetCommentData data : dataList) {
                String outComment = String.format("%10s %40s %40s",
                        dateFormat.format(new Date(data.patchSetDate)),
                        data.reviewerEmail,
                        data.authorEmail);
                builder.append(outComment).append('\n');
            }
            System.out.print(builder.toString());
        }
    }

    public ReviewerProcessor(@Nonnull CommitFilter filter, @Nonnull OutputRules outputRules) {
        super(filter, outputRules);
    }

    /**
     * Creates a CSV text formatter regardless of the user-defined output type.
     */
    @Nonnull
    @Override
    protected OutputFormatter<PatchSetCommentList> createOutputFormatter() {
        return new CsvFormatter();
    }

    @Override
    public void process(@Nonnull OutputFormatter<PatchSetCommentList> formatter,
                        @Nonnull QueryData queryData) {
        final PatchSetCommentList dataList = new PatchSetCommentList();

        CommitVisitor visitor = new CommitVisitor(getCommitFilter()) {
            @Override public void visitCommit(@Nonnull Commit commit) {}
            @Override public void visitPatchSet(@Nonnull Commit commit,
                                                @Nonnull Commit.PatchSet patchSet) {}
            @Override public void visitApproval(@Nonnull Commit.PatchSet patchSet,
                                                @Nonnull Commit.Approval approval) {}

            @Override
            public void visitPatchSetComment(@Nonnull Commit commit,
                                             @Nonnull Commit.PatchSet patchSet,
                                             @Nonnull Commit.PatchSetComment patchSetComment) {
                PatchSetCommentData data = new PatchSetCommentData(patchSet.createdOnDate);
                data.reviewerEmail = patchSetComment.reviewer.email;
                data.authorEmail = patchSet.author.email;
                dataList.add(data);
            }
        };
        visitor.visit(queryData.getCommits());

        formatter.format(dataList);
    }
}
