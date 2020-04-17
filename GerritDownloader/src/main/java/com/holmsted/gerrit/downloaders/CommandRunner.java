package com.holmsted.gerrit.downloaders;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public abstract class CommandRunner {

    // https://stackoverflow.com/questions/64000/draining-standard-error-in-java
    private static class InputHandler extends Thread {

        private final InputStream is;

        private final ByteArrayOutputStream os;

        public InputHandler(InputStream input) {
            this.is = input;
            this.os = new ByteArrayOutputStream();
        }

        public void run() {
            try {
                byte[] buf = new byte[16384];
                int c;
                while ((c = is.read(buf)) > 0)
                    os.write(buf, 0, c);
            } catch (Throwable t) {
                throw new IllegalStateException(t);
            }
        }

        public String getOutput() {
            try {
                os.flush();
            } catch (Throwable t) {
                throw new IllegalStateException(t);
            }
            return os.toString();
        }

    }

    public String commandArrayToString(String [] arr) {
        StringBuilder bldr = new StringBuilder();
        for (String a : arr) {
            bldr.append(a);
            bldr.append(' ');
        }
        return bldr.toString();
    }

    /**
     * Runs the specified system command, collects stdout and returns it. If the
     * command exit code is not zero, prints errors to stderr and returns null.
     * 
     * @param command Array with command at position zero plus arguments; for
     *                example, [ "ls", "/tmp" ]
     * @return Contents of stdout on success, null on failure
     */
    public String runCommand(String... command) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            Process p = builder.start();
            InputHandler outHandler = new InputHandler(p.getInputStream());
            outHandler.start();
            InputHandler errHandler = new InputHandler(p.getErrorStream());
            errHandler.start();
            int rc = p.waitFor();
            // Wait for output to drain
            outHandler.join();
            errHandler.join();
            p.getInputStream().close();
            p.getErrorStream().close();
            p.getOutputStream().close();
            if (rc != 0) {
                System.err.println("Process exited with return code " + rc + " and output:");
                System.err.println(errHandler.getOutput());
                return null;
            }
            return outHandler.getOutput();
        } catch (Exception ex) {
            System.err.println("Failed to execute command: " + command);
            ex.printStackTrace();
            return null;
        }
    }
}
