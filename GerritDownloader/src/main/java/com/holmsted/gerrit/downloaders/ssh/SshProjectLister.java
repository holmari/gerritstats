package com.holmsted.gerrit.downloaders.ssh;

import com.holmsted.gerrit.GerritServer;
import com.holmsted.gerrit.downloaders.GerritProjectLister;

import java.util.List;

import javax.annotation.Nonnull;

/**
 * Creates a listing of all Gerrit projects on the given server.
 */
public class SshProjectLister extends GerritProjectLister {

    public SshProjectLister(@Nonnull GerritServer gerritServer) {
        super(gerritServer);
    }

    @Nonnull
    public List<String> getProjectListing() {
        return SshCommand.getProjectList(getGerritServer());
    }
}
