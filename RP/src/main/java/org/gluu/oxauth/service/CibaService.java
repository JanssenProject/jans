package org.gluu.oxauth.service;

import org.gluu.oxauth.model.ciba.CibaFlowState;
import org.gluu.oxauth.model.ciba.CibaRequestSession;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class CibaService {

    @Inject
    private Logger log;

    public void processPingCallback(String callbackJsonBody, CibaRequestSession session) {
        log.info("Processing ping callback: {}, session: {}", callbackJsonBody, session);
        session.setState(CibaFlowState.RESPONSE_GOTTEN);
        session.setCallbackJsonBody(callbackJsonBody);
    }

    public void processPushCallback(String callbackJsonBody, CibaRequestSession session) {
        log.info("Processing push callback: {}, session: {}", callbackJsonBody, session);
        session.setState(CibaFlowState.RESPONSE_GOTTEN);
        session.setCallbackJsonBody(callbackJsonBody);
    }
}
