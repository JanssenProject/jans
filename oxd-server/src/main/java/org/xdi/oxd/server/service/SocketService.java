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
import org.xdi.oxd.server.ShutdownException;
import org.xdi.oxd.server.SocketProcessor;
import org.xdi.oxd.server.license.LicenseFileUpdateService;
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
    private HttpService httpService;
    private TimeService timeService;

    /**
     * Avoid direct instance creation
     */
    @Inject
    public SocketService(Configuration conf, HttpService httpService, TimeService timeService) {
        this.conf = conf;
        this.httpService = httpService;
        this.timeService = timeService;
    }

    public void listenSocket() throws IOException {
        final int port = conf.getPort();

        final LicenseService licenseService = new LicenseService(conf, httpService, timeService);
        licenseService.start();

        try {
            final Boolean localhostOnly = conf.getLocalhostOnly();
            if (localhostOnly == null || localhostOnly) {
                final InetAddress address = InetAddress.getByName("127.0.0.1");
                serverSocket = new ServerSocket(port, 50, address);
            } else {
                serverSocket = new ServerSocket(port, 50);
            }

            serverSocket.setSoTimeout(conf.getTimeOutInSeconds() * 1000);
            if (conf.getTimeOutInSeconds() > 0) {
                LOG.info("time_out_in_seconds of socket is not zero, server automatically shutdown socket after this timeout.");
            }

            LOG.info("Server socket is bound to port: {}, with timeout: {} seconds. Start listening for notifications.", port, conf.getTimeOutInSeconds());
            while (!shutdown) {
                try {
                    final Socket clientSocket = serverSocket.accept();

                    if (!licenseService.isLicenseValid()) {
                        LOG.error("License is invalid. Please check your license_id and make sure it is not expired.");
                        LOG.error("Unable to fetch valid license after " + LicenseFileUpdateService.RETRY_LIMIT +
                                " re-tries. Shutdown the server.");
                        throw new ShutdownException("Unable to fetch valid license after " + LicenseFileUpdateService.RETRY_LIMIT +
                                " re-tries. Shutdown the server.");
                    }

                    executorService().execute(new SocketProcessor(clientSocket));
                } catch (IOException e) {
                    LOG.error("Accept failed, port: {}", port);
                    throw e;
                }
            }
        } catch (IOException e) {
            LOG.error("Could not listen on port: {}.", port);
            throw e;
        } finally {
            IOUtils.closeQuietly(serverSocket);
        }
    }

    private ExecutorService executorService() {
        return Executors.newSingleThreadExecutor(CoreUtils.daemonThreadFactory());
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
