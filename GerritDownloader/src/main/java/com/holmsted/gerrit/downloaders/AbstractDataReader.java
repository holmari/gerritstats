package com.holmsted.gerrit.downloaders;

import java.util.List;

import javax.annotation.Nonnull;

import org.json.JSONObject;

import com.holmsted.gerrit.GerritServer;
import com.holmsted.gerrit.GerritVersion;

public abstract class AbstractDataReader {
    
    private int overallCommitLimit;
    protected String gerritQuery;

    private GerritServer gerritServer;
    private GerritVersion gerritVersion;

    @Nonnull
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

    /**
     * Builds the query appropriate for the Gerrit interface
     * 
     * @param projectName project name
     * @param afterDate   Date
     * @param beforeDate  Date
     */
    public abstract void setGerritQuery(String projectName, String afterDate, String beforeDate);

    /**
     * Gets the previously constructed query.
     * 
     * @return Query string
     */
    public String getGerritQuery() {
        return gerritQuery;
    }

    public void setGerritVersion(@Nonnull GerritVersion gerritVersion) {
        this.gerritVersion = gerritVersion;
    }

    public GerritVersion getGerritVersion() {
        return gerritVersion;
    }
}