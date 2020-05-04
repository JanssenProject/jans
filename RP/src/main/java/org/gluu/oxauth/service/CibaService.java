package org.gluu.oxauth.service;

import org.gluu.oxauth.model.ciba.CibaCallback;
import org.gluu.oxauth.model.ciba.CibaFlowState;
import org.gluu.oxauth.model.ciba.CibaRequestSession;
import org.gluu.oxauth.model.ciba.PingCibaCallback;
import org.gluu.oxauth.model.common.BackchannelTokenDeliveryMode;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class CibaService {

    @Inject
    private Logger log;

    public void processPingCallback(PingCibaCallback callback, CibaRequestSession session) {
        log.info("Processing ping callback: {}, session: {}", callback, session);

        session.setState(CibaFlowState.ACCEPTED);
    }

}
