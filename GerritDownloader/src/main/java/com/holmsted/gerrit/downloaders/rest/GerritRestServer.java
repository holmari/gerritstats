package com.holmsted.gerrit.downloaders.rest;

import javax.annotation.Nonnull;

import com.holmsted.gerrit.GerritServer;

/**
 * Represents a Gerrit server accessed via a command-line tool to an HTTPS
 * endpoint that exposes Gerrit REST API.
 */
public class GerritRestServer extends GerritServer {

    public GerritRestServer(@Nonnull String serverName, int port, Cli cli) {
        super(serverName, port, cli);
    }

    public GerritRestServer(@Nonnull String serverName) {
        super(serverName, 0, null);
    }

    @Override
    public int getDefaultPort() {
        return 443;
    }

    @Override
    public Cli getDefaultCli() {
        return Cli.CURL;
    }

    @Override
    public String toString() {
        return String.format("%s:%d", getServerName(), getPort());
    }

}
