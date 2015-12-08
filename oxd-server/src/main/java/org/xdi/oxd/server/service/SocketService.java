/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.service;

import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.server.Configuration;
import org.xdi.oxd.server.SocketProcessor;
import org.xdi.oxd.server.license.LicenseService;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Socket service.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 27/07/2013
 */
public class SocketService {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(SocketService.class);

    /**
     * Server socket
     */
    private volatile ServerSocket serverSocket = null;

    /**
     * Shutdown flag
     */
    private volatile boolean shutdown = false;

    private Configuration conf;

    /**
     * Avoid direct instance creation
     */
    @Inject
    public SocketService(Configuration conf) {
        this.conf = conf;
    }

    public void listenSocket() throws IOException {
        final int port = conf.getPort();

        final LicenseService licenseService = new LicenseService(conf);
        final ExecutorService executorService = Executors.newFixedThreadPool(licenseService.getThreadsCount(), CoreUtils.daemonThreadFactory());

        try {
            final Boolean localhostOnly = conf.getLocalhostOnly();
            if (localhostOnly == null || localhostOnly) {
                final InetAddress address = InetAddress.getByName("127.0.0.1");
                serverSocket = new ServerSocket(port, 50, address);
            } else {
                serverSocket = new ServerSocket(port, 50);
            }

            serverSocket.setSoTimeout(conf.getTimeOutInSeconds() * 1000);
            LOG.info("Server socket is bound to port: {}, with timeout: {} seconds. Start listening for notifications.", port, conf.getTimeOutInSeconds());
            while (!shutdown) {
                try {
                    if (licenseService.isLicenseChanged()) {
                        licenseService.reset();
                        LOG.info("License was changed. Restart oxd server to enforce new license!");
                        shutdownNow();
                        shutdown = false;
                        LOG.info("Starting...");
                        listenSocket();
                    }

                    final Socket clientSocket = serverSocket.accept();
                    LOG.debug("Start new SocketProcessor...");
                    executorService.execute(new SocketProcessor(clientSocket));
                } catch (IOException e) {
                    LOG.error("Accept failed, port: {}", port);
                    throw e;
                    //System.exit(-1);
                }
            }
        } catch (IOException e) {
            LOG.error("Could not listen on port: {}.", port);
            throw e;
        } finally {
            IOUtils.closeQuietly(serverSocket);
        }
    }

    public void shutdownNow() {
        LOG.info("Shutdown server...");
        try {
            shutdown = true;
            IOUtils.closeQuietly(serverSocket);
        } finally {
            LOG.info("Shutdown finished.");
        }
    }
}
