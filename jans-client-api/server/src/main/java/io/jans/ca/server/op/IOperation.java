/*
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package io.jans.ca.server.op;

import io.jans.ca.common.params.IParams;
import io.jans.ca.common.response.IOpResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface IOperation<T extends IParams> {

    /**
     * Executes operations and produces response.
     *
     * @return command response
     */
    IOpResponse execute(T params, HttpServletRequest httpRequest) throws Exception;

    Class<T> getParameterClass();

    String getReturnType();
}
