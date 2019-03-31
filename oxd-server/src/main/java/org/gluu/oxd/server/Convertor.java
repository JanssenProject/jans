/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.gluu.oxd.server;

import org.codehaus.jackson.node.POJONode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.CoreUtils;
import org.gluu.oxd.common.ErrorResponseCode;
import org.gluu.oxd.common.params.IParams;

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
            T params = CoreUtils.createJsonMapper().readValue(paramsAsString, clazz);
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
