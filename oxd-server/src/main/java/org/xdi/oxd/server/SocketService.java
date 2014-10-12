package org.xdi.oxd.server;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.CoreUtils;
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
     * Singleton
     */
    private static final SocketService INSTANCE = new SocketService();

    /**
     * Server socket
     */
    private volatile ServerSocket m_serverSocket = null;

    /**
     * Shutdown flag
     */
    private volatile boolean m_shutdown = false;

    /**
     * Avoid direct instance creation
     */
    private SocketService() {
    }

    public static SocketService getInstance() {
        return INSTANCE;
    }

    public void listenSocket() {
        final Configuration c = Configuration.getInstance();
        final int port = c.getPort();

        final LicenseService licenseService = new LicenseService(c);
        final ExecutorService executorService = Executors.newFixedThreadPool(licenseService.getThreadsCount(), CoreUtils.daemonThreadFactory());

        try {
            final Boolean localhostOnly = c.getLocalhostOnly();
            if (localhostOnly == null || localhostOnly) {
                final InetAddress address = InetAddress.getByName("127.0.0.1");
                m_serverSocket = new ServerSocket(port, 50, address);
            } else {
                m_serverSocket = new ServerSocket(port, 50);
            }

            m_serverSocket.setSoTimeout(c.getTimeOutInSeconds() * 1000);
            LOG.info("Server socket is bound to port: {}, with timeout: {} seconds. Start listening for notifications.", port, c.getTimeOutInSeconds());
            while (!m_shutdown) {
                try {
                    final Socket clientSocket = m_serverSocket.accept();
                    LOG.debug("Start new SocketProcessor...");
                    executorService.execute(new SocketProcessor(clientSocket, licenseService));
                } catch (IOException e) {
                    LOG.error("Accept failed, port: {}", port);
                    throw e;
                    //System.exit(-1);
                }
            }
        } catch (IOException e) {
            LOG.error("Could not listen on port: {}.", port);
        } finally {
            IOUtils.closeQuietly(m_serverSocket);
        }
    }

    public void setShutdown(boolean p_shutdown) {
        m_shutdown = p_shutdown;
    }

    public void shutdownNow() {
        LOG.info("Shutdown server...");
        try {
            m_shutdown = true;
            IOUtils.closeQuietly(m_serverSocket);
        } finally {
            LOG.info("Shutdown finished.");
        }
    }
}
