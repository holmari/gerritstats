package com.holmsted.gerrit.processors.perperson;

import com.holmsted.gerrit.processors.CommitDataProcessor;

import java.util.Locale;

import javax.annotation.Nonnull;

class PerPersonCsvFormatter implements CommitDataProcessor.OutputFormatter<PerPersonData> {

    @Override
    public void format(@Nonnull PerPersonData data) {
        IdentityRecordList orderedList = data.toOrderedList(new CommentsWrittenComparator());

        PerPersonPlaintextFormatter.printHeader(data);

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
            builder.append(String.format("%34s\t%7d\t%16d\t%17d\t%20f\t%17d\t%20f\t%20f\t%19d\t%16d\t%16d\t%16d\t%16d\t%30d\n",
                    record.identity.email,
                    record.commits.size(),
                    record.commentsWritten.size(),
                    record.commentsReceived.size(),
                    record.getReceivedCommentRatio(),
                    record.addedAsReviewerTo.size(),
                    record.getReviewCommentRatio(),
                    record.getAveragePatchSetCount(),
                    record.getMaxPatchSetCount(),
                    record.reviewCountPlus2,
                    record.reviewCountPlus1,
                    record.reviewCountMinus1,
                    record.reviewCountMinus2,
                    record.reviewersForOwnCommits.size(), Locale.getDefault()));
        }
        System.out.println(builder.toString());
    }
}
