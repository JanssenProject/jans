/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.client;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.common.params.LicenseStatusParams;
import org.xdi.oxd.common.response.LicenseStatusOpResponse;

import java.io.IOException;

/**
 * Command client. Used to execute oxd operations.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/08/2013
 */

public class CommandClient {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(CoreUtils.class);

    /**
     * Transport client
     */
    private TransportClient m_client;

    /**
     * Constructor.
     *
     * @param host host
     * @param port port
     * @throws IOException throws if unable to connect by specified host and port
     */
    public CommandClient(String host, int port) throws IOException {
        m_client = new TransportClient(host, port);
    }

    /**
     * Executes command.
     *
     * @param p_command command to execute
     * @return command response
     */
    public CommandResponse send(Command p_command) {
        if (p_command == null) {
            throw new IllegalArgumentException("Command is null");
        }
        try {
            final String commandAsJson = CoreUtils.asJson(p_command);
            final String responseAsJson = m_client.sendCommand(commandAsJson);
            if (StringUtils.isNotBlank(responseAsJson)) {
                return CoreUtils.createJsonMapper().readValue(responseAsJson, CommandResponse.class);
            } else {
                LOG.error("Server doesn't send any response.");
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Release resources used by client.
     */
    public void close() {
        if (m_client != null) {
            m_client.close();
        }
    }

    /**
     * Release resources quietly.
     *
     * @param p_client client
     */
    public static void closeQuietly(CommandClient p_client) {
        if (p_client != null) {
            p_client.close();
        }
    }

    public LicenseStatusOpResponse licenseStatus() {
        final Command command = new Command(CommandType.LICENSE_STATUS);
        command.setParamsObject(new LicenseStatusParams());
        final CommandResponse response = send(command);
        return response.dataAsResponse(LicenseStatusOpResponse.class);
    }

}
