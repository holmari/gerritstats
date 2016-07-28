package com.holmsted.gerrit.downloaders.ssh;

import com.google.common.base.Strings;
import com.holmsted.gerrit.GerritServer;
import com.holmsted.gerrit.downloaders.AbstractGerritStatsDownloader;
import com.holmsted.gerrit.downloaders.ssh.GerritSsh.Version;
import com.holmsted.json.JsonUtils;

import org.json.JSONObject;

import javax.annotation.Nonnull;

public class SshDownloader extends AbstractGerritStatsDownloader {

    public static final int NO_COMMIT_LIMIT = -1;

    @Nonnull
    private final Version gerritVersion;

    static class GerritOutput {
        private String output;
        private int rowCount;
        private int runtimeMsec;
        private boolean moreChanges;
        private int lastLineStartIndex = -1;
        private String resumeSortkey;

        @Nonnull
        private final Version gerritVersion;

        public GerritOutput(@Nonnull String output, @Nonnull Version gerritVersion) {
            this.output = output;
            this.gerritVersion = gerritVersion;
            int lastLineBreak = output.lastIndexOf('\n');
            if (lastLineBreak != -1) {
                lastLineStartIndex = output.lastIndexOf('\n', lastLineBreak - 1);
                if (lastLineStartIndex != -1) {
                    JSONObject metadata = JsonUtils.readJsonString(this.output.substring(lastLineStartIndex));
                    moreChanges = metadata.optBoolean("moreChanges");
                    rowCount = metadata.optInt("rowCount");
                    runtimeMsec = metadata.optInt("runTimeMilliseconds");
                    resumeSortkey = metadata.optString("resumeSortKey");
                }
            }
        }

        public boolean hasMoreChanges() {
            if (gerritVersion.isAtLeast(2, 9)) {
                return moreChanges;
            } else {
                return !Strings.nullToEmpty(resumeSortkey).isEmpty();
            }
        }

        public String getResumeSortkey() {
            return resumeSortkey;
        }

        public int getRowCount() {
            return rowCount;
        }

        public int getRuntimeMec() {
            return runtimeMsec;
        }

        @Override
        public String toString() {
            return (lastLineStartIndex != -1) ? output.substring(0, lastLineStartIndex) : output;
        }
    }

    static abstract class DataReader {
        private int overallCommitLimit;
        private String projectNameList;

        private GerritServer gerritServer;
        private Version gerritVersion;

        public abstract String readUntilLimit();

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

        public void setGerritVersion(@Nonnull Version gerritVersion) {
            this.gerritVersion = gerritVersion;
        }

        public Version getGerritVersion() {
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
        public String readUntilLimit() {
            rowCount = 0;

            StringBuilder builder = new StringBuilder();

            String[] statusQueries = {"status:merged", "status:open", "status:abandoned"};

            for (String statusQuery : statusQueries) {
                builder.append(readOutputWithStatusQueryUntilLimit(statusQuery));
            }

            return builder.toString();
        }

        private String readOutputWithStatusQueryUntilLimit(@Nonnull String statusQuery) {
            StringBuilder builder = new StringBuilder();

            boolean hasMoreChanges = true;
            while (hasMoreChanges && (rowCount < getOverallCommitLimit() || getOverallCommitLimit() == NO_COMMIT_LIMIT)) {
                GerritOutput gerritOutput = readOutputWithStatusQuery(statusQuery);
                builder.append(gerritOutput.toString());

                resumeSortkey = gerritOutput.getResumeSortkey();
                hasMoreChanges = gerritOutput.hasMoreChanges();
                rowCount += gerritOutput.getRowCount();
            }

            return builder.toString();
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
        public String readUntilLimit() {
            StringBuilder builder = new StringBuilder();
            boolean hasMoreChanges = true;
            int rowCount = 0;

            while (hasMoreChanges && (rowCount < getOverallCommitLimit() || getOverallCommitLimit() == NO_COMMIT_LIMIT)) {
                GerritOutput gerritOutput = readData();
                builder.append(gerritOutput.toString());

                hasMoreChanges = gerritOutput.hasMoreChanges();
                rowCount += gerritOutput.getRowCount();
                setStartOffset(startOffset + gerritOutput.getRowCount());
            }

            return builder.toString();
        }

        private String createStartOffsetArg() {
            return startOffset != 0 ? "--start " + String.valueOf(startOffset) + " " : "";
        }
    }

    public SshDownloader(@Nonnull GerritServer gerritServer, @Nonnull Version gerritVersion) {
        super(gerritServer);
        this.gerritVersion = gerritVersion;
    }

    /**
     * Reads the data in json format from gerrit.
     */
    public String readData() {
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
