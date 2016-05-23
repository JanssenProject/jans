/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.op;

import org.xdi.oxd.common.CommandResponse;

/**
 * Base interface for oxd operations. Operation parameter must be specified via contructor.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/08/2013
 */

public interface IOperation {

    /**
     * Executes operations and produces response.
     *
     * @return command response
     */
    public CommandResponse execute() throws Exception;
}
