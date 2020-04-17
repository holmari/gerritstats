package com.holmsted.gerrit.downloaders.rest;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.holmsted.gerrit.GerritStatsVersion;
import com.holmsted.gerrit.GerritVersion;
import com.holmsted.gerrit.downloaders.AbstractDataReader;
import com.holmsted.gerrit.downloaders.AbstractGerritStatsDownloader;
import com.holmsted.json.JsonUtils;

/**
 * Unlike the SSH version, this has no LegacyReader for Gerrit versions prior to
 * 2.9 because I had no access to old systems for testing.
 */
public class GerritRestStatsDownloader extends AbstractGerritStatsDownloader {

    @Nonnull
    private final GerritVersion gerritVersion;

    private static final String MORE_CHANGES = "_more_changes";

    /**
     * Represents metadata returned within the last object by the REST API:
     * 
     * <pre>
     *       "_more_changes": true
     * </pre>
     * 
     * Lacks stats returned by the Gerrit query API such as running time.
     */
    static class QueryMetadata {
        final int rowCount;
        final boolean moreChanges;

        static QueryMetadata fromJSON(JSONObject lastLineData, int rowCount) {
            return new QueryMetadata(lastLineData, rowCount);
        }

        private QueryMetadata(JSONObject metadata, int rowCount) {
            this.moreChanges = metadata.optBoolean(MORE_CHANGES);
            this.rowCount = rowCount;
        }
    }

    static class GerritOutput {
        private final List<JSONObject> output = new ArrayList<>();
        private final QueryMetadata metadata;

        @Nonnull
        private final GerritVersion gerritVersion;

        public GerritOutput(@Nonnull String text, @Nonnull GerritVersion gerritVersion) {
            // Answers an array of changes (JSON objects)
            JSONArray changes = JsonUtils.readJsonArray(text);
            JSONObject lastChange = changes.getJSONObject(changes.length() - 1);
            this.metadata = Preconditions.checkNotNull(QueryMetadata.fromJSON(lastChange, changes.length()));
            for (int i = 0; i < changes.length(); ++i) {
                JSONObject project = changes.getJSONObject(i);
                this.output.add(project);
            }
            this.gerritVersion = gerritVersion;
        }

        public boolean hasMoreChanges() {
            return metadata.moreChanges;
        }

        public int getRowCount() {
            return metadata.rowCount;
        }

        public List<JSONObject> getOutput() {
            return output;
        }
    }

    /**
     * Reads changes data from Gerrit versions 2.9 and higher.
     */
    static class DefaultDataReader extends AbstractDataReader {
        private int startOffset;

        public void setStartOffset(int startOffset) {
            this.startOffset = startOffset;
        }

        public GerritOutput readData() {
            String gerritQuery = getGerritQuery();
            GerritRestCommand restCommand = new GerritRestCommand((GerritRestServer) getGerritServer());
            // SSH requests --all-approvals, --all-reviewers, --comments
            // DETAILED_LABELS yields the actual code-review approval value (1 vs 2) plus much unneeded detail
            String outputFields = "&o=ALL_REVISIONS&o=ALL_COMMITS&o=DETAILED_ACCOUNTS&o=LABELS&o=MESSAGES";
            String urlSuffix = String.format("/r/changes/?q=%s%s%s", //
                    gerritQuery, outputFields, createStartOffsetArg());
            String output = restCommand.exec(urlSuffix);
            return new GerritOutput(Strings.nullToEmpty(output), getGerritVersion());
        }

        @Nonnull
        @Override
        public List<JSONObject> readUntilLimit() {
            List<JSONObject> items = new ArrayList<>();
            boolean hasMoreChanges = true;
            int rowCount = 0;

            while (hasMoreChanges
                    && (rowCount < getOverallCommitLimit() || getOverallCommitLimit() == NO_COMMIT_LIMIT)) {
                GerritOutput gerritOutput = readData();
                items.addAll(gerritOutput.getOutput());

                hasMoreChanges = gerritOutput.hasMoreChanges();
                rowCount += gerritOutput.getRowCount();
                setStartOffset(startOffset + gerritOutput.getRowCount());
            }

            return items;
        }

        @Override
        public void setGerritQuery(String projectName, String afterDate, String beforeDate) {
            if (projectName.isEmpty()) {
                throw new IllegalStateException("No project name defined!");
            }
            this.gerritQuery = String.format("project:^%s", projectName);
            if (afterDate != null)
                this.gerritQuery += String.format("+after:%s", afterDate);
            if (beforeDate != null)
                this.gerritQuery += String.format("+before:%s", beforeDate);
        }

        private String createStartOffsetArg() {
            return startOffset != 0 ? "&start=" + startOffset : "";
        }
    }

    public GerritRestStatsDownloader(@Nonnull GerritRestServer gerritServer, @Nonnull GerritVersion gerritVersion) {
        super(gerritServer);
        this.gerritVersion = gerritVersion;
    }

    public int getGerritStatsVersion() {
        return GerritStatsVersion.REST_CHANGE.ordinal();
    }

    /**
     * Reads the data in JSON format from gerrit.
     */
    @Nonnull
    public List<JSONObject> readData() {
        if (getOverallCommitLimit() != NO_COMMIT_LIMIT) {
            System.out.println(String.format("Reading data from %s for last %d commits", getGerritServer(),
                    getOverallCommitLimit()));
        } else {
            System.out.println("Reading all commit data from " + getGerritServer());
        }
        AbstractDataReader reader = createDataReader();
        return reader.readUntilLimit();
    }

    @Nonnull
    private AbstractDataReader createDataReader() {
        AbstractDataReader reader = new DefaultDataReader();
        reader.setGerritServer(getGerritServer());
        reader.setOverallCommitLimit(getOverallCommitLimit());
        reader.setGerritQuery(getProjectName(), getAfterDate(), getBeforeDate());
        reader.setGerritVersion(gerritVersion);
        return reader;
    }
}
