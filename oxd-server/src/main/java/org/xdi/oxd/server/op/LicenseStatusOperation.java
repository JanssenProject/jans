/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.EmptyParams;
import org.xdi.oxd.common.response.LicenseStatusOpResponse;
import org.xdi.oxd.license.client.js.LicenseMetadata;
import org.xdi.oxd.server.license.LicenseService;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/11/2014
 */

public class LicenseStatusOperation extends BaseOperation<EmptyParams> {

    private static final Logger LOG = LoggerFactory.getLogger(LicenseStatusOperation.class);

    protected LicenseStatusOperation(Command command, final Injector injector) {
        super(command, injector, EmptyParams.class);
    }

    @Override
    public CommandResponse execute(EmptyParams params) {
        try {
            final LicenseService licenseService = getInjector().getInstance(LicenseService.class);
            final LicenseStatusOpResponse opResponse = new LicenseStatusOpResponse();
            opResponse.setValid(licenseService.isLicenseValid());

            if (licenseService.isLicenseValid()) {
                final LicenseMetadata metadata = licenseService.getMetadata();

                opResponse.setThreadCount(metadata.getThreadsCount());
                opResponse.setName(metadata.getLicenseName());
                opResponse.setFeatures(metadata.getLicenseFeatures());

            }

            return okResponse(opResponse);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return CommandResponse.INTERNAL_ERROR_RESPONSE;
    }
}
