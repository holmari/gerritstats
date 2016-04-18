package com.holmsted.gerrit.processors.perperson;

import com.google.common.base.Strings;
import com.holmsted.gerrit.Commit;
import com.holmsted.gerrit.DatedCommitList;
import com.holmsted.gerrit.DatedPatchSetCommentList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import javax.annotation.Nonnull;

public class IdentityRecord {

    public static class ReviewerData {
        int addedAsReviewerCount;
        int approvalCount;

        public int getAddedAsReviewerCount() {
            return addedAsReviewerCount;
        }

        public int getApprovalCount() {
            return approvalCount;
        }
    }

    final Commit.Identity identity;

    int reviewCountPlus2;
    int reviewCountPlus1;
    int reviewCountMinus1;
    int reviewCountMinus2;

    final Hashtable<Integer, Integer> receivedReviews = new Hashtable<>();

    final DatedCommitList commits = new DatedCommitList();

    final List<Commit> addedAsReviewerTo = new ArrayList<>();
    final ReviewerDataTable reviewRequestors = new ReviewerDataTable();

    final PatchSetCommentTable commentsWritten = new PatchSetCommentTable();
    final PatchSetCommentTable commentsReceived = new PatchSetCommentTable();
    final ReviewerDataTable reviewersForOwnCommits = new ReviewerDataTable();

    private long averageTimeInCodeReview;

    public IdentityRecord(Commit.Identity identity) {
        this.identity = identity;
    }

    public String getName() {
        return identity.getName();
    }

    public long getAverageTimeInCodeReview() {
        return averageTimeInCodeReview;
    }

    public String getPrintableAverageTimeInCodeReview() {
       return formatPrintableDuration(averageTimeInCodeReview);
    }

    public String getEmail() {
        return identity.getEmail();
    }

    public String getUsername() {
        return identity.getUsername();
    }

    public String getFilenameStem() {
        String filename = identity.getUsername();
        if (Strings.isNullOrEmpty(filename)) {
            filename = Strings.nullToEmpty(identity.getEmail()).replace(".", "_");
            int atMarkIndex = filename.indexOf('@');
            if (atMarkIndex != -1) {
                filename = filename.substring(0, atMarkIndex);
            } else {
                filename = "anonymous_coward";
            }
        }
        return filename;
    }

    public DatedCommitList getCommits() {
        return commits;
    }

    public List<Commit> getAddedAsReviewerTo() {
        return addedAsReviewerTo;
    }

    public DatedPatchSetCommentList getAllCommentsWritten() {
        return commentsWritten.getAllComments();
    }

    public List<Commit.PatchSetComment> getAllCommentsReceived() {
        List<Commit.PatchSetComment> allComments = new ArrayList<>();
        for (List<Commit.PatchSetComment> comments : commentsReceived.values()) {
            allComments.addAll(comments);
        }
        return allComments;
    }

    public Hashtable<Commit.Identity, ReviewerData> getReviewersForOwnCommits() {
        return reviewersForOwnCommits;
    }

    public ReviewerData getReviewerDataForOwnCommitFor(@Nonnull Commit.Identity identity) {
        return reviewersForOwnCommits.get(identity);
    }

    public ReviewerData getReviewRequestorDataFor(@Nonnull Commit.Identity identity) {
        return reviewRequestors.get(identity);
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
        int receivedComments = getAllCommentsReceived().size();
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
        List<Commit.Identity> sortedIdentities = new ArrayList<>(reviewersForOwnCommits.keySet());
        Collections.sort(sortedIdentities, new ReviewerAddedCountComparator(reviewersForOwnCommits));
        return sortedIdentities;
    }

    public String getDisplayableMyReviewerList() {
        return getPrintableReviewerList(getMyReviewerList(), reviewersForOwnCommits);
    }

    public List<Commit.Identity> getReviewRequestorList() {
        List<Commit.Identity> sortedIdentities = new ArrayList<>(reviewRequestors.keySet());
        Collections.sort(sortedIdentities, new ReviewerAddedCountComparator(reviewRequestors));
        return sortedIdentities;
    }

    public String getDisplayableAddedReviewerList() {
        return getPrintableReviewerList(getReviewRequestorList(), reviewRequestors);
    }

    private ReviewerData getOrCreateReviewerForOwnCommit(@Nonnull Commit.Identity identity) {
        ReviewerData reviewerData = reviewersForOwnCommits.get(identity);
        if (reviewerData == null) {
            reviewerData = new ReviewerData();
        }
        return reviewerData;
    }

    public void addReviewerForOwnCommit(@Nonnull Commit.Identity identity) {
        ReviewerData reviewerData = getOrCreateReviewerForOwnCommit(identity);
        reviewerData.addedAsReviewerCount++;
        reviewersForOwnCommits.put(identity, reviewerData);
    }

    void addApprovalForOwnCommit(@Nonnull Commit.Identity identity) {
        ReviewerData reviewerData = getOrCreateReviewerForOwnCommit(identity);
        reviewerData.approvalCount++;
        reviewersForOwnCommits.put(identity, reviewerData);
    }

    private String getPrintableReviewerList(@Nonnull List<Commit.Identity> sortedIdentities,
                                            @Nonnull Hashtable<Commit.Identity, ReviewerData> reviewsForIdentity) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < sortedIdentities.size(); ++i) {
            Commit.Identity identity = sortedIdentities.get(i);
            builder.append(String.format("%s (%d)",
                    identity.toString(), reviewsForIdentity.get(identity).addedAsReviewerCount));
            if (i < sortedIdentities.size() - 1) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }

    public List<Commit> getCommitsWithNPatchSets(int patchSetCountThreshold) {
        List<Commit> exceedingCommits = new ArrayList<>();
        for (Commit commit : commits) {
            int patchSetCount = commit.getPatchSetCountForKind(Commit.PatchSetKind.REWORK);
            if (patchSetCount <= patchSetCountThreshold) {
                continue;
            }
            int firstNonAuthorCommentPatchSetIndex = commit.getFirstPatchSetIndexWithNonAuthorReview();
            if (firstNonAuthorCommentPatchSetIndex != -1
                    && commit.getPatchSets().size() - firstNonAuthorCommentPatchSetIndex > patchSetCountThreshold) {
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

        ReviewerData reviewsDoneForIdentity = reviewRequestors.get(commit.owner);
        if (reviewsDoneForIdentity == null) {
            reviewsDoneForIdentity = new ReviewerData();
        }
        reviewsDoneForIdentity.addedAsReviewerCount++;

        reviewRequestors.put(commit.owner, reviewsDoneForIdentity);
    }

    public void addWrittenComment(@Nonnull Commit commit, @Nonnull Commit.PatchSetComment patchSetComment) {
        commentsWritten.addCommentForCommit(commit, patchSetComment);
    }

    public void addReceivedComment(@Nonnull Commit commit, Commit.PatchSetComment patchSetComment) {
        commentsReceived.addCommentForCommit(commit, patchSetComment);
    }

    public List<Commit> getCommitsWithWrittenComments() {
        ArrayList<Commit> commits = Collections.list(commentsWritten.keys());
        Collections.sort(commits, new CommitDateComparator());
        return commits;
    }

    public List<Commit.PatchSetComment> getWrittenCommentsForCommit(@Nonnull Commit commit) {
        return commentsWritten.get(commit);
    }

    public int getReceivedReviewsForScore(int score) {
        Integer value = receivedReviews.get(score);
        return value != null ? value : 0;
    }

    public void addCommit(@Nonnull Commit commit) {
        commits.add(commit);
    }

    void updateAverageTimeInCodeReview(long commitTimeInCodeReviewMsec) {
        int prevCount = commits.size() - 1;
        long newAverage = averageTimeInCodeReview * prevCount;
        averageTimeInCodeReview = (newAverage + commitTimeInCodeReviewMsec) / commits.size();
    }

    void addReceivedCodeReview(@Nonnull Commit.Approval approval) {
        Integer receivedReviewCountForValue = receivedReviews.get(approval.value);
        if (receivedReviewCountForValue == null) {
            receivedReviewCountForValue = 1;
        } else {
            receivedReviewCountForValue++;
        }
        receivedReviews.put(approval.value, receivedReviewCountForValue);
    }

    private static String formatPrintableDuration(long duration) {
        int durationInSecs = (int) (duration / 1000);
        int days = durationInSecs / (60 * 60 * 24);
        int hours = durationInSecs / (60 * 60) % 24;
        int minutes = durationInSecs / (60) % 60;

        return String.format("%dd %dh %dmin", days, hours, minutes);
    }
}
