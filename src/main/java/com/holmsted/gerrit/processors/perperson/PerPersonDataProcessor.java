package com.holmsted.gerrit.processors.perperson;

import com.holmsted.gerrit.Commit;
import com.holmsted.gerrit.CommitFilter;
import com.holmsted.gerrit.OutputRules;
import com.holmsted.gerrit.OutputType;
import com.holmsted.gerrit.QueryData;
import com.holmsted.gerrit.processors.CommitDataProcessor;
import com.holmsted.gerrit.processors.CommitVisitor;

import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nonnull;

public class PerPersonDataProcessor extends CommitDataProcessor<PerPersonData> {

    private final PerPersonData records = new PerPersonData();

    public PerPersonDataProcessor(@Nonnull CommitFilter filter, @Nonnull OutputRules outputRules) {
        super(filter, outputRules);
    }

    @Override
    public void process(@Nonnull OutputFormatter<PerPersonData> formatter, @Nonnull QueryData queryData) {
        records.clear();
        final AtomicLong fromDate = new AtomicLong(Long.MAX_VALUE);
        final AtomicLong toDate = new AtomicLong(Long.MIN_VALUE);

        CommitVisitor visitor = new CommitVisitor(getCommitFilter()) {
            @Override
            public void visitCommit(@Nonnull Commit commit) {
                IdentityRecord ownerRecord = getOrCreateRecord(commit.owner);
                ownerRecord.commits.add(commit);
                if (commit.lastUpdatedDate > toDate.get()) {
                    toDate.set(commit.lastUpdatedDate);
                }
                if (commit.lastUpdatedDate < fromDate.get()) {
                    fromDate.set(commit.lastUpdatedDate);
                }

                for (Commit.Identity identity : commit.reviewers) {
                    if (!getCommitFilter().isIncluded(identity)) {
                        continue;
                    }
                    if (!ownerRecord.identity.equals(identity)) {
                        ownerRecord.addReviewerForOwnCommit(identity);
                    }

                    IdentityRecord reviewerRecord = getOrCreateRecord(identity);
                    if (!commit.owner.equals(reviewerRecord.identity)) {
                        reviewerRecord.addReviewedCommit(commit);
                    }
                }
            }

            @Override
            public void visitPatchSet(@Nonnull Commit.PatchSet patchSet) {
            }

            @Override
            public void visitApproval(@Nonnull Commit.PatchSet patchSet, @Nonnull Commit.Approval approval) {
                IdentityRecord record = getOrCreateRecord(approval.grantedBy);
                record.addApproval(approval);
            }

            @Override
            public void visitPatchSetComment(@Nonnull Commit.PatchSet patchSet,
                                             @Nonnull Commit.PatchSetComment patchSetComment) {
                IdentityRecord reviewerRecord = getOrCreateRecord(patchSetComment.reviewer);
                if (!patchSet.author.equals(patchSetComment.reviewer)) {
                    reviewerRecord.commentsWritten.add(patchSetComment);
                }

                IdentityRecord authorRecord = getOrCreateRecord(patchSet.author);
                if (!patchSet.author.equals(patchSetComment.reviewer)) {
                    authorRecord.commentsReceived.add(patchSetComment);
                }
            }
        };
        visitor.visit(queryData.getCommits());
        records.setQueryData(queryData);
        records.setFromDate(fromDate.get());
        records.setToDate(toDate.get());

        formatter.format(records);
    }

    @Nonnull
    @Override
    protected OutputFormatter<PerPersonData> createOutputFormatter() {
        OutputType outputType = getOutputRules().getOutputType();
        switch (outputType) {
            case CSV:
                return new PerPersonCsvFormatter();
            case PLAIN:
                return new PerPersonPlaintextFormatter(getOutputRules());
            case HTML:
                return new PerPersonHtmlFormatter(getOutputRules());
            default:
                throw new UnsupportedOperationException("Unsupported format " + outputType);
        }
    }

    @Nonnull
    private IdentityRecord getOrCreateRecord(@Nonnull Commit.Identity identity) {
        IdentityRecord identityRecord = records.get(identity);
        if (identityRecord == null) {
            identityRecord = new IdentityRecord(identity);
            records.put(identity, identityRecord);
        }
        return identityRecord;
    }
}
