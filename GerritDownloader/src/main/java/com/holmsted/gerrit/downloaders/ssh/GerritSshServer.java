package com.holmsted.gerrit.downloaders.ssh;

import javax.annotation.Nonnull;

import com.holmsted.gerrit.GerritServer;

/**
 * Represents a Gerrit server accessed via command-line tool ssh, which uses the
 * script-friendly Gerrit query API.
 */
public class GerritSshServer extends GerritServer {

    @Nonnull
    private final String privateKey;

    public GerritSshServer(@Nonnull String serverName, int port, @Nonnull String privateKey) {
        super(serverName, port);
        this.privateKey = privateKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    @Override
    public int getDefaultPort() {
        return 29418;
    }

    @Override
    public Cli getDefaultCli() {
        return Cli.SSH;
    }

    @Override
    public String toString() {
        return String.format("%s:%d", getServerName(), getPort());
    }
}
