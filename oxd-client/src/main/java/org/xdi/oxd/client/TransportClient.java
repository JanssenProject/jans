/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */

package org.xdi.oxd.client;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.common.ReadResult;

import java.io.*;
import java.net.Socket;

/**
 * Transport client, low level client that handles all transport details. Used by Command Client
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 27/07/2013
 * @see CommandClient
 */
public class TransportClient {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(TransportClient.class);

    /**
     * Socket
     */
    private final Socket m_socket;

    /**
     * Out writer
     */
    private final PrintWriter m_out;

    /**
     * In reader
     */
    private final BufferedReader m_in;

    /**
     * Constructor
     *
     * @param p_host host
     * @param p_port port
     * @throws IOException throws if unable to connect by specified host and port
     */
    public TransportClient(String p_host, int p_port) throws IOException {
        m_socket = new Socket(p_host, p_port);
        m_out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(m_socket.getOutputStream(), CoreUtils.UTF8)), true);
        m_in = new BufferedReader(new InputStreamReader(m_socket.getInputStream(), CoreUtils.UTF8));
    }

    /**
     * Sends command
     *
     * @param p_command command as json
     * @return response as json
     * @throws IOException if unable to execute command
     */
    public String sendCommand(String p_command) throws IOException {
        if (StringUtils.isBlank(p_command)) {
            throw new IllegalArgumentException("It's not allowed to send blank/empty command.");
        }
        final String lengthString = CoreUtils.normalizeLengthPrefixString(p_command.length());

        final StringBuilder sb = new StringBuilder(lengthString);
        sb.append(p_command);

        final String toSend = sb.toString();
        LOG.trace("Send: {}", toSend);
        m_out.print(toSend);
        m_out.flush();

        final ReadResult readResult = CoreUtils.readCommand("", m_in);
        LOG.trace("Response: {}", readResult);
        return readResult != null ? readResult.getCommand() : null;
    }

    /**
     * Release resources.
     */
    public void close() {
        IOUtils.closeQuietly(m_out);
        IOUtils.closeQuietly(m_in);
        IOUtils.closeQuietly(m_socket);
    }

    /**
     * Release resources silently.
     *
     * @param p_client client
     */
    public static void closeQuietly(TransportClient p_client) {
        if (p_client != null) {
            p_client.close();
        }
    }
}
