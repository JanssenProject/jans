package org.xdi.oxd.server.license;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.server.ServerLauncher;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * @author yuriyz
 */
@Provider
public class LicenseGuardFilter implements ContainerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(LicenseGuardFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!ServerLauncher.getInjector().getInstance(LicenseService.class).isLicenseValid()) {
            LOG.error("License is invalid. Please check your license_id and make sure it is not expired.");
            LOG.error("Unable to fetch valid license after " + LicenseFileUpdateService.RETRY_LIMIT +
                    " re-tries. Shutdown the server.");
            ServerLauncher.shutdownDueToInvalidLicense();
        }
    }
}
