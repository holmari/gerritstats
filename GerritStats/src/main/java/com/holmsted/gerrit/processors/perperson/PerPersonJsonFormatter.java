package com.holmsted.gerrit.processors.perperson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.holmsted.file.FileWriter;
import com.holmsted.gerrit.Commit;
import com.holmsted.gerrit.Commit.Identity;
import com.holmsted.gerrit.OutputRules;
import com.holmsted.gerrit.processors.CommitDataProcessor;
import com.holmsted.gerrit.processors.perperson.IdentityRecord.ReviewerData;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

@SuppressWarnings("PMD.ExcessiveImports")
class PerPersonJsonFormatter implements CommitDataProcessor.OutputFormatter<PerPersonData> {
    private static final String RES_OUTPUT_DIR = ".";
    private static final String DATA_PATH = ".";

    private final File outputDir;
    private final File resOutputDir;

    private final Map<String, Identity> identities = new HashMap<>();

    public PerPersonJsonFormatter(@Nonnull OutputRules outputRules) {
        outputDir = new File(outputRules.getOutputDir());
        resOutputDir = new File(outputDir.getAbsolutePath() + File.separator + RES_OUTPUT_DIR);
    }

    @Override
    public void format(@Nonnull PerPersonData data) {
        if (!resOutputDir.exists() && !resOutputDir.mkdirs()) {
            throw new IOError(new IOException("Cannot create output directory " + outputDir.getAbsolutePath()));
        }

        IdentityRecordList orderedList = data.toOrderedList(new AlphabeticalOrderComparator());

        for (Identity identity : data.keySet()) {
            identities.put(identity.getIdentifier(), identity);
        }

        createOverviewJs(orderedList);
        createDatasetOverviewJs(data);
        createPerPersonFiles(orderedList);
        createIdsJs();

        System.out.println("Output written to " + outputDir.getAbsolutePath());
    }

    private void createIdsJs() {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Identity.class, new IdentitySerializer())
                .create();

        new JsonFileBuilder(outputDir)
                .setOutputFilename("ids.js")
                .setMemberName("ids")
                .setSerializedJs(gson.toJson(identities))
                .build();
    }

    private void createOverviewJs(IdentityRecordList identityRecords) {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Identity.class, new IdentityMappingSerializer())
                .registerTypeAdapter(IdentityRecord.class, new IdentityRecordOverviewSerializer())
                .create();

        String json = gson.toJson(identityRecords);
        json = IdentityMappingSerializer.postprocess(json);

        new JsonFileBuilder(outputDir)
                .setOutputFilename("overview.js")
                .setMemberName("overviewUserdata")
                .setSerializedJs(json)
                .build();
    }

    private void createDatasetOverviewJs(PerPersonData perPersonData) {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Identity.class, new IdentityMappingSerializer())
                .registerTypeAdapter(IdentityRecord.class, new IdentityRecordOverviewSerializer())
                .create();

        JsonObject datasetOverview = new JsonObject();
        datasetOverview.add("projectName", gson.toJsonTree(perPersonData.getQueryData().getDisplayableProjectName()));
        datasetOverview.add("filenames", gson.toJsonTree(perPersonData.getQueryData().getFilenames()));
        datasetOverview.add("branchList", gson.toJsonTree(perPersonData.getQueryData().getBranches()));
        datasetOverview.add("fromDate", gson.toJsonTree(perPersonData.getFromDate()));
        datasetOverview.add("toDate", gson.toJsonTree(perPersonData.getToDate()));
        datasetOverview.add("generatedDate", gson.toJsonTree(new Date().getTime()));
        datasetOverview.add("hashCode", gson.toJsonTree(perPersonData.getQueryData().getDatasetKey()));
        datasetOverview.add("gerritVersion", gson.toJsonTree(perPersonData.getQueryData().getMinGerritVersion()));

        new JsonFileBuilder(outputDir)
                .setOutputFilename("datasetOverview.js")
                .setMemberName("datasetOverview")
                .setSerializedJs(gson.toJson(datasetOverview))
                .build();
    }

    private void createPerPersonFiles(@Nonnull IdentityRecordList orderedList) {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(PatchSetCommentTable.class, new PatchSetCommentTableSerializer())
                .registerTypeAdapter(ReviewerDataTable.class, new ReviewerDataTableSerializer())
                .registerTypeAdapter(Identity.class, new IdentityMappingSerializer())
                .registerTypeAdapterFactory(new IdentityRecordTypeAdapterFactory())
                .create();

        for (IdentityRecord record : orderedList) {
            // add any potentially missing ids to the table
            record.getMyReviewerList().stream().forEach(identity ->
                    identities.put(identity.getIdentifier(), identity));
            record.getReviewRequestorList().stream().forEach(identity ->
                    identities.put(identity.getIdentifier(), identity));

            String json = gson.toJson(record);
            json = IdentityMappingSerializer.postprocess(json);

            writeUserdataJsonFile(record, json);
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
    private void writeUserdataJsonFile(@Nonnull IdentityRecord record, @Nonnull String json) {
        String outputFilename = record.getFilenameStem() + ".js";
        System.out.println("Creating " + outputFilename);

        StringWriter writer = new StringWriter();
        writer.write(String.format("userdata['%s'] = %s;",
                record.getFilenameStem(),
                json));

        FileWriter.writeFile(outputDir.getPath()
                + File.separator + DATA_PATH
                + File.separator + "users"
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
            for (Map.Entry<Identity, ReviewerData> entry : table.entrySet()) {
                JsonObject pair = new JsonObject();
                pair.add("identity", context.serialize(entry.getKey()));
                pair.add("approvalData", context.serialize(entry.getValue()));
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
            json.add("selfReviewedCommitCount", context.serialize(identityRecord.getSelfReviewedCommits().size()));
            json.add("abandonedCommitCount", context.serialize(identityRecord.getAbandonedCommitCount()));
            json.add("firstActiveDate", context.serialize(identityRecord.firstActiveDate));
            json.add("lastActiveDate", context.serialize(identityRecord.lastActiveDate));

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

    private static class IdentityRecordTypeAdapterFactory implements TypeAdapterFactory {

        @Override
        public <T> TypeAdapter<T> create(final Gson gson, TypeToken<T> type) {
            if (!IdentityRecord.class.isAssignableFrom(type.getRawType())) {
                return null;
            }

            final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
            final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
            return new TypeAdapter<T>() {
                @Override
                public void write(JsonWriter writer, T value) throws IOException {
                    JsonElement tree = delegate.toJsonTree(value);

                    IdentityRecord record = (IdentityRecord) value;
                    JsonObject object = (JsonObject) tree;
                    object.add("abandonedCommitCount", new JsonPrimitive(record.getAbandonedCommitCount()));
                    object.add("projects", gson.toJsonTree(record.getGerritProjects()));
                    object.add("selfReviewedCommitCount", gson.toJsonTree(record.getSelfReviewedCommits().size()));
                    object.add("inReviewCommitCount", new JsonPrimitive(record.getInReviewCommitCount()));

                    elementAdapter.write(writer, tree);
                }

                @Override
                public T read(JsonReader reader) throws IOException {
                    JsonElement tree = elementAdapter.read(reader);
                    return delegate.fromJsonTree(tree);
                }
            };
        }
    }

    private class IdentitySerializer implements JsonSerializer<Commit.Identity> {
        @Override
        public JsonElement serialize(Identity identity, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            String identifier = identity.getIdentifier();
            json.add("identifier", context.serialize(identifier));
            json.add("name", context.serialize(identity.name));
            json.add("email", context.serialize(identity.email));
            json.add("username", context.serialize(identity.username));

            // There can be some identities in the reviewer data that are not in the per-person data.
            // To make sure all the references to users map work, add them here.
            if (!identities.containsKey(identifier)) {
                identities.put(identifier, identity);
            }

            return json;
        }
    }

    /**
     * This hacky mapping reduces the .json file sizes by about 30%, by using a variable reference
     * for all identities.
     *
     * Because the writer methods in gson are final, it doesn't seem possible to
     * write e.g. variable references in the code, so any '__$$users[' strings are replaced
     * with a real variable reference in a postprocessing step.
     */
    private static class IdentityMappingSerializer implements JsonSerializer<Commit.Identity> {
        @Override
        public JsonElement serialize(Identity identity, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive("__$$ids[" + identity.getIdentifier() + "]");
        }

        /**
         * Processes the passed json string so that all found __$$users[] instances are replaced
         * with actual array references.
         */
        public static String postprocess(String serializedJson) {
            return serializedJson.replaceAll("\"__\\$\\$ids\\[(.+)\\]\"", "ids[\"$1\"]");
        }
    }

    private static class JsonFileBuilder {

        @Nonnull
        final File outputDir;

        String outputFilename;
        String serializedJs;
        String memberVariableName;

        public JsonFileBuilder(@Nonnull File outputDir) {
            this.outputDir = outputDir;
        }

        public JsonFileBuilder setOutputFilename(@Nonnull String outputFilename) {
            this.outputFilename = outputFilename;
            System.out.println("Creating " + this.outputFilename);
            return this;
        }

        public JsonFileBuilder setMemberName(String memberVariableName) {
            this.memberVariableName = memberVariableName;
            return this;
        }

        public JsonFileBuilder setSerializedJs(String serializedJs) {
            this.serializedJs = serializedJs;
            return this;
        }

        public void build() {
            StringWriter writer = new StringWriter();

            writer.write(String.format("var %s = %s;",
                    memberVariableName,
                    serializedJs));

            FileWriter.writeFile(outputDir.getPath()
                    + File.separator + DATA_PATH
                    + File.separator + outputFilename, writer.toString());
        }
    }
}
