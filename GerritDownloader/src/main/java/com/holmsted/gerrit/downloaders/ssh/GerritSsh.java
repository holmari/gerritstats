package com.holmsted.gerrit.downloaders.ssh;

import com.holmsted.gerrit.GerritServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

public class GerritSsh {

    public static List<String> listProjects(@Nonnull GerritServer gerritServer) {
        GerritSshCommand sshCommand = new GerritSshCommand(gerritServer);
        String output = sshCommand.exec("ls-projects");

        List<String> projectList = new ArrayList<>();
        Collections.addAll(projectList, output.split("\n"));
        return projectList;
    }
}
