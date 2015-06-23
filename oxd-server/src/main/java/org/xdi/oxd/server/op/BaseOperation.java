package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.codehaus.jackson.node.POJONode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.common.response.IOpResponse;

/**
 * Base abstract class for all operations.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/08/2013
 */

public abstract class BaseOperation implements IOperation {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(BaseOperation.class);

    /**
     * Command
     */
    private final Command m_command;

    private final Injector injector;

    /**
     * Base constructor
     *
     * @param p_command command
     */
    protected BaseOperation(Command p_command, final Injector injector) {
        this.injector = injector;
        m_command = p_command;
    }

    /**
     * Gets injector.
     *
     * @return injector
     */
    public Injector getInjector() {
        return injector;
    }

    /**
     * Returns command
     *
     * @return command
     */
    public Command getCommand() {
        return m_command;
    }

    /**
     * Returns parameter object based on string representation.
     *
     * @param p_class parameter class
     * @param <T>     parameter calss
     * @return parameter object based on string representation
     */
    public <T> T asParams(Class<T> p_class) {
        final String paramsAsString = m_command.paramsAsString();
        try {
            return CoreUtils.createJsonMapper().readValue(paramsAsString, p_class);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.error("Unable to parse string to params, string: {}", paramsAsString);
        return null;
    }

    /**
     * Ok response for operation
     *
     * @param p_data response
     * @return ok response with data
     */
    public CommandResponse okResponse(IOpResponse p_data) {
        if (p_data == null) {
            return CommandResponse.createInternalError();
        }
        return CommandResponse.ok().setData(new POJONode(p_data));
    }
}
