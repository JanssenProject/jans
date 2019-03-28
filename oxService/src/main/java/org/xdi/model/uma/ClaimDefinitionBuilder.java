package org.xdi.model.uma;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.gluu.model.uma.ClaimDefinition;
import org.gluu.model.uma.ClaimDefinitionList;
import org.xdi.util.Util;

/**
 * @author yuriyz on 06/16/2017.
 */
public final class ClaimDefinitionBuilder {

    private static final Logger LOG = Logger.getLogger(ClaimDefinitionBuilder.class);

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
