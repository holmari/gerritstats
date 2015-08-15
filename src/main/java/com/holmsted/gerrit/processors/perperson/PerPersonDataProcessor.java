package com.holmsted.gerrit.processors.perperson;

import com.holmsted.gerrit.Commit;
import com.holmsted.gerrit.CommitFilter;
import com.holmsted.gerrit.Output;
import com.holmsted.gerrit.OutputRules;
import com.holmsted.gerrit.OutputType;
import com.holmsted.gerrit.QueryData;
import com.holmsted.gerrit.processors.CommitDataProcessor;
import com.holmsted.gerrit.processors.CommitVisitor;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nonnull;

import file.FileWriter;

public class PerPersonDataProcessor extends CommitDataProcessor<PerPersonData> {

    static class PerPersonCsvFormatter implements OutputFormatter<PerPersonData> {

        @Override
        public void format(@Nonnull PerPersonData data) {
            List<IdentityRecord> orderedList = data.toOrderedList(new CommentsWrittenComparator());

            printHeader(data);

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

    static class PerPersonPlaintextFormatter implements OutputFormatter<PerPersonData> {

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
    }

    static class PerPersonHtmlFormatter implements OutputFormatter<PerPersonData> {
        private static final String DEFAULT_OUTPUT_DIR = "out";
        private static final String TEMPLATES_RES_PATH = "templates";
        private static final String VM_PERSON_PROFILE = TEMPLATES_RES_PATH + File.separator + "person_profile.vm";
        private OutputRules outputRules;

        public PerPersonHtmlFormatter(@Nonnull OutputRules outputRules) {
            this.outputRules = outputRules;
        }

        @Override
        public void format(@Nonnull PerPersonData data) {
            File outputDir = new File(DEFAULT_OUTPUT_DIR);
            if (!outputDir.exists() && !outputDir.mkdirs()) {
                throw new IOError(new IOException("Cannot create output directory " + outputDir.getAbsolutePath()));
            }
            VelocityEngine velocity = new VelocityEngine();
            velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
            velocity.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
            velocity.init();

            List<IdentityRecord> orderedList = data.toOrderedList(new AlphabeticalOrderComparator());

            for (IdentityRecord record : orderedList) {
                String outputFilename = getOutputFilenameForIdentity(record.identity);
                System.out.println("Creating " + outputFilename);

                Context context = new VelocityContext();
                context.put("outputRules", outputRules);
                context.put("identity", record.identity);
                context.put("record", record);

                StringWriter writer = new StringWriter();
                velocity.mergeTemplate(VM_PERSON_PROFILE, "UTF-8", context, writer);

                FileWriter.writeFile(outputDir.getPath() + File.separator + outputFilename, writer.toString());
            }

            System.out.println("Output written to " + outputDir.getAbsolutePath());
        }

        private static String getOutputFilenameForIdentity(@Nonnull Commit.Identity identity) {
            return identity.username + ".html";
        }
    }

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

    static void printHeader(@Nonnull PerPersonData records) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        QueryData queryData = records.getQueryData();
        System.out.println("Project: " + queryData.getDisplayableProjectName());
        System.out.println("Branches: " + queryData.getDisplayableBranchList());
        System.out.println("From: " + dateFormat.format(new Date(records.getFromDate())));
        System.out.println("To: " + dateFormat.format(new Date(records.getToDate())));
        System.out.println("");
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
