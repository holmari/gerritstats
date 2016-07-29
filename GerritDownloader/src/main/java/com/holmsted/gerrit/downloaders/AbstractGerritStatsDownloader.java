package com.holmsted.gerrit.downloaders;

import com.holmsted.gerrit.GerritServer;

import org.json.JSONObject;

import java.util.List;

import javax.annotation.Nonnull;

public abstract class AbstractGerritStatsDownloader {
    public static final int NO_COMMIT_LIMIT = -1;

    private GerritServer gerritServer;
    private String projectName;

    private int overallCommitLimit = NO_COMMIT_LIMIT;

    public AbstractGerritStatsDownloader(@Nonnull GerritServer gerritServer) {
        this.gerritServer = gerritServer;
    }

    @Nonnull
    public GerritServer getGerritServer() {
        return gerritServer;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectName() {
        return projectName;
    }

    /**
     * Sets how many commits' stats are downloaded. If this number exceeds the server limit,
     * multiple requests will be made to fulfill the goal.
     * <p>
     * Note: this does not get respected if the limit is not a multiple of the server limit.
     */
    public void setOverallCommitLimit(int overallLimit) {
        overallCommitLimit = overallLimit;
    }

    public int getOverallCommitLimit() {
        return overallCommitLimit;
    }

    /**
     * Reads data from the server. Returns data in JSON-like format, which can be parsed by
     * GerritStats tool.
     */
    public abstract List<JSONObject> readData();
}
