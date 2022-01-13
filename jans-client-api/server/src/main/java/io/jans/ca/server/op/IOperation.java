/*
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package io.jans.ca.server.op;

import io.jans.ca.common.params.IParams;
import io.jans.ca.common.response.IOpResponse;

/**
 * Base interface for oxd operations. Operation parameter must be specified via contructor.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/08/2013
 */

public interface IOperation<T extends IParams> {

    /**
     * Executes operations and produces response.
     *
     * @return command response
     */
    IOpResponse execute(T params) throws Exception;

    Class<T> getParameterClass();
}
