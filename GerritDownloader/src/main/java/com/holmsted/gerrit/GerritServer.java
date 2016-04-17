package com.holmsted.gerrit;

import javax.annotation.Nonnull;

/**
 * Represents a Gerrit server.
 */
public class GerritServer {

    private static final int GERRIT_DEFAULT_PORT = 29418;

    private final int port;

    @Nonnull
    private final String serverName;

    public GerritServer(@Nonnull String serverName, int port) {
        this.serverName = serverName;
        this.port = port != 0 ? port : GERRIT_DEFAULT_PORT;
    }

    public String getServerName() {
        return serverName;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return String.format("%s:%d", serverName, port);
    }
}
