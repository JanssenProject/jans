package org.xdi.oxauth.service.uma.authorization;

import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.util.ServerUtil;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/04/2015
 */

public class NeedInfoResponseBuilder {

    private final static Logger log = LoggerFactory.getLogger(NeedInfoResponseBuilder.class);

    private NeedInfoResponseBuilder() {
    }

    public static String entityForResponse(NeedInfoAuthenticationContext authenticationContext,
                                           NeedInfoRequestingPartyClaims requestingPartyClaims) {

        JSONObject result = new JSONObject();
        JSONObject errorDetails = new JSONObject();

        try {
            result.put("error", "need_info");
            result.put("error_details", errorDetails);

            if (authenticationContext != null) {
                errorDetails.put("authentication_context", new JSONObject(ServerUtil.asJson(authenticationContext)));
            }
            if (requestingPartyClaims != null) {
                errorDetails.put("requesting_party_claims", new JSONObject(ServerUtil.asJson(requestingPartyClaims)));
            }
        } catch (Exception ex) {
            log.error("Failed to generate 'need_info' json response", ex);
            return null;
        }

        return result.toString();
    }

}
