package com.holmsted.gerrit.downloaders.ssh;


import com.holmsted.gerrit.GerritServer;
import com.holmsted.gerrit.GerritVersion;
import com.holmsted.gerrit.downloaders.GerritCommitDownloader;
import com.holmsted.gerrit.downloaders.GerritProjectLister;

import javax.annotation.Nonnull;

public class SshDownloaderFactory {

    public static GerritProjectLister createProjectLister(@Nonnull GerritServer gerritServer, @Nonnull GerritVersion gerritVersion) {
        return new SshProjectLister(gerritServer);
    }

    public static GerritCommitDownloader createCommitDownloader(@Nonnull GerritServer gerritServer, @Nonnull GerritVersion gerritVersion) {
        GerritCommitDownloader reader;

        if (gerritVersion.isAtLeast(2, 9)) {
            reader = new SshDefaultCommitDownloader(gerritServer, gerritVersion);
        } else {
            reader = new SshLegacyCommitDownloader(gerritServer, gerritVersion);
        }

        return reader;
    }
}
