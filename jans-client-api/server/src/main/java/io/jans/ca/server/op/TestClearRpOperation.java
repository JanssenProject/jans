/*
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package io.jans.ca.server.op;

import io.jans.as.model.uma.UmaNeedInfoResponse;
import io.jans.as.model.util.Util;
import io.jans.ca.common.Command;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.Jackson2;
import io.jans.ca.common.params.EmptyParams;
import io.jans.ca.common.params.RpGetRptParams;
import io.jans.ca.common.response.ClearTestResponse;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.persistence.service.PersistenceService;
import io.jans.ca.server.persistence.service.PersistenceServiceImpl;
import io.jans.ca.server.service.RpService;
import io.jans.ca.server.service.ServiceProvider;
import io.jans.ca.server.service.UmaTokenService;
import jakarta.inject.Inject;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class TestClearRpOperation extends BaseOperation<EmptyParams> {

    private static final Logger LOG = LoggerFactory.getLogger(TestClearRpOperation.class);
    RpService rpService;
    PersistenceServiceImpl persistenceService;

    public TestClearRpOperation(Command command, ServiceProvider serviceProvider) {
        super(command, serviceProvider, EmptyParams.class);
        this.rpService = serviceProvider.getRpService();
        this.persistenceService = this.rpService.getPersistenceService();
    }

    @Override
    public IOpResponse execute(EmptyParams params) throws Exception {
        try {
            persistenceService.create();
            rpService.removeAllRps();
            rpService.load();
            LOG.debug("Finished removeExistingRps successfullly.");
            return new ClearTestResponse("OK");
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("Failed to remove existing RPs.", e);
            return new ClearTestResponse("FAIL");
        }
    }

}
