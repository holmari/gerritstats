package com.holmsted.gerrit.downloaders;

import com.holmsted.gerrit.GerritServer;

import java.util.List;

import javax.annotation.Nonnull;

public abstract class ProjectLister {

    @Nonnull
    private final GerritServer gerritServer;

    public ProjectLister(@Nonnull GerritServer gerritServer) {
        this.gerritServer = gerritServer;
    }

    @Nonnull
    public GerritServer getGerritServer() {
        return gerritServer;
    }

    @Nonnull
    public abstract List<String> getProjectListing();
}
