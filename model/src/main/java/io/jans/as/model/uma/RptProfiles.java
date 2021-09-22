/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
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

    private final String identifyingUri;

    RptProfiles(String identifyingUri) {
        this.identifyingUri = identifyingUri;
    }

    public String getIdentifyingUri() {
        return identifyingUri;
    }
}
