/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.uma;

import java.io.IOException;
import java.util.List;

import io.jans.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yuriyz on 06/16/2017.
 */
public final class ClaimDefinitionBuilder {

	private static final Logger LOG = LoggerFactory.getLogger(ClaimDefinitionBuilder.class);

    private ClaimDefinitionBuilder() {
    }

    /**
     * Parse json. Sample: [ { "issuer" : [ "https://example.com" ], "name" :
     * "country", "claim_token_format" : [
     * "http://openid.net/specs/openid-connect-core-1_0.html#IDToken" ],
     * "claim_type" : "string", "friendly_name" : "country" } ]
     *
     * @param json
     * @return
     */
    public static List<ClaimDefinition> build(String json) {
        try {
            return Util.createJsonMapper().readValue(json, ClaimDefinitionList.class);
        } catch (IOException e) {
            LOG.error("Failed to parse claim definition json: " + json, e);
            throw new RuntimeException("Failed to parse claim definition json: " + json, e);
        }
    }
}
