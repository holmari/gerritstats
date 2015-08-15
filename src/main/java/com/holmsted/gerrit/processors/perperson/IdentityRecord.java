package com.holmsted.gerrit.processors.perperson;

import com.holmsted.gerrit.Commit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import javax.annotation.Nonnull;

class IdentityRecord {
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

    public String getCommitsWithNPatchSets(int patchSetCountThreshold) {
        List<Commit> exceedingCommits = new ArrayList<Commit>();
        for (Commit commit : commits) {
            int patchSetCount = commit.getPatchSetCountForKind(Commit.PatchSetKind.REWORK);
            if (patchSetCount > patchSetCountThreshold) {
                exceedingCommits.add(commit);
            }
        }
        Collections.sort(exceedingCommits, new PatchSetCountComparator());

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < exceedingCommits.size(); ++i) {
            Commit commit = exceedingCommits.get(i);
            builder.append(String.format("%s (%d)",
                    commit.url,
                    commit.getPatchSetCountForKind(Commit.PatchSetKind.REWORK)));
            if (i < exceedingCommits.size() - 1) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }

    public String getAllReviewComments() {
        StringBuilder builder = new StringBuilder();
        for (Commit.PatchSetComment comment : commentsWritten) {
            builder.append(comment.message).append("\n");
        }

        return builder.toString();
    }
}
