/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common.service.common.api;

import io.jans.as.model.common.IdType;

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
    String generateId(String p_idType, String p_idPrefix);

    String generateId(IdType p_idType, String p_idPrefix);
}
