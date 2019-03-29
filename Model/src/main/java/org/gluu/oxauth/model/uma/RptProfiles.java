package org.gluu.oxauth.model.uma;

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
