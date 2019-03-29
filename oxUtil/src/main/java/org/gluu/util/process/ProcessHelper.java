/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.util.process;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.log4j.Logger;
import org.gluu.util.StringHelper;

/**
 * Utility to execute external processes
 *
 * @author Yuriy Movchan Date: 11.22.2010
 */
public final class ProcessHelper {

    private static Logger LOG = Logger.getLogger(ProcessHelper.class);

    private static final long PRINT_JOB_TIMEOUT = 100 * 1000;

    private ProcessHelper() {
    }

    public static boolean executeProgram(String programPath, boolean executeInBackground, int successExitValue,
            OutputStream outputStream) {
        return executeProgram(programPath, null, executeInBackground, successExitValue, outputStream);
    }

    public static boolean executeProgram(String programPath, String workingDirectory, boolean executeInBackground,
            int successExitValue, OutputStream outputStream) {
        CommandLine commandLine = new CommandLine(programPath);
        return executeProgram(commandLine, workingDirectory, executeInBackground, successExitValue, outputStream);
    }

    public static boolean executeProgram(CommandLine commandLine, boolean executeInBackground, int successExitValue,
            OutputStream outputStream) {
        return executeProgram(commandLine, null, executeInBackground, successExitValue, outputStream);
    }

    public static boolean executeProgram(CommandLine commandLine, String workingDirectory, boolean executeInBackground,
            int successExitValue, OutputStream outputStream) {
        long printJobTimeout = PRINT_JOB_TIMEOUT;

        ExecuteStreamHandler streamHandler = null;
        if (outputStream != null) {
            streamHandler = new PumpStreamHandler(outputStream);
        }

        PrintResultHandler printResult = null;
        try {
            LOG.debug(String.format("Preparing to start process %s", commandLine.toString()));
            printResult = executeProgram(commandLine, workingDirectory, printJobTimeout, executeInBackground,
                    successExitValue, streamHandler);
            LOG.debug(String.format("Successfully start process %s", commandLine.toString()));
        } catch (Exception ex) {
            LOG.trace(String.format("Problem during starting process %s", commandLine.toString()), ex);
            ex.printStackTrace();
            return false;
        }

        // come back to check the print result
        LOG.debug(String.format("Waiting for the proces %s finish", commandLine.toString()));
        try {
            if (printResult == null) {
                return false;
            }
            printResult.waitFor();
        } catch (InterruptedException ex) {
            LOG.error(String.format("Problem during process execution %s", commandLine.toString()), ex);
        }

        LOG.debug(String.format("Process %s has finished", commandLine.toString()));

        return true;
    }

    /**
     *
     * @param printJobTimeout
     *            the printJobTimeout (ms) before the watchdog terminates the print
     *            process
     * @param printInBackground
     *            printing done in the background or blocking
     * @param streamHandler
     * @return a print result handler (implementing a future)
     * @throws IOException
     *             the test failed
     */
    public static PrintResultHandler executeProgram(CommandLine commandLine, long printJobTimeout,
            boolean printInBackground, int successExitValue, ExecuteStreamHandler streamHandler) throws IOException {
        return executeProgram(commandLine, null, printJobTimeout, printInBackground, successExitValue, streamHandler);
    }

    /**
     *
     * @param printJobTimeout
     *            the printJobTimeout (ms) before the watchdog terminates the print
     *            process
     * @param printInBackground
     *            printing done in the background or blocking
     * @param streamHandler
     * @return a print result handler (implementing a future)
     * @throws IOException
     *             the test failed
     */
    public static PrintResultHandler executeProgram(CommandLine commandLine, String workingDirectory,
            long printJobTimeout, boolean printInBackground, int successExitValue, ExecuteStreamHandler streamHandler)
            throws IOException {
        ExecuteWatchdog watchdog = null;
        PrintResultHandler resultHandler;

        // Create the executor and consider the successExitValue as success
        Executor executor = new DefaultExecutor();
        executor.setExitValue(successExitValue);

        if (StringHelper.isNotEmpty(workingDirectory)) {
            executor.setWorkingDirectory(new File(workingDirectory));
        }

        // Redirect streams if needed
        if (streamHandler != null) {
            executor.setStreamHandler(streamHandler);
        }

        // Create a watchdog if requested
        if (printJobTimeout > 0) {
            watchdog = new ExecuteWatchdog(printJobTimeout);
            executor.setWatchdog(watchdog);
        }

        // Pass a "ExecuteResultHandler" when doing background printing
        if (printInBackground) {
            LOG.debug(String.format("Executing non-blocking process %s", commandLine.toString()));
            resultHandler = new PrintResultHandler(watchdog);
            executor.execute(commandLine, resultHandler);
        } else {
            LOG.debug(String.format("Executing blocking process %s", commandLine.toString()));
            successExitValue = executor.execute(commandLine);
            resultHandler = new PrintResultHandler(successExitValue);
        }

        return resultHandler;
    }

    private static class PrintResultHandler extends DefaultExecuteResultHandler {

        private ExecuteWatchdog watchdog;

        PrintResultHandler(ExecuteWatchdog watchdog) {
            this.watchdog = watchdog;
        }

        PrintResultHandler(int exitValue) {
            super.onProcessComplete(exitValue);
        }

        public void onProcessComplete(int exitValue) {
            super.onProcessComplete(exitValue);
            LOG.debug("The process successfully executed");
        }

        public void onProcessFailed(ExecuteException ex) {
            super.onProcessFailed(ex);
            if ((watchdog != null) && watchdog.killedProcess()) {
                LOG.debug("The process timed out");
            } else {
                LOG.debug("The process failed to do", ex);
            }
        }
    }

}
