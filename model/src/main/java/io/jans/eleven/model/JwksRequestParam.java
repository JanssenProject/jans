/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.model;

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
