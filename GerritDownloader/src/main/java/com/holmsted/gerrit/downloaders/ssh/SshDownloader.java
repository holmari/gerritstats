package com.holmsted.gerrit.downloaders.ssh;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.holmsted.gerrit.GerritServer;
import com.holmsted.gerrit.GerritVersion;
import com.holmsted.gerrit.downloaders.AbstractGerritStatsDownloader;
import com.holmsted.json.JsonUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

public class SshDownloader extends AbstractGerritStatsDownloader {

    public static final int NO_COMMIT_LIMIT = -1;

    @Nonnull
    private final GerritVersion gerritVersion;

    static class QueryMetadata {
        final int rowCount;
        final int runtimeMsec;
        final boolean moreChanges;
        final String resumeSortkey;

        static QueryMetadata fromOutputString(String output) {
            JSONObject lastLineData = JsonUtils.readJsonString(output);
            if (lastLineData.get("rowCount") != null) {
                return new QueryMetadata(lastLineData);
            } else {
                return null;
            }
        }

        private QueryMetadata(JSONObject metadata) {
            moreChanges = metadata.optBoolean("moreChanges");
            rowCount = metadata.optInt("rowCount");
            runtimeMsec = metadata.optInt("runTimeMilliseconds");
            resumeSortkey = metadata.optString("resumeSortKey");
        }
    }

    static class GerritOutput {
        private final List<JSONObject> output = new ArrayList<>();
        private QueryMetadata metadata;

        @Nonnull
        private final GerritVersion gerritVersion;

        public GerritOutput(@Nonnull String output, @Nonnull GerritVersion gerritVersion) {
            List<String> strings = Arrays.asList(output.split("\n"));

            String lastLine = strings.get(strings.size() - 1);
            this.metadata = Preconditions.checkNotNull(QueryMetadata.fromOutputString(lastLine));

            for (int i = 0; i < strings.size() - 1; ++i) {
                this.output.add(new JSONObject(strings.get(i)));
            }
            this.gerritVersion = gerritVersion;
        }

        public boolean hasMoreChanges() {
            if (gerritVersion.isAtLeast(2, 9)) {
                return metadata.moreChanges;
            } else {
                return !Strings.nullToEmpty(metadata.resumeSortkey).isEmpty();
            }
        }

        public String getResumeSortkey() {
            return metadata.resumeSortkey;
        }

        public int getRowCount() {
            return metadata.rowCount;
        }

        public int getRuntimeMec() {
            return metadata.runtimeMsec;
        }

        public List<JSONObject> getOutput() {
            return output;
        }
    }

    static abstract class DataReader {
        private int overallCommitLimit;
        private String projectNameList;

        private GerritServer gerritServer;
        private GerritVersion gerritVersion;

        public abstract List<JSONObject> readUntilLimit();

        public void setGerritServer(@Nonnull GerritServer gerritServer) {
            this.gerritServer = gerritServer;
        }

        public GerritServer getGerritServer() {
            return gerritServer;
        }

        public void setOverallCommitLimit(int overallCommitLimit) {
            this.overallCommitLimit = overallCommitLimit;
        }

        public int getOverallCommitLimit() {
            return overallCommitLimit;
        }

        public void setProjectNameList(String projectNameList) {
            this.projectNameList = projectNameList;
        }

        public String getProjectNameList() {
            return projectNameList;
        }

        public void setGerritVersion(@Nonnull GerritVersion gerritVersion) {
            this.gerritVersion = gerritVersion;
        }

        public GerritVersion getGerritVersion() {
            return gerritVersion;
        }
    }

    /**
     * Data reader with Gerrit pre-2.9 support. The following problems are being worked around:
     *
     * 1) if no status query is passed, only open commits are listed. So this reader manually
     *    queries for open, merged and abandoned commits.
     * 2) the resume/limit behavior is not implemented in pre-2.9, so resume_sortKey is used instead.
     */
    static class LegacyDataReader extends DataReader {

        private String resumeSortkey;

        private int rowCount;

        @Override
        public List<JSONObject> readUntilLimit() {
            rowCount = 0;

            List<JSONObject> items = new ArrayList<>();

            String[] statusQueries = {"status:merged", "status:open", "status:abandoned"};

            for (String statusQuery : statusQueries) {
                items.addAll(readOutputWithStatusQueryUntilLimit(statusQuery));
            }

            return items;
        }

        private List<JSONObject> readOutputWithStatusQueryUntilLimit(@Nonnull String statusQuery) {
            List<JSONObject> items = new ArrayList<>();

            boolean hasMoreChanges = true;
            while (hasMoreChanges && (rowCount < getOverallCommitLimit() || getOverallCommitLimit() == NO_COMMIT_LIMIT)) {
                GerritOutput gerritOutput = readOutputWithStatusQuery(statusQuery);
                items.addAll(gerritOutput.getOutput());

                resumeSortkey = gerritOutput.getResumeSortkey();
                hasMoreChanges = gerritOutput.hasMoreChanges();
                rowCount += gerritOutput.getRowCount();
            }

            return items;
        }

        private GerritOutput readOutputWithStatusQuery(String statusQuery) {
            String projectNameList = getProjectNameList();
            GerritSshCommand sshCommand = new GerritSshCommand(getGerritServer());
            String resumeSortkeyArg = !Strings.nullToEmpty(resumeSortkey).isEmpty()
                    ?  "resume_sortkey:" + resumeSortkey : "";

            String output = sshCommand.exec(String.format("query %s %s "
                    + "--format=JSON "
                    + "--all-approvals "
                    + "--comments "
                    + "%s ",
                projectNameList,
                statusQuery,
                resumeSortkeyArg
            ));

            return new GerritOutput(Strings.nullToEmpty(output), getGerritVersion());
        }
    }

    /**
     * Reads data from Gerrit versions 2.9 and higher.
     */
    static class DefaultDataReader extends DataReader {
        private int startOffset;

        public void setStartOffset(int startOffset) {
            this.startOffset = startOffset;
        }

        public GerritOutput readData() {
            String projectNameList = getProjectNameList();
            GerritSshCommand sshCommand = new GerritSshCommand(getGerritServer());

            String output = sshCommand.exec(String.format("query %s "
                            + "--format=JSON "
                            + "--all-approvals "
                            + "--comments "
                            + "--all-reviewers "
                            + createStartOffsetArg(),
                    projectNameList
                    ));

            return new GerritOutput(Strings.nullToEmpty(output), getGerritVersion());
        }

        @Override
        public List<JSONObject> readUntilLimit() {
            List<JSONObject> items = new ArrayList<>();
            boolean hasMoreChanges = true;
            int rowCount = 0;

            while (hasMoreChanges && (rowCount < getOverallCommitLimit() || getOverallCommitLimit() == NO_COMMIT_LIMIT)) {
                GerritOutput gerritOutput = readData();
                items.addAll(gerritOutput.getOutput());

                hasMoreChanges = gerritOutput.hasMoreChanges();
                rowCount += gerritOutput.getRowCount();
                setStartOffset(startOffset + gerritOutput.getRowCount());
            }

            return items;
        }

        private String createStartOffsetArg() {
            return startOffset != 0 ? "--start " + String.valueOf(startOffset) + " " : "";
        }
    }

    public SshDownloader(@Nonnull GerritServer gerritServer, @Nonnull GerritVersion gerritVersion) {
        super(gerritServer);
        this.gerritVersion = gerritVersion;
    }

    /**
     * Reads the data in json format from gerrit.
     */
    public List<JSONObject> readData() {
        if (getOverallCommitLimit() != NO_COMMIT_LIMIT) {
            System.out.println("Reading data from " + getGerritServer() + " for last " + getOverallCommitLimit() + " commits");
        } else {
            System.out.println("Reading all commit data from " + getGerritServer());
        }

        DataReader reader = createDataReader();
        return reader.readUntilLimit();
    }

    private DataReader createDataReader() {
        DataReader reader;
        if (gerritVersion.isAtLeast(2, 9)) {
            reader = new DefaultDataReader();
        } else {
            reader = new LegacyDataReader();
        }
        reader.setGerritServer(getGerritServer());
        reader.setOverallCommitLimit(getOverallCommitLimit());
        reader.setProjectNameList(createProjectNameList());
        reader.setGerritVersion(gerritVersion);

        return reader;
    }

    private String createProjectNameList() {
        StringBuilder builder = new StringBuilder("project:^");
        if (getProjectName().isEmpty()) {
            throw new IllegalStateException("No project name defined!");
        }
        builder.append(getProjectName());
        return builder.toString();
    }
}
