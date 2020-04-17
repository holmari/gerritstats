package com.holmsted.gerrit.downloaders.ssh;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.holmsted.gerrit.downloaders.CommandRunner;

public class GerritSshCommand extends CommandRunner {
    @Nonnull
    private final GerritSshServer gerritServer;

    public GerritSshCommand(@Nonnull GerritSshServer gerritServer) {
        this.gerritServer = gerritServer;
    }

    public String exec(@Nonnull String... gerritCommands) {
        List<String> l = new ArrayList<>();
        l.add("ssh");
        l.add("-p");
        l.add(String.valueOf(gerritServer.getPort()));
        if (gerritServer.getPrivateKey() != null) {
            l.add("-i");
            l.add(gerritServer.getPrivateKey());
        }
        l.add(gerritServer.getServerName());
        l.add("gerrit");
        for (String c: gerritCommands)
            l.add(c);
        String [] commandArray = l.toArray(new String[] {});
        System.out.println(commandArrayToString(commandArray));
        return runCommand(commandArray);
    }
}
