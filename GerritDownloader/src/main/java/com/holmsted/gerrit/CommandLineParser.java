package com.holmsted.gerrit;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.holmsted.gerrit.downloaders.ssh.SshDownloader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@SuppressWarnings("unused")
public class CommandLineParser {

    private static final String DEFAULT_OUTPUT_DIR = "out";

    @Parameter(names = {"-s", "--server"},
            description = "Download from Gerrit server name and port, in format server:port. "
                    + "Some servers require a username; e.g., mylogin@gerrit.project.org. "
                    + "If port is omitted, defaults to 29418.",
            arity = 1,
            required = true,
            converter = ServerAndPort.Converter.class)
    private ServerAndPort serverAndPort;

    @Parameter(names = {"-i", "--private-key"},
            description = "The SSH private key to access the server. Defaults to ~/.ssh/id_rsa.",
            required = false)
    private String privateKey;

    @Parameter(names = {"-p", "--project"},
            description = "The Gerrit project from which to retrieve stats. This parameter can appear multiple times. "
                    + "If omitted, stats will be retrieved from all projects.")
    private final List<String> projectNames = new ArrayList<>();

    @Parameter(names = {"-o", "--output-dir"},
            description = "The directory into which the json output will be written into. "
                    + "If multiple projects are specified, each is downloaded into its own file.",
            required = true)
    private String outputDir;

    @Parameter(names = {"-l", "--limit"},
            description = "The number of commits which to retrieve from the server. "
            + "If omitted, stats will be retrieved until no further records are available. "
            + "This value is an approximation; the actual number of downloaded commit data "
            + "will be a multiple of the limit set on the Gerrit server.")
    private int limit = SshDownloader.NO_COMMIT_LIMIT; // NOPMD

    @Parameter(names = {"-a", "--after-date"},
            description = "If specified, commits older than this date won't be downloaded."
            + "Format should be in the form yyyy-mm-dd",
            required = false,
            converter = DateConverter.class)
     private String afterDate;

    @Parameter(names = {"-b", "--before-date"},
            description = "If specified, commits younger than this date won't be downloaded."
            + "Format should be in the form yyyy-mm-dd",
            required = false,
            converter = DateConverter.class)
     private String beforeDate;

    @Nonnull
    private final JCommander jCommander = new JCommander(this);

    public static class ServerAndPort {
        private String serverName;
        private int serverPort;

        public static class Converter implements IStringConverter<ServerAndPort> {
            @Override
            public ServerAndPort convert(String value) {
                ServerAndPort result = new ServerAndPort();

                int protocolSeparator = value.indexOf("://");
                int portSeparator = value.lastIndexOf(':');
                int serverNameEnd = value.length();
                if (portSeparator != -1 && portSeparator != protocolSeparator) {
                    result.serverPort = Integer.parseInt(value.substring(portSeparator + 1));
                    serverNameEnd = portSeparator;
                }

                int serverNameStart = protocolSeparator != -1 ? protocolSeparator + 3 : 0;
                result.serverName = value.substring(serverNameStart, serverNameEnd);
                if (result.serverName.endsWith("/")) {
                    result.serverName = result.serverName.substring(0, result.serverName.length() - 1);
                }

                return result;
            }
        }
    }

    public static class DateConverter implements IStringConverter<String> {
        @Override
        public String convert(String value) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                dateFormat.setLenient(false);
                dateFormat.parse(value);
                return value;
            } catch (ParseException e) {
                throw new ParameterException("Bad Date format " + value);
            }
        }
    }

    public CommandLineParser() {
        ClassLoader loader = getClass().getClassLoader();
        URL url = loader.getResource("META-INF/MANIFEST.MF");
        try {
            Manifest manifest = new Manifest(url.openStream());
            Attributes attr = manifest.getMainAttributes();
            String mainClass = attr.getValue(Attributes.Name.MAIN_CLASS);
            jCommander.setProgramName(mainClass);
        } catch (IOException e) {
            // Ignore.
        }
    }

    public boolean parse(String[] args) {
        try {
            jCommander.parse(args);
        } catch (ParameterException e) {
            return false;
        }

        outputDir = resolveOutputDir(outputDir);

        return getServerName() != null;
    }

    @Nonnull
    private static String resolveOutputDir(@Nonnull String path) {
        String resolvedDir = path;
        if (path.startsWith("~" + File.separator)) {
            resolvedDir = System.getProperty("user.home") + path.substring(1);
        }
        return resolvedDir;
    }

    @Nullable
    public String getOutputDir() {
        return outputDir;
    }

    @Nullable
    public String getServerName() {
        return serverAndPort != null ? serverAndPort.serverName : null;
    }

    @Nullable
    public String getPrivateKey() {
        if (privateKey != null) {
            if (privateKey.startsWith("~" + File.separator)) {
                privateKey = System.getProperty("user.home") + privateKey.substring(1);
            }
            return privateKey;
        }
        return System.getProperty("user.home") + "/.ssh/id_rsa";
    }

    @Nullable
    public List<String> getProjectNames() {
        return projectNames;
    }

    public int getServerPort() {
        return serverAndPort != null ? serverAndPort.serverPort : 0;
    }

    public int getCommitLimit() {
        return limit;
    }

    public String getAfterDate() {
        return afterDate;
    }

    public String getBeforeDate() {
        return beforeDate;
    }

    public void printUsage() {
        jCommander.usage();
        System.out.println("Options preceded by an asterisk are required.");
    }
}
