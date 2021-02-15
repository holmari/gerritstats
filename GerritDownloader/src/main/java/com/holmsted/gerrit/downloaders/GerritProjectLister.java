package com.holmsted.gerrit.downloaders;

import com.holmsted.gerrit.GerritServer;

import java.util.List;

import javax.annotation.Nonnull;

public abstract class GerritProjectLister {

    @Nonnull
    private final GerritServer gerritServer;

    public GerritProjectLister(@Nonnull GerritServer gerritServer) {
        this.gerritServer = gerritServer;
    }

    @Nonnull
    public GerritServer getGerritServer() {
        return gerritServer;
    }

    @Nonnull
    public abstract List<String> getProjectListing();
}
