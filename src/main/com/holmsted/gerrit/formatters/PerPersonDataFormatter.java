package com.holmsted.gerrit.formatters;

import com.holmsted.gerrit.Commit;
import com.holmsted.gerrit.CommitFilter;
import com.holmsted.gerrit.OutputRules;
import com.holmsted.gerrit.OutputType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;

public class PerPersonDataFormatter extends CommitDataFormatter {

    private static class IdentityRecord {
        final Commit.Identity identity;

        int reviewCountPlus2;
        int reviewCountPlus1;
        int reviewCountMinus1;
        int reviewCountMinus2;

        final List<Commit> commits = new ArrayList<Commit>();
        final List<Commit> addedAsReviewerTo = new ArrayList<Commit>();
        final List<Commit.PatchSetComment> commentsWritten = new ArrayList<Commit.PatchSetComment>();
        final List<Commit.PatchSetComment> commentsReceived = new ArrayList<Commit.PatchSetComment>();
        final Hashtable<Commit.Identity, Integer> reviewersForOwnCommits = new Hashtable<Commit.Identity, Integer>();

        public IdentityRecord(Commit.Identity identity) {
            this.identity = identity;
        }

        public float getReceivedCommentRatio() {
            int receivedComments = commentsReceived.size();
            int commitCount = commits.size();
            if (commitCount > 0) {
                return (float) receivedComments / commitCount;
            } else {
                return 0;
            }
        }

        public float getAveragePatchSetCount() {
            int commitCount = commits.size();
            if (commitCount > 0) {
                int patchSetCount = 0;
                for (Commit commit : commits) {
                    patchSetCount += commit.getPatchSetCountForKind(Commit.PatchSetKind.REWORK);
                }
                return (float) patchSetCount / commits.size();
            } else {
                return 0;
            }
        }

        public int getMaxPatchSetCount() {
            int commitCount = commits.size();
            if (commitCount > 0) {
                int max = Integer.MIN_VALUE;
                for (Commit commit : commits) {
                    max = Math.max(commit.getPatchSetCountForKind(Commit.PatchSetKind.REWORK), max);
                }
                return max;
            } else {
                return 0;
            }
        }

        public float getReviewCommentRatio() {
            return (float) commentsWritten.size() / addedAsReviewerTo.size();
        }

        public void addApproval(Commit.Approval approval) {
            if (approval.value == 2) {
                ++reviewCountPlus2;
            } else if (approval.value == 1) {
                ++reviewCountPlus1;
            } else if (approval.value == -1) {
                ++reviewCountMinus1;
            } else if (approval.value == -2) {
                ++reviewCountMinus2;
            }
        }

        public String getDisplayableMyReviewerList() {
            List<Commit.Identity> sortedIdentities = new ArrayList<Commit.Identity>(reviewersForOwnCommits.keySet());
            Collections.sort(sortedIdentities, new ReviewComparator(reviewersForOwnCommits));
            return getPrintableReviewerList(sortedIdentities, reviewersForOwnCommits);
        }

        public String getDisplayableAddedReviewerList() {
            Hashtable<Commit.Identity, Integer> identities = new Hashtable<Commit.Identity, Integer>();
            for (Commit commit : addedAsReviewerTo) {
                Integer count = identities.get(commit.owner);
                if (!commit.owner.equals(identity)) {
                    identities.put(commit.owner, count == null ? 1 : count + 1);
                }
            }

            List<Commit.Identity> sortedIdentities = new ArrayList<Commit.Identity>(identities.keySet());
            Collections.sort(sortedIdentities, new ReviewComparator(identities));

            return getPrintableReviewerList(sortedIdentities, identities);
        }

        public void addReviewerForOwnCommit(Commit.Identity identity) {
            Integer reviewCount = reviewersForOwnCommits.get(identity);
            reviewersForOwnCommits.put(identity, reviewCount == null ? 1 : reviewCount + 1);
        }

        private String getPrintableReviewerList(@Nonnull List<Commit.Identity> sortedIdentities,
                                                @Nonnull Hashtable<Commit.Identity, Integer> reviewsForIdentity) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < sortedIdentities.size(); ++i) {
                Commit.Identity identity = sortedIdentities.get(i);
                builder.append(String.format("%s (%d)",
                        identity.toString(), reviewsForIdentity.get(identity).intValue()));
                if (i < sortedIdentities.size() - 1) {
                    builder.append(", ");
                }
            }
            return builder.toString();
        }
    }

    private static class ReviewComparator implements Comparator<Commit.Identity> {
        private Hashtable<Commit.Identity, Integer> reviewsForIdentity;
        public ReviewComparator(Hashtable<Commit.Identity, Integer> reviewsForIdentity) {
            this.reviewsForIdentity = reviewsForIdentity;
        }

        @Override
        public int compare(Commit.Identity left, Commit.Identity right) {
            Integer reviewCountLeft = reviewsForIdentity.get(left);
            Integer reviewCountRight = reviewsForIdentity.get(right);
            if (reviewCountLeft < reviewCountRight) {
                return 1;
            } else if (reviewCountLeft > reviewCountRight) {
                return -1;
            } else {
                return left.email.compareTo(right.email);
            }
        }
    }

    private static class CommentsWrittenComparator implements Comparator<IdentityRecord> {
        @Override
        public int compare(IdentityRecord left, IdentityRecord right) {
            if (left.commentsWritten.size() < right.commentsWritten.size()) {
                return -1;
            } else if (left.commentsWritten.size() > right.commentsWritten.size()) {
                return 1;
            } else {
                return 1;
            }
        }
    }

    private final Hashtable<Commit.Identity, IdentityRecord> records =
            new Hashtable<Commit.Identity, IdentityRecord>();

    public PerPersonDataFormatter(@Nonnull CommitFilter filter, @Nonnull OutputRules outputRules) {
        super(filter, outputRules);
    }

    @Override
    public String invoke(@Nonnull List<Commit> commits) {
        records.clear();
        CommitVisitor visitor = new CommitVisitor(getCommitFilter()) {
            @Override
            public void visitCommit(@Nonnull Commit commit) {
                IdentityRecord ownerRecord = getOrCreateRecord(commit.owner);
                ownerRecord.commits.add(commit);

                for (Commit.Identity identity : commit.reviewers) {
                    if (!getCommitFilter().isIncluded(identity)) {
                        continue;
                    }
                    if (!ownerRecord.identity.equals(identity)) {
                        ownerRecord.addReviewerForOwnCommit(identity);
                    }

                    IdentityRecord reviewerRecord = getOrCreateRecord(identity);
                    if (!commit.owner.equals(reviewerRecord.identity)) {
                        reviewerRecord.addedAsReviewerTo.add(commit);
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
        visitor.visit(commits);

        List<IdentityRecord> orderedList = new ArrayList<IdentityRecord>();
        orderedList.addAll(records.values());
        Collections.sort(orderedList, new CommentsWrittenComparator());

        switch (getOutputRules().getOutputType()) {
            case CSV:
                return createCsvOutput(orderedList);
            case PLAIN:
            default:
                return createPlainOutput(orderedList);
        }
    }

    private String createCsvOutput(@Nonnull List<IdentityRecord> orderedList) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%34s\t%7s\t%16s\t%17s\t%20s\t%17s\t%20s\t%20s\t%19s\t%16s\t%16s\t%16s\t%16s\t%30s\n",
                "Identity",
                "Commits",
                "Comments written",
                "Comments received",
                "Commit/comment ratio",
                "Added as reviewer",
                "Review comment ratio",
                "Avg. patch set count",
                "Max patch set count",
                "+2 reviews given",
                "+1 reviews given",
                "-1 reviews given",
                "-2 reviews given",
                "# of people added as reviewers"));
        for (IdentityRecord record : orderedList) {
            builder.append(String.format("%34s\t%7d\t%16d\t%17d\t%20f\t%17d\t",
                    record.identity.email,
                    record.commits.size(),
                    record.commentsWritten.size(),
                    record.commentsReceived.size(),
                    record.getReceivedCommentRatio(),
                    record.addedAsReviewerTo.size(), Locale.getDefault()));
            builder.append(String.format("%20f\t%20f\t%19d\t%16d\t%16d\t%16d\t%16d\t%30d\n",
                    record.getReviewCommentRatio(),
                    record.getAveragePatchSetCount(),
                    record.getMaxPatchSetCount(),
                    record.reviewCountPlus2,
                    record.reviewCountPlus1,
                    record.reviewCountMinus1,
                    record.reviewCountMinus2,
                    record.reviewersForOwnCommits.size(), Locale.getDefault()));
        }
        return builder.toString();
    }

    private String createPlainOutput(@Nonnull List<IdentityRecord> orderedList) {
        HumanReadableLineBuilder builder = new HumanReadableLineBuilder();
        for (IdentityRecord record : orderedList) {
            builder.addLine(record.identity.email);
            builder.addIndentLine("Commits: " + record.commits.size());
            builder.addIndentLine("Comments written: " + record.commentsWritten.size());
            builder.addIndentLine("Comments received: " + record.commentsReceived.size());
            builder.addIndentLine("Commit/comment ratio: " + record.getReceivedCommentRatio());
            builder.addIndentLine("Added as reviewer: " + record.addedAsReviewerTo.size());
            builder.addIndentLine("Review comment ratio: " + record.getReviewCommentRatio());
            builder.addIndentLine("Avg. patch set count: " + record.getAveragePatchSetCount());
            builder.addIndentLine("Max patch set count: " + record.getMaxPatchSetCount());
            builder.addIndentLine("+2 reviews given: " + record.reviewCountPlus2);
            builder.addIndentLine("+1 reviews given: " + record.reviewCountPlus1);
            builder.addIndentLine("-1 reviews given: " + record.reviewCountMinus1);
            builder.addIndentLine("-2 reviews given: " + record.reviewCountMinus2);
            builder.addIndentLine("# of people added as reviewers: " + record.reviewersForOwnCommits.size());
            builder.addIndentLine("Adds them as reviewers: " + record.getDisplayableMyReviewerList());
            builder.addIndentLine("They add this person as reviewer: " + record.getDisplayableAddedReviewerList());
            builder.addLine("");
        }

        return builder.toString();
    }

    private IdentityRecord getOrCreateRecord(@Nonnull Commit.Identity identity) {
        IdentityRecord identityRecord = records.get(identity);
        if (identityRecord == null) {
            identityRecord = new IdentityRecord(identity);
            records.put(identity, identityRecord);
        }
        return identityRecord;
    }
}
