/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.rp.service;

import io.jans.as.rp.ciba.CibaFlowState;
import io.jans.as.rp.ciba.CibaRequestSession;
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
