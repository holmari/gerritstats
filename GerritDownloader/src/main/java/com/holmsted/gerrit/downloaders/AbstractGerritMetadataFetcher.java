package com.holmsted.gerrit.downloaders;

import java.util.List;

import javax.annotation.Nonnull;

import com.holmsted.gerrit.GerritServer;
import com.holmsted.gerrit.GerritVersion;

public abstract class AbstractGerritMetadataFetcher {
    
    private final GerritServer gerritServer;

    public AbstractGerritMetadataFetcher(@Nonnull GerritServer gerritServer) {
        this.gerritServer = gerritServer;
    }

    public GerritServer getGerritServer() {
        return gerritServer;
    }

    public abstract List<String> listProjects();
    public abstract GerritVersion getVersion();
}
