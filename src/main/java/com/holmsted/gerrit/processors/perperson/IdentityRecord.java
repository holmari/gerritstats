package com.holmsted.gerrit.processors.perperson;

import com.holmsted.gerrit.Commit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import javax.annotation.Nonnull;

public class IdentityRecord {
    final Commit.Identity identity;

    int reviewCountPlus2;
    int reviewCountPlus1;
    int reviewCountMinus1;
    int reviewCountMinus2;

    final List<Commit> commits = new ArrayList<Commit>();

    final List<Commit> addedAsReviewerTo = new ArrayList<Commit>();
    final Hashtable<Commit.Identity, Integer> reviewRequestors = new Hashtable<Commit.Identity, Integer>();

    final Hashtable<Commit, List<Commit.PatchSetComment>> commentsWritten =
            new Hashtable<Commit, List<Commit.PatchSetComment>>();
    final Hashtable<Commit, List<Commit.PatchSetComment>> commentsReceived =
            new Hashtable<Commit, List<Commit.PatchSetComment>>();
    final Hashtable<Commit.Identity, Integer> reviewersForOwnCommits = new Hashtable<Commit.Identity, Integer>();

    public IdentityRecord(Commit.Identity identity) {
        this.identity = identity;
    }

    public List<Commit> getCommits() {
        return commits;
    }

    public List<Commit> getAddedAsReviewerTo() {
        return addedAsReviewerTo;
    }

    public List<Commit.PatchSetComment> getAllCommentsWritten() {
        List<Commit.PatchSetComment> allComments = new ArrayList<Commit.PatchSetComment>();
        for (List<Commit.PatchSetComment> comments : commentsWritten.values()) {
            allComments.addAll(comments);
        }
        return allComments;
    }

    public List<Commit.PatchSetComment> getAllCommentsReceived() {
        List<Commit.PatchSetComment> allComments = new ArrayList<Commit.PatchSetComment>();
        for (List<Commit.PatchSetComment> comments : commentsReceived.values()) {
            allComments.addAll(comments);
        }
        return allComments;
    }

    public Hashtable<Commit.Identity, Integer> getReviewersForOwnCommits() {
        return reviewersForOwnCommits;
    }

    public Hashtable<Commit.Identity, Integer> getReviewRequestorCounts() {
        return reviewRequestors;
    }

    public int getReviewCountMinus1() {
        return reviewCountMinus1;
    }

    public int getReviewCountMinus2() {
        return reviewCountMinus2;
    }

    public int getReviewCountPlus1() {
        return reviewCountPlus1;
    }

    public int getReviewCountPlus2() {
        return reviewCountPlus2;
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

    public List<Commit.Identity> getMyReviewerList() {
        List<Commit.Identity> sortedIdentities = new ArrayList<Commit.Identity>(reviewersForOwnCommits.keySet());
        Collections.sort(sortedIdentities, new ReviewComparator(reviewersForOwnCommits));
        return sortedIdentities;
    }

    public String getDisplayableMyReviewerList() {
        return getPrintableReviewerList(getMyReviewerList(), reviewersForOwnCommits);
    }

    public List<Commit.Identity> getReviewRequestorList() {
        List<Commit.Identity> sortedIdentities = new ArrayList<Commit.Identity>(reviewRequestors.keySet());
        Collections.sort(sortedIdentities, new ReviewComparator(reviewRequestors));
        return sortedIdentities;
    }

    public String getDisplayableAddedReviewerList() {
        return getPrintableReviewerList(getReviewRequestorList(), reviewRequestors);
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

    public List<Commit> getCommitsWithNPatchSets(int patchSetCountThreshold) {
        List<Commit> exceedingCommits = new ArrayList<Commit>();
        for (Commit commit : commits) {
            int patchSetCount = commit.getPatchSetCountForKind(Commit.PatchSetKind.REWORK);
            if (patchSetCount > patchSetCountThreshold) {
                exceedingCommits.add(commit);
            }
        }
        Collections.sort(exceedingCommits, new PatchSetCountComparator());
        return exceedingCommits;
    }

    public String getPrintableCommitsWithNPatchSets(int patchSetCountThreshold) {
        List<Commit> exceedingCommits = getCommitsWithNPatchSets(patchSetCountThreshold);
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

    public String getPrintableAllReviewComments() {
        StringBuilder builder = new StringBuilder();
        for (Commit.PatchSetComment comment : getAllCommentsWritten()) {
            builder.append(comment.message).append("\n");
        }

        return builder.toString();
    }

    public void addReviewedCommit(@Nonnull Commit commit) {
        addedAsReviewerTo.add(commit);

        Integer reviewCountForIdentity = reviewRequestors.get(commit.owner);
        reviewRequestors.put(commit.owner, reviewCountForIdentity != null ? reviewCountForIdentity + 1 : 1);
    }

    public void addWrittenComment(@Nonnull Commit commit, @Nonnull Commit.PatchSetComment patchSetComment) {
        List<Commit.PatchSetComment> patchSetComments = commentsWritten.get(commit);
        if (patchSetComments == null) {
            patchSetComments = new ArrayList<Commit.PatchSetComment>();
        }
        patchSetComments.add(patchSetComment);
        commentsWritten.put(commit, patchSetComments);
    }

    public void addReceivedComment(@Nonnull Commit commit, Commit.PatchSetComment patchSetComment) {
        List<Commit.PatchSetComment> patchSetComments = commentsReceived.get(commit);
        if (patchSetComments == null) {
            patchSetComments = new ArrayList<Commit.PatchSetComment>();
        }
        patchSetComments.add(patchSetComment);
        commentsReceived.put(commit, patchSetComments);
    }

    public List<Commit> getCommitsWithWrittenComments() {
        ArrayList<Commit> commits = Collections.list(commentsWritten.keys());
        Collections.sort(commits, new CommitDateComparator());
        return commits;
    }

    public List<Commit.PatchSetComment> getWrittenCommentsForCommit(@Nonnull Commit commit) {
        return commentsWritten.get(commit);
    }
}
