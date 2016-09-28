package com.holmsted.gerrit.downloaders.ssh;

import com.holmsted.gerrit.GerritServer;
import com.holmsted.gerrit.GerritVersion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

public final class GerritSsh {

    public static List<String> listProjects(@Nonnull GerritServer gerritServer) {
        GerritSshCommand sshCommand = new GerritSshCommand(gerritServer);
        String output = sshCommand.exec("ls-projects");

        List<String> projectList = new ArrayList<>();
        Collections.addAll(projectList, output.split("\n"));
        return projectList;
    }

    public static GerritVersion version(@Nonnull GerritServer gerritServer) {
        GerritSshCommand sshCommand = new GerritSshCommand(gerritServer);
        String output = sshCommand.exec("version");
        if (output != null) {
            return GerritVersion.fromString(output.substring(output.lastIndexOf(' ') + 1));
        } else {
            return null;
        }
    }

    private GerritSsh() {
    }
}
