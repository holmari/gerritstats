package com.holmsted.gerrit;

import javax.annotation.Nonnull;

/**
 * Represents a Gerrit server and the command-line tool (which implies the
 * protocol) used to access it. Access via SSH protocol requires a totally
 * different query syntax than HTTPS protocol, but yields similar results.
 */
public abstract class GerritServer {

    public enum Cli {

        SSH(new String[] { "ssh" }), //
        CURL(new String[] { "curl", "-s", "--header", "Accept:application/json" }), //
        WGET(new String[] { "wget", "-q", "--header", "Accept:application/json", "-O-" });

        // How to invoke the command with result sent to stdout
        private String[] commands;

        private Cli(String[] cmds) {
            this.commands = cmds;
        }

        public String[] getCommands() {
            return commands;
        }

    }

    @Nonnull
    private final String serverName;
    private final int port;
    private final Cli cli;

    public abstract int getDefaultPort();

    public abstract Cli getDefaultCli();

    public GerritServer(@Nonnull String serverName, int port, Cli cli) {
        this.serverName = serverName;
        this.port = port > 0 ? port : getDefaultPort();
        this.cli = cli != null ? cli : getDefaultCli();
    }

    public GerritServer(@Nonnull String serverName, int port) {
        this(serverName, port, null);
    }

    public String getServerName() {
        return serverName;
    }

    public int getPort() {
        return port;
    }

    public Cli getCli() {
        return cli;
    }

    @Override
    public String toString() {
        return serverName + ":" + Integer.toString(getPort()) + ", via " + getCli();
    }
}
