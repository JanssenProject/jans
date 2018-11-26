/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.api;

import org.xdi.oxauth.model.common.IdType;

/**
 * Id generator interface. Base interface for id generation. It must be implemented by
 * python class in case of custom id generation.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 24/06/2013
 */

public interface IdGenerator {

    /**
     * Generates id.
     *
     * @param p_idType   id type : use to specify entity type, i.e. person, client
     * @param p_idPrefix id prefix : If you want to prefix all ids, use this, otherwise pass ""
     * @return generated id as string
     */
    public String generateId(String p_idType, String p_idPrefix);

    public String generateId(IdType p_idType, String p_idPrefix);
}
