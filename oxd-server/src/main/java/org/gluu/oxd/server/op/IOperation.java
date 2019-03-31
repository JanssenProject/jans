/*
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.gluu.oxd.server.op;

import org.gluu.oxd.common.params.IParams;
import org.gluu.oxd.common.response.IOpResponse;

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
