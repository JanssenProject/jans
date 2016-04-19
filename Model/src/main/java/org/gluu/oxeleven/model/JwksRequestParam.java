/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.model;

import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version April 18, 2016
 */
public class JwksRequestParam {

    private List<KeyRequestParam> keyRequestParams;

    public List<KeyRequestParam> getKeyRequestParams() {
        return keyRequestParams;
    }

    public void setKeyRequestParams(List<KeyRequestParam> keyRequestParams) {
        this.keyRequestParams = keyRequestParams;
    }
}
