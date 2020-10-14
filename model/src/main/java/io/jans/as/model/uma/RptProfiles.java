/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.uma;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/12/2015
 */

public enum RptProfiles {

    BEARER("https://docs.kantarainitiative.org/uma/profiles/uma-token-bearer-1.0");

    private String identifyingUri;

    private RptProfiles(String identifyingUri) {
        this.identifyingUri = identifyingUri;
    }

    public String getIdentifyingUri() {
        return identifyingUri;
    }
}
