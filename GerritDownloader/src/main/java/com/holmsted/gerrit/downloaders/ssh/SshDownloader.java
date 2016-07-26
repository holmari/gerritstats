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

    private String projectName;
    private int perQueryCommitLimit = NO_COMMIT_LIMIT;
    private int overallCommitLimit = NO_COMMIT_LIMIT;

    class GerritOutput {
        private String output;
        private int rowCount;
        private int runtimeMsec;
        private boolean moreChanges;
        private int lastLineStartIndex = -1;

        public GerritOutput(@Nonnull String output) {
            this.output = output;
            int lastLineBreak = output.lastIndexOf('\n');
            if (lastLineBreak != -1) {
                lastLineStartIndex = output.lastIndexOf('\n', lastLineBreak - 1);
                if (lastLineStartIndex != -1) {
                    JSONObject metadata = JsonUtils.readJsonString(this.output.substring(lastLineStartIndex));
                    moreChanges = metadata.optBoolean("moreChanges");
                    rowCount = metadata.optInt("rowCount");
                    runtimeMsec = metadata.optInt("runTimeMilliseconds");
                }
            }
        }

        public boolean hasMoreChanges() {
            return moreChanges;
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

    class GerritDataReader {
        private int startOffset;

        private Version gerritVersion;

        public GerritDataReader(@Nonnull Version gerritVersion) {
            this.gerritVersion = gerritVersion;
        }

        public void setStartOffset(int startOffset) {
            this.startOffset = startOffset;
        }

        public GerritOutput readData() {
            String projectNameList = createProjectNameList();
            GerritSshCommand sshCommand = new GerritSshCommand(getGerritServer());
            String reviewersArg = gerritVersion.isAtLeast(2, 9) ? "--all-reviewers " : "";
            String output = sshCommand.exec(String.format("query %s "
                            + "--format=JSON "
                            + "--all-approvals "
                            + "--comments "
                            + "%s "
                            + createStartOffsetArg()
                            + createLimitArg(),
                    projectNameList,
                    reviewersArg
                    ));

            return new GerritOutput(Strings.nullToEmpty(output));
        }

        private String createStartOffsetArg() {
            return startOffset != 0 ? "--start " + String.valueOf(startOffset) + " " : "";
        }
    }

    public SshDownloader(@Nonnull GerritServer gerritServer, @Nonnull Version gerritVersion) {
        super(gerritServer);
        this.gerritVersion = gerritVersion;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * Fetch only x commits per query.
     * Server maximum is respected; it's typically 500 or something similar.
     */
    public void setPerQueryCommitLimit(int limit) {
        perQueryCommitLimit = limit;
    }

    /**
     * Sets how many commits' stats are downloaded. If this number exceeds the server limit,
     * multiple requests will be made to fulfill the goal.
     * <p>
     * Note: this does not get respected if the limit is not a multiple of the server limit.
     */
    public void setCommitLimit(int overallLimit) {
        overallCommitLimit = overallLimit;
    }

    /**
     * Reads the data in json format from gerrit.
     */
    public String readData() {
        if (overallCommitLimit != NO_COMMIT_LIMIT) {
            System.out.println("Reading data from " + getGerritServer() + " for last " + overallCommitLimit + " commits");
        } else {
            System.out.println("Reading all commit data from " + getGerritServer());
        }

        GerritDataReader connection = new GerritDataReader(gerritVersion);
        StringBuilder builder = new StringBuilder();

        boolean hasMoreChanges = true;
        int offset = 0;
        while (hasMoreChanges && (offset < overallCommitLimit || overallCommitLimit == NO_COMMIT_LIMIT)) {
            connection.setStartOffset(offset);
            GerritOutput gerritOutput = connection.readData();
            if (gerritOutput == null) {
                break;
            }
            builder.append(gerritOutput.toString());
            hasMoreChanges = gerritOutput.hasMoreChanges();
            offset += gerritOutput.getRowCount();
        }

        return builder.toString();
    }

    private String createLimitArg() {
        return perQueryCommitLimit != NO_COMMIT_LIMIT ? "limit:" + String.valueOf(perQueryCommitLimit) : "";
    }

    private String createProjectNameList() {
        StringBuilder builder = new StringBuilder("project:^");
        if (projectName.isEmpty()) {
            throw new IllegalStateException("No project name defined!");
        }
        builder.append(projectName);
        return builder.toString();
    }
}
