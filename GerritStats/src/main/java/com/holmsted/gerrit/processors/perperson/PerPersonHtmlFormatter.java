package com.holmsted.gerrit.processors.perperson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.holmsted.file.FileWriter;
import com.holmsted.gerrit.Commit;
import com.holmsted.gerrit.Commit.Identity;
import com.holmsted.gerrit.OutputRules;
import com.holmsted.gerrit.processors.CommitDataProcessor;
import com.holmsted.gerrit.processors.perperson.IdentityRecord.ReviewerData;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Nonnull;

class PerPersonHtmlFormatter implements CommitDataProcessor.OutputFormatter<PerPersonData> {
    private static final String RES_OUTPUT_DIR = "res";

    private static final String[] JS_RESOURCES = {
            "d3.min.js",
            "style.css",
            "jquery.min.js",
            "jquery.tablesorter.min.js",
            "numeral.min.js",
            "bootstrap.css",
            "bootstrap.min.js",
            "moment.min.js",
            "gerritstats.js",
            "tablecellhighlighter.js",
            "selectedusers.js",
            "proximitygraph.js"
    };

    private static final String[] HTML_RESOURCES = {
            "index.html",
            "profile.html"
    };

    private File outputDir;
    private File resOutputDir;

    public PerPersonHtmlFormatter(@Nonnull OutputRules outputRules) {
        outputDir = new File(outputRules.getOutputDir());
        resOutputDir = new File(outputDir.getAbsolutePath() + File.separator + RES_OUTPUT_DIR);
    }

    @Override
    public void format(@Nonnull PerPersonData data) {
        if (!resOutputDir.exists() && !resOutputDir.mkdirs()) {
            throw new IOError(new IOException("Cannot create output directory " + outputDir.getAbsolutePath()));
        }

        IdentityRecordList orderedList = data.toOrderedList(new AlphabeticalOrderComparator());

        createOverviewJs(data, orderedList);
        createPerPersonFiles(orderedList);

        copyFilesToTarget(resOutputDir, JS_RESOURCES);
        copyFilesToTarget(outputDir, HTML_RESOURCES);

        System.out.println("Output written to " + outputDir.getAbsolutePath());
    }

    private void createOverviewJs(PerPersonData perPersonData, IdentityRecordList identityRecords) {
        String outputFilename = "overview.js";
        System.out.println("Creating " + outputFilename);

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(IdentityRecord.class, new IdentityRecordOverviewSerializer())
                .create();

        StringWriter writer = new StringWriter();
        writer.write(String.format("overviewUserdata = %s;",
                gson.toJson(identityRecords)));

        JsonObject datasetOverview = new JsonObject();
        datasetOverview.add("projectName", gson.toJsonTree(perPersonData.getQueryData().getDisplayableProjectName()));
        datasetOverview.add("filenames", gson.toJsonTree(perPersonData.getQueryData().getFilenames()));
        datasetOverview.add("branchList", gson.toJsonTree(perPersonData.getQueryData().getDisplayableBranchList()));
        datasetOverview.add("fromDate", gson.toJsonTree(perPersonData.getFromDate()));
        datasetOverview.add("toDate", gson.toJsonTree(perPersonData.getToDate()));
        datasetOverview.add("generatedDate", gson.toJsonTree(new Date().getTime()));

        writer.write('\n');
        writer.write(String.format("datasetOverview = %s;",
                gson.toJson(datasetOverview)));

        FileWriter.writeFile(outputDir.getPath()
                + File.separator + "overview"
                + File.separator + outputFilename, writer.toString());
    }

    private void copyFilesToTarget(File outputDir, String... filenames) {
        for (String filename : filenames) {
            copyFileToTarget(outputDir, filename);
        }
    }

    private void copyFileToTarget(File outputDir, String filename) {
        InputStream stream = safeOpenFileResource(filename);
        FileWriter.writeFile(outputDir.getAbsolutePath() + File.separator + filename, stream);
    }

    @Nonnull
    private static InputStream safeOpenFileResource(String resourceName) {
        ClassLoader classLoader = PerPersonHtmlFormatter.class.getClassLoader();
        InputStream stream = classLoader.getResourceAsStream(resourceName);
        if (stream == null) {
            throw new IOError(new IOException("File not found in jar: " + resourceName));
        }
        return stream;
    }

    private void createPerPersonFiles(@Nonnull IdentityRecordList orderedList) {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(PatchSetCommentTable.class, new PatchSetCommentTableSerializer())
                .registerTypeAdapter(ReviewerDataTable.class, new ReviewerDataTableSerializer())
                .registerTypeAdapter(Identity.class, new IdentitySerializer())
                .create();

        for (IdentityRecord record : orderedList) {
            writeUserdataJsonFile(record, gson);
        }
    }

    /**
     * Writes a .js file with a json object for the given identity record.
     *
     *
     * Ideally, pure .json files would be written,but it's not easily possible
     * to load json files locally from the pages without a web server to serve the
     * requests.
     *
     * See e.g. http://stackoverflow.com/questions/7346563/loading-local-json-file
     */
    private void writeUserdataJsonFile(@Nonnull IdentityRecord record, @Nonnull Gson gson) {
        String outputFilename = record.getFilenameStem() + ".js";
        System.out.println("Creating " + outputFilename);

        StringWriter writer = new StringWriter();
        writer.write(String.format("userdata['%s'] = %s;",
                record.getFilenameStem(),
                gson.toJson(record)));

        FileWriter.writeFile(outputDir.getPath()
                + File.separator + "userdata"
                + File.separator + outputFilename, writer.toString());
    }

    private static class PatchSetCommentTableSerializer implements JsonSerializer<PatchSetCommentTable> {

        @Override
        public JsonElement serialize(PatchSetCommentTable table,
                                     Type typeOfSrc,
                                     JsonSerializationContext context) {
            JsonArray tableJson = new JsonArray();
            for (Commit key : table.keySet()) {
                JsonObject pair = new JsonObject();
                pair.add("commit", context.serialize(key));
                pair.add("commentsByUser", context.serialize(table.get(key)));
                tableJson.add(pair);
            }
            return tableJson;
        }
    }

    private static class ReviewerDataTableSerializer implements JsonSerializer<ReviewerDataTable> {
        @Override
        public JsonElement serialize(ReviewerDataTable table,
                                     Type typeOfSrc,
                                     JsonSerializationContext context) {
            JsonArray tableJson = new JsonArray();
            for (Commit.Identity key : table.keySet()) {
                JsonObject pair = new JsonObject();
                pair.add("identity", context.serialize(key));
                pair.add("approvalData", context.serialize(table.get(key)));
                tableJson.add(pair);
            }
            return tableJson;
        }
    }

    private static class IdentityRecordOverviewSerializer implements JsonSerializer<IdentityRecord> {

        @Override
        public JsonElement serialize(IdentityRecord identityRecord,
                                     Type typeOfSrc,
                                     JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.add("identifier", context.serialize(identityRecord.getFilenameStem()));
            json.add("identity", context.serialize(identityRecord.identity));
            json.add("reviewCountPlus2", context.serialize(identityRecord.getReviewCountPlus2()));
            json.add("reviewCountPlus1", context.serialize(identityRecord.getReviewCountPlus1()));
            json.add("reviewCountMinus1", context.serialize(identityRecord.getReviewCountMinus1()));
            json.add("reviewCountMinus2", context.serialize(identityRecord.getReviewCountMinus2()));
            json.add("allCommentsWritten", context.serialize(identityRecord.getAllCommentsWritten().size()));
            json.add("allCommentsReceived", context.serialize(identityRecord.getAllCommentsReceived().size()));
            json.add("commitCount", context.serialize(identityRecord.getCommits().size()));
            json.add("averageTimeInCodeReview", context.serialize(identityRecord.getAverageTimeInCodeReview()));
            json.add("receivedCommentRatio", context.serialize(identityRecord.getReceivedCommentRatio()));
            json.add("reviewCommentRatio", context.serialize(identityRecord.getReviewCommentRatio()));
            json.add("addedAsReviewerToCount", context.serialize(identityRecord.addedAsReviewerTo.size()));

            List<JsonObject> reviewerList = new ArrayList<>();
            for (Identity reviewer : identityRecord.getMyReviewerList()) {
                JsonObject reviewerRecord = new JsonObject();
                ReviewerData reviewerData = identityRecord.getReviewerDataForOwnCommitFor(reviewer);
                reviewerRecord.add("identity", context.serialize(reviewer));
                reviewerRecord.add("reviewData", context.serialize(reviewerData));
                reviewerList.add(reviewerRecord);
            }
            json.add("myReviewerList", context.serialize(reviewerList));
            return json;
        }
    }

    private static class IdentitySerializer implements JsonSerializer<Commit.Identity> {
        @Override
        public JsonElement serialize(Identity identity, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.add("identifier", context.serialize(identity.getIdentifier()));
            json.add("name", context.serialize(identity.name));
            json.add("email", context.serialize(identity.email));
            json.add("username", context.serialize(identity.username));
            return json;
        }
    }
}
