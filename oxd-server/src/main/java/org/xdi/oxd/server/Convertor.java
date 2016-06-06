/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.common.ErrorResponseCode;
import org.xdi.oxd.common.ErrorResponseException;
import org.xdi.oxd.common.params.IParams;

/**
 * Convenient static convertor.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 13/08/2013
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
     * @param <T>     parameter calss
     * @return parameter object based on string representation
     */
    public static <T extends IParams> T asParams(Class<T> clazz, Command command) {
        final String paramsAsString = command.paramsAsString();
        try {
            T params = CoreUtils.createJsonMapper().readValue(paramsAsString, clazz);
            if (params == null) {
                throw new ErrorResponseException(ErrorResponseCode.INTERNAL_ERROR_NO_PARAMS);
            }
            LOG.trace("Params: {}", params);
            return params;
        } catch (ErrorResponseException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.error("Unable to parse string to params, string: {}", paramsAsString);
        throw new ErrorResponseException(ErrorResponseCode.INTERNAL_ERROR_NO_PARAMS);
    }
}
