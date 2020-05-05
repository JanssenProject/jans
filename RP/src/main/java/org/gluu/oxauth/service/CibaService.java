package org.gluu.oxauth.service;

import org.gluu.oxauth.client.TokenClient;
import org.gluu.oxauth.client.TokenRequest;
import org.gluu.oxauth.client.TokenResponse;
import org.gluu.oxauth.model.ciba.CibaFlowState;
import org.gluu.oxauth.model.ciba.CibaRequestSession;
import org.gluu.oxauth.model.ciba.PingCibaCallback;
import org.gluu.oxauth.model.common.GrantType;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class CibaService {

    @Inject
    private Logger log;

    public void processPingCallback(String authReqId, PingCibaCallback callback, CibaRequestSession session) {
        log.info("Processing ping callback: {}, session: {}", callback, session);

        TokenResponse tokenResponse = getToken(authReqId, session);
        if ( tokenResponse.getStatus() == 200 ) {
            session.setState(CibaFlowState.ACCEPTED);
        } else {
            session.setState(CibaFlowState.REJECTED);
        }
        session.setTokenResponse(tokenResponse);
    }

    public TokenResponse getToken(String authReqId, CibaRequestSession session) {
        try {
            TokenRequest tokenRequest = new TokenRequest(GrantType.CIBA);
            tokenRequest.setAuthUsername(session.getClientId());
            tokenRequest.setAuthPassword(session.getClientSecret());
            tokenRequest.setAuthReqId(authReqId);

            TokenClient tokenClient = new TokenClient(session.getTokenEndpoint());
            tokenClient.setRequest(tokenRequest);
            return tokenClient.exec();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

}
