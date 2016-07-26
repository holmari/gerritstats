package com.holmsted.gerrit;

import com.holmsted.gerrit.downloaders.ssh.GerritSsh;

import java.util.List;

import javax.annotation.Nonnull;

/**
 * Creates a listing of all Gerrit projects on the given server.
 */
public class GerritProjectLister {

    @Nonnull
    private final GerritServer gerritServer;

    public GerritProjectLister(@Nonnull GerritServer gerritServer) {
        this.gerritServer = gerritServer;
    }

    @Nonnull
    public List<String> getProjectListing() {
        return GerritSsh.listProjects(gerritServer);
    }
}
