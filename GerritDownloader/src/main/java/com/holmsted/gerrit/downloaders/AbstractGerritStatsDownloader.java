package com.holmsted.gerrit.downloaders;

import java.util.List;

import javax.annotation.Nonnull;

import org.json.JSONObject;

import com.holmsted.gerrit.GerritServer;

public abstract class AbstractGerritStatsDownloader {
    public static final int NO_COMMIT_LIMIT = -1;

    @Nonnull
    private final GerritServer gerritServer;
    private String projectName;
    private int overallCommitLimit = NO_COMMIT_LIMIT;
    private String afterDate;
    private String beforeDate;

    public AbstractGerritStatsDownloader(@Nonnull GerritServer gerritServer) {
        this.gerritServer = gerritServer;
    }

    /**
     * Answers the file-format version written by the downloader.
     *
     * @return Integer
     */
    public abstract int getGerritStatsVersion();

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
     * Sets how many commits' stats are downloaded. If this number exceeds the
     * server limit, multiple requests will be made to fulfill the goal.
     * <p>
     * Note: this does not get respected if the limit is not a multiple of the
     * server limit.
     * 
     * @param overallLimit Commit limit
     */
    public void setOverallCommitLimit(int overallLimit) {
        overallCommitLimit = overallLimit;
    }

    public int getOverallCommitLimit() {
        return overallCommitLimit;
    }

    public void setAfterDate(String date) {
        afterDate = date;
    }

    public void setBeforeDate(String date) {
        beforeDate = date;
    }

    public String getAfterDate() {
        return afterDate;
    }

    public String getBeforeDate() {
        return beforeDate;
    }

    /**
     * Queries the server for data using the project name, limit and other field
     * values. Returns data in JSON-like format, which can be parsed by GerritStats
     * tool.
     * 
     * @return List of JSONObject
     */
    public abstract List<JSONObject> readData();
}
