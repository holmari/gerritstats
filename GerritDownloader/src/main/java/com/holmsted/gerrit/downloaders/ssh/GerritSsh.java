package com.holmsted.gerrit.downloaders.ssh;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import com.holmsted.gerrit.GerritVersion;
import com.holmsted.gerrit.downloaders.AbstractGerritMetadataFetcher;

public final class GerritSsh extends AbstractGerritMetadataFetcher {

    public GerritSsh(@Nonnull GerritSshServer gerritServer) {
        super(gerritServer);
    }

    @Override
    public List<String> listProjects() {
        GerritSshCommand sshCommand = new GerritSshCommand((GerritSshServer) getGerritServer());
        String output = sshCommand.exec("ls-projects");
        List<String> projectList = new ArrayList<>();
        Collections.addAll(projectList, output.split("\n"));
        return projectList;
    }

	@Override
    public GerritVersion getVersion() {
        GerritSshCommand sshCommand = new GerritSshCommand((GerritSshServer) getGerritServer());
        String output = sshCommand.exec("version");
        if (output != null) {
            // answers one line (no quotes or garbage):
            // gerrit version 2.14.20
            return GerritVersion.fromString(output.substring(output.lastIndexOf(' ') + 1));
        } else {
            return null;
        }
    }
}
