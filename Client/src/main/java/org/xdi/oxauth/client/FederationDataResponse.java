package org.xdi.oxauth.client;

import org.xdi.oxauth.model.federation.FederationErrorResponseType;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 08/10/2012
 */

public class FederationDataResponse extends BaseResponseWithErrors<FederationErrorResponseType> {

    @Override
    public FederationErrorResponseType fromString(String p_str) {
        return FederationErrorResponseType.fromString(p_str);
    }
}
