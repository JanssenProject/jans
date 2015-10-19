package org.xdi.oxd.rp.client;

import org.xdi.oxd.common.params.RegisterSiteParams;
import org.xdi.oxd.common.response.CheckIdTokenResponse;
import org.xdi.oxd.common.response.RegisterSiteResponse;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/10/2015
 */

public interface RpClient {

    RpClient register(String authorizationUrl);

    RpClient register(RegisterSiteParams params);

    RegisterSiteResponse getRegistrationDetails();

    String getOxdId();

    void close();

    String getAuthorizationUrl();

    CheckIdTokenResponse validateIdToken(String idToken);
}
