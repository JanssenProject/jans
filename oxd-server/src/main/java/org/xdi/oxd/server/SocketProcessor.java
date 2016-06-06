/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.common.ReadResult;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Socket processor.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 27/07/2013
 */
public class SocketProcessor implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(SocketProcessor.class);

    private final Socket socket;
    private final Processor processor;

    public SocketProcessor(Socket socket) {
        this.socket = socket;
        this.processor = ServerLauncher.getInjector().getInstance(Processor.class);
    }

    @Override
    public void run() {
        PrintWriter out = null;
        BufferedReader in = null;
        try {
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), CoreUtils.UTF8)), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), CoreUtils.UTF8));

            String leftString = "";
            while (true) {
                LOG.trace("Socket processor handling...");

                final ReadResult readResult = CoreUtils.readCommand(leftString, in);

                if (readResult == null || StringUtils.isBlank(readResult.getCommand())) {
                    LOG.trace("Quit. Read result is null or command string is blank.");
                    break;
                }

                leftString = readResult.getLeftString();

                final String response = processor.process(readResult.getCommand());
                if (StringUtils.isBlank(response)) {
                    break;
                }

                writeResponse(out, response);

                if (response.equals(CommandResponse.INTERNAL_ERROR_RESPONSE_AS_STRING)) {
                    LOG.error("Quit. Enable to process command.");
                    break;
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(socket);
        }
    }

    private static void writeResponse(PrintWriter out, String p_string) {
        if (StringUtils.isNotBlank(p_string)) {
            final int length = p_string.length();
            final String normalizedLengthString = CoreUtils.normalizeLengthPrefixString(length);

            final StringBuilder sb = new StringBuilder(normalizedLengthString);
            sb.append(p_string);
            out.print(sb.toString());
            out.flush();
        } else {
            LOG.trace("Skip response write. It's not allowed to write blank/empty string.");
        }
    }
}
