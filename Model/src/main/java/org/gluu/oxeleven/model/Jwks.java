/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.model;

import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version April 12, 2016
 */
public class Jwks {

    private List<Key> keys;

    public List<Key> getKeys() {
        return keys;
    }

    public void setKeys(List<Key> keys) {
        this.keys = keys;
    }
}
