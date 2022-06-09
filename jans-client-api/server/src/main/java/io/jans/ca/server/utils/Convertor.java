/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package io.jans.ca.server.utils;

import com.fasterxml.jackson.databind.node.POJONode;
import io.jans.ca.common.Command;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.Jackson2;
import io.jans.ca.common.params.IParams;
import io.jans.ca.server.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenient static convertor.
 *
 * @author Yuriy Zabrovarnyy
 */

public class Convertor {

    private static final Logger LOG = LoggerFactory.getLogger(Convertor.class);

    /**
     * Avoid instance creation
     */
    private Convertor() {
    }

    /**
     * Returns parameter object based on string representation.
     *
     * @param clazz parameter class
     * @param <T>   parameter calss
     * @return parameter object based on string representation
     */
    public static <T extends IParams> T asParams(Class<T> clazz, Command command) {
        if (command.getParams() instanceof POJONode) {
            return (T) ((POJONode)command.getParams()).getPojo();
        }
        final String paramsAsString = command.paramsAsString();
        try {
            T params = Jackson2.createJsonMapper().readValue(paramsAsString, clazz);
            if (params == null) {
                throw new HttpException(ErrorResponseCode.INTERNAL_ERROR_NO_PARAMS);
            }
            return params;
        } catch (HttpException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.error("Unable to parse string to params, string: {}", paramsAsString);
        throw new HttpException(ErrorResponseCode.INTERNAL_ERROR_NO_PARAMS);
    }
}
