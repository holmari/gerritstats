package com.holmsted.gerrit.processors.perperson;

import com.holmsted.gerrit.Commit;
import com.holmsted.gerrit.Commit.Approval;
import com.holmsted.gerrit.Commit.Identity;
import com.holmsted.gerrit.Commit.PatchSet;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("PMD.TooManyFields")
public class IdentityRecord {

    final Commit.Identity identity;

    int reviewCountPlus2;
    int reviewCountPlus1;
    int reviewCountMinus1;
    int reviewCountMinus2;

    // updated when commit or comments written by this user are added
    long firstActiveDate = Long.MAX_VALUE;
    long lastActiveDate;

    long activeDayCount;
    private final transient Set<String> activeDays = new HashSet<>();

    final List<Commit> commits = new ArrayList<>();

    final List<Commit> addedAsReviewerTo = new ArrayList<>();
    final ReviewerDataTable reviewRequestors = new ReviewerDataTable();

    final PatchSetCommentTable commentsWritten = new PatchSetCommentTable();
    final PatchSetCommentTable commentsReceived = new PatchSetCommentTable();
    final ReviewerDataTable reviewersForOwnCommits = new ReviewerDataTable();

    final Map<String, GerritProject> repositories = new HashMap<>();

    private long averageTimeInCodeReview;

    public static class ReviewerData {
        int addedAsReviewerCount;
        int approvalCount;
        int commentCount;
        final Map<Integer, Integer> approvals = new HashMap<>();
    }

    public static class GerritProject {
        final String name;
        final String url;
        final ReviewerDataTable reviewRequestors = new ReviewerDataTable();

        int commitCountForUser;

        private GerritProject(String project, String url) {
            this.name = project;
            this.url = url;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof GerritProject) {
                GerritProject otherRepo = (GerritProject) other;
                return name.equals(otherRepo.name);
            } else {
                return false;
            }
        }

        public static GerritProject fromCommit(Commit commit) {
            String url = String.format("%s/#/q/project:%s",
                    commit.url.substring(0, commit.url.lastIndexOf('/')),
                    commit.project);
            return new GerritProject(commit.project, url);
        }

        public void addWrittenComment(@Nonnull Commit commit,
                                      @Nonnull PatchSet patchSet,
                                      @Nonnull Commit.PatchSetComment patchSetComment) {
            if (patchSet.author == null) {
                return;
            }
            ReviewerData data = reviewRequestors.computeIfAbsent(patchSet.author, k -> new ReviewerData());
            data.commentCount++;
        }
    }

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
        return identity.getIdentifier();
    }

    public List<Commit> getCommits() {
        return commits;
    }

    public List<Commit> getAddedAsReviewerTo() {
        return addedAsReviewerTo;
    }

    public List<Commit.PatchSetComment> getAllCommentsWritten() {
        return commentsWritten.getAllComments();
    }

    public List<Commit.PatchSetComment> getAllCommentsReceived() {
        List<Commit.PatchSetComment> allComments = new ArrayList<>();
        for (List<Commit.PatchSetComment> comments : commentsReceived.values()) {
            allComments.addAll(comments);
        }
        return allComments;
    }

    public Map<Commit.Identity, ReviewerData> getReviewersForOwnCommits() {
        return reviewersForOwnCommits;
    }

    public ReviewerData getReviewerDataForOwnCommitFor(@Nonnull Commit.Identity identity) {
        return reviewersForOwnCommits.get(identity);
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

    public List<GerritProject> getGerritProjects() {
        return new ArrayList<>(repositories.values());
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

    public int getInReviewCommitCount() {
        int newCommitCount = 0;
        for (Commit commit : commits) {
            if ("NEW".equals(commit.status)) {
                ++newCommitCount;
            }
        }
        return newCommitCount;
    }

    public int getAbandonedCommitCount() {
        int abandonedCommitCount = 0;
        for (Commit commit : commits) {
            if ("ABANDONED".equals(commit.status)) {
                ++abandonedCommitCount;
            }
        }
        return abandonedCommitCount;
    }

    public float getReviewCommentRatio() {
        if (addedAsReviewerTo.isEmpty()) {
            return 0;
        } else {
            return (float) getAllCommentsWritten().size() / addedAsReviewerTo.size();
        }
    }

    public void addApprovalByThisIdentity(@Nonnull Commit.Identity patchSetAuthor, Approval approval) {
        ReviewerData reviewerData = reviewRequestors.computeIfAbsent(patchSetAuthor, k -> new ReviewerData());
        reviewerData.approvalCount++;
        reviewerData.approvals.merge(approval.value, 1, Integer::sum);

        updateActivityTimestamps(approval.grantedOnDate);

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

    public List<Commit> getSelfReviewedCommits() {
        List<Commit> result = new ArrayList<>();

        for (Commit commit : commits) {
            if (!"MERGED".equals(commit.status)) {
                continue;
            }

            // merge always comes from the last patch set, so check that first
            boolean selfReviewedLastCommit = true;
            PatchSet lastPatchSet = commit.patchSets.get(commit.patchSets.size() - 1);
            for (Approval approval : lastPatchSet.approvals) {
                if (approval.value == 2) {
                    selfReviewedLastCommit &= approval.grantedBy.equals(identity);
                }
            }
            if (!selfReviewedLastCommit) {
                continue;
            }

            boolean foundEarlierNonAuthorApproval = false;
            for (PatchSet patchSet : commit.patchSets) {
                for (Approval approval : patchSet.approvals) {
                    if (approval.value == 2 && !approval.grantedBy.equals(identity)) {
                        foundEarlierNonAuthorApproval = true;
                        break;
                    }
                }
                if (foundEarlierNonAuthorApproval) {
                    break;
                }
            }
            if (foundEarlierNonAuthorApproval) {
                continue;
            }

            result.add(commit);
        }


        return result;
    }

    public List<Commit.Identity> getMyReviewerList() {
        List<Commit.Identity> sortedIdentities = new ArrayList<>(reviewersForOwnCommits.keySet());
        Collections.sort(sortedIdentities, new ReviewerAddedCountComparator(reviewersForOwnCommits));
        return sortedIdentities;
    }

    @Nonnull
    public List<Commit.Identity> getReviewRequestorList() {
        List<Commit.Identity> sortedIdentities = new ArrayList<>(reviewRequestors.keySet());
        Collections.sort(sortedIdentities, new ReviewerAddedCountComparator(reviewRequestors));
        return sortedIdentities;
    }

    @Nonnull
    private ReviewerData getOrCreateReviewerForOwnCommit(@Nonnull Commit.Identity identity) {
        return reviewersForOwnCommits.computeIfAbsent(identity, k -> new ReviewerData());
    }

    public void addReviewerForOwnCommit(@Nonnull Commit.Identity identity) {
        ReviewerData reviewerData = getOrCreateReviewerForOwnCommit(identity);
        reviewerData.addedAsReviewerCount++;
    }

    void addApprovalForOwnCommit(@Nonnull Identity approver, @Nonnull Approval approval) {
        ReviewerData reviewerData = getOrCreateReviewerForOwnCommit(approver);
        reviewerData.approvalCount++;
        reviewerData.approvals.merge(approval.value, 1, Integer::sum);
    }

    public void addReviewedCommit(@Nonnull Commit commit) {
        addedAsReviewerTo.add(commit);
        ReviewerData reviewsDoneForIdentity = reviewRequestors.computeIfAbsent(commit.owner,
                k -> new ReviewerData());
        reviewsDoneForIdentity.addedAsReviewerCount++;
    }

    public void addWrittenComment(@Nonnull Commit commit,
                                  @Nonnull  PatchSet patchSet,
                                  @Nonnull Commit.PatchSetComment patchSetComment) {
        ReviewerData reviewsDoneForIdentity = reviewRequestors.computeIfAbsent(commit.owner,
                k -> new ReviewerData());
        reviewsDoneForIdentity.commentCount++;

        GerritProject project = repositories.computeIfAbsent(commit.project,
                projectName -> GerritProject.fromCommit(commit));
        project.addWrittenComment(commit, patchSet, patchSetComment);

        commentsWritten.addCommentForCommit(commit, patchSetComment);
        updateActivityTimestamps(commit.getPatchSetForComment(patchSetComment).createdOnDate);
    }

    public void addReceivedComment(@Nonnull Commit commit,
                                   @Nonnull PatchSet patchSet,
                                   Commit.PatchSetComment patchSetComment) {
        Identity reviewer = checkNotNull(patchSetComment.getReviewer());
        ReviewerData reviewerData = getOrCreateReviewerForOwnCommit(reviewer);
        reviewerData.commentCount++;

        commentsReceived.addCommentForCommit(commit, patchSetComment);
    }

    public void addCommit(@Nonnull Commit commit) {
        GerritProject project = repositories.computeIfAbsent(commit.project,
                projectName -> GerritProject.fromCommit(commit));
        project.commitCountForUser++;
        commits.add(commit);
        updateActivityTimestamps(commit.lastUpdatedDate);
        updateActivityTimestamps(commit.createdOnDate);
    }

    private void updateActivityTimestamps(long unixEpochMsec) {
        firstActiveDate = Math.min(unixEpochMsec, firstActiveDate);
        lastActiveDate = Math.max(unixEpochMsec, lastActiveDate);

        DateTime dateTime = new DateTime(unixEpochMsec);
        activeDays.add(dateTime.toString("YYYY-MM-DD"));

        activeDayCount = activeDays.size();
    }

    void updateAverageTimeInCodeReview(long commitTimeInCodeReviewMsec) {
        int prevCount = commits.size() - 1;
        long newAverage = averageTimeInCodeReview * prevCount;
        averageTimeInCodeReview = (newAverage + commitTimeInCodeReviewMsec) / commits.size();
    }

    private static String formatPrintableDuration(long duration) {
        int durationInSecs = (int) (duration / 1000);
        int days = durationInSecs / (60 * 60 * 24);
        int hours = durationInSecs / (60 * 60) % 24;
        int minutes = durationInSecs / (60) % 60;

        return String.format("%dd %dh %dmin", days, hours, minutes);
    }
}
