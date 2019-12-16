package org.gluu.oxd.server.service;

import com.google.inject.Inject;
import org.gluu.oxauth.client.RegisterClient;
import org.gluu.oxauth.client.RegisterRequest;
import org.gluu.oxauth.client.RegisterResponse;
import org.gluu.oxd.server.Utils;
import org.gluu.oxd.server.mapper.RegisterResponseMapper;
import org.gluu.oxd.server.persistence.PersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.util.Calendar;
import java.util.Date;

public class RpSyncService {

    private RpService rpService;

    private PersistenceService persistenceService;

    private static final Logger LOG = LoggerFactory.getLogger(RpSyncService.class);

    @Inject
    public RpSyncService(RpService rpService, PersistenceService persistenceService) {
        this.rpService = rpService;
        this.persistenceService = persistenceService;
    }

    public static boolean shouldSync(Rp rp) {
        if (rp == null || !rp.isSyncClientFromOp())
            return false;

        if ((Utils.addTimeToDate(rp.getLastSynced(), rp.getSyncClientPeriodInSeconds(), Calendar.SECOND).getTime() < new Date().getTime()))
            return true;

        return false;
    }

    public Rp getRp(String oxdId) {
        Rp rp = rpService.getRp(oxdId);
        if (!shouldSync(rp))
            return rp;

        return sync(rp);
    }

    //this method created for running thetest cases to skip validations of the original method `public Rp getRp(String oxdId)`.
    public Rp getRpForTest(String oxdId) {
        Rp rp = rpService.getRps().get(oxdId);
        if (!shouldSync(rp))
            return rp;

        return sync(rp);
    }

    private Rp sync(Rp rp) {
        if (!shouldSync(rp))
            return rp;

        try {
            // read client with oxauth-client and update Rp object
            final RegisterResponse response = getClientMetadataFromOP(rp.getClientRegistrationClientUri(), rp.getClientRegistrationAccessToken());
            if (RegisterResponseMapper.hasClientMetadataChangedAtOp(rp, response)) {
                persistenceService.update(rp);
            }

            LOG.debug("Successfully synced Rp object from OP. Rp: " + rp.toString());
            return rp;
        } catch (Exception e) {
            LOG.error("Error in sync Rp object from OP: ", e);
            return rp;
        }
    }

    private RegisterResponse getClientMetadataFromOP(String clientRegistrationClientUri, String clientRegistrationAccessToken) {
        final RegisterRequest request = new RegisterRequest(clientRegistrationAccessToken);
        request.setHttpMethod(HttpMethod.GET);

        final RegisterClient registerClient = new RegisterClient(clientRegistrationClientUri);
        registerClient.setRequest(request);
        return registerClient.exec();
    }
}
