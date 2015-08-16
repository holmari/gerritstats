package com.holmsted.gerrit.processors.perperson;

import com.holmsted.gerrit.OutputRules;
import com.holmsted.gerrit.QueryData;
import com.holmsted.gerrit.processors.CommitDataProcessor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;

class PerPersonPlaintextFormatter implements CommitDataProcessor.OutputFormatter<PerPersonData> {

    @Nonnull
    private final OutputRules outputRules;

    PerPersonPlaintextFormatter(@Nonnull OutputRules outputRules) {
        this.outputRules = outputRules;
    }

    @Override
    public void format(@Nonnull PerPersonData data) {
        List<IdentityRecord> orderedList = data.toOrderedList(new CommentsWrittenComparator());

        printHeader(data);

        int maxPatchSetCountForList = outputRules.getListCommitsExceedingPatchSetCount();
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
            if (maxPatchSetCountForList != OutputRules.INVALID_PATCH_COUNT) {
                builder.addIndentLine(String.format("Commits exceeding %d patches: %s",
                        maxPatchSetCountForList,
                        record.getCommitsWithNPatchSets(maxPatchSetCountForList)));
            }
            if (outputRules.getListReviewComments()) {
                builder.addIndentLine("Review comments: ");
                builder.addLine(record.getAllReviewComments());
            }
            builder.addLine("");
        }

        System.out.println(builder.toString());
    }

    static void printHeader(@Nonnull PerPersonData records) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        QueryData queryData = records.getQueryData();
        System.out.println("Project: " + queryData.getDisplayableProjectName());
        System.out.println("Branches: " + queryData.getDisplayableBranchList());
        System.out.println("From: " + dateFormat.format(new Date(records.getFromDate())));
        System.out.println("To: " + dateFormat.format(new Date(records.getToDate())));
        System.out.println("");
    }
}
