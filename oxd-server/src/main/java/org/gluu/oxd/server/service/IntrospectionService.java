package org.gluu.oxd.server.service;

import com.google.inject.Inject;
import org.jboss.resteasy.client.ClientResponseFailure;
import org.jboss.resteasy.client.ProxyFactory;
import org.jboss.resteasy.spi.ReaderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gluu.oxauth.model.common.IntrospectionResponse;
import org.gluu.oxauth.model.uma.UmaMetadata;
import org.gluu.oxd.common.introspection.CorrectRptIntrospectionResponse;
import org.gluu.oxd.common.introspection.CorrectUmaPermission;
import org.gluu.oxd.server.introspection.*;

import java.util.*;

/**
 * @author yuriyz
 */
public class IntrospectionService {

    private static final Logger LOG = LoggerFactory.getLogger(IntrospectionService.class);

    private HttpService httpService;
    private UmaTokenService umaTokenService;
    private DiscoveryService discoveryService;

    @Inject
    public IntrospectionService(HttpService httpService, UmaTokenService umaTokenService, DiscoveryService discoveryService) {
        this.httpService = httpService;
        this.umaTokenService = umaTokenService;
        this.discoveryService = discoveryService;
    }

    public IntrospectionResponse introspectToken(String oxdId, String accessToken) {
        return introspectToken(oxdId, accessToken, true);
    }

    private IntrospectionResponse introspectToken(String oxdId, String accessToken, boolean retry) {
        final String introspectionEndpoint = discoveryService.getConnectDiscoveryResponseByOxdId(oxdId).getIntrospectionEndpoint();
        final org.gluu.oxauth.client.service.IntrospectionService introspectionService = ProxyFactory.create(org.gluu.oxauth.client.service.IntrospectionService.class, introspectionEndpoint, httpService.getClientExecutor());

        try {
            IntrospectionResponse response = introspectionService.introspectToken("Bearer " + umaTokenService.getPat(oxdId).getToken(), accessToken);
            return response; // we need local variable to force convertion here
        } catch (ClientResponseFailure e) {
            int status = e.getResponse().getStatus();
            LOG.debug("Failed to introspect token. Entity: " + e.getResponse().getEntity(String.class) + ", status: " + status, e);
            if (retry && (status == 400 || status == 401)) {
                LOG.debug("Try maybe PAT is lost on AS, force refresh PAT and re-try ...");
                umaTokenService.obtainPat(oxdId); // force to refresh PAT
                return introspectToken(oxdId, accessToken, false);
            } else {
                throw e;
            }
        } catch (Throwable e) {
            LOG.trace("Exception during access token introspection.", e);
            if (e instanceof ReaderException) { // dummy construction but checked JsonParseException is thrown inside jackson provider, so we don't have choice
                // trying to handle compatiblity issue.
                LOG.trace("Trying to handle compatibility issue ...");
                BackCompatibleIntrospectionService backCompatibleIntrospectionService = ClientFactory.instance().createBackCompatibleIntrospectionService(introspectionEndpoint, httpService.getClientExecutor());
                BackCompatibleIntrospectionResponse backResponse = backCompatibleIntrospectionService.introspectToken("Bearer " + umaTokenService.getPat(oxdId).getToken(), accessToken);
                LOG.trace("Handled compatibility issue. Response: " + backResponse);

                IntrospectionResponse response = new IntrospectionResponse();
                response.setSub(backResponse.getSubject());
                response.setAudience(backResponse.getAudience());
                response.setTokenType(backResponse.getTokenType());
                response.setActive(backResponse.isActive());
                response.setScope(backResponse.getScopes());
                if (!backResponse.getScope().isEmpty()) {
                    response.setScope(backResponse.getScope());
                }
                response.setIssuer(backResponse.getIssuer());
                response.setUsername(backResponse.getUsername());
                response.setClientId(backResponse.getClientId());
                response.setJti(backResponse.getJti());
                response.setAcrValues(backResponse.getAcrValues());
                response.setExpiresAt(dateToSeconds(backResponse.getExpiresAt()));
                response.setIssuedAt(dateToSeconds(backResponse.getIssuedAt()));

                return response;
            }
            throw e;
        }
    }

    public CorrectRptIntrospectionResponse introspectRpt(String oxdId, String rpt) {
        return introspectRpt(oxdId, rpt, true);
    }

    private CorrectRptIntrospectionResponse introspectRpt(String oxdId, String rpt, boolean retry) {
        final UmaMetadata metadata = discoveryService.getUmaDiscoveryByOxdId(oxdId);

        try {
            final CorrectRptIntrospectionService introspectionService = ClientFactory.instance().createCorrectRptStatusService(metadata, httpService.getClientExecutor());
            return introspectionService.requestRptStatus("Bearer " + umaTokenService.getPat(oxdId).getToken(), rpt, "");
        } catch (ClientResponseFailure e) {
            int httpStatus = e.getResponse().getStatus();
            if (retry && (httpStatus == 401 || httpStatus == 400 || httpStatus == 403)) {
                umaTokenService.obtainPat(oxdId).getToken();
                return introspectRpt(oxdId, rpt, false);
            } else {
                throw e;
            }
        } catch (Throwable e) {
            LOG.trace("Exception during rpt introspection, message: " + e.getMessage());
            if (e instanceof ReaderException) { // dummy construction but checked JsonParseException is thrown inside jackson provider, so we don't have choice
                // trying to handle compatiblity issue.
                LOG.trace("Trying to handle compatibility issue ...");
                BadRptIntrospectionService badService = ClientFactory.instance().createBadRptStatusService(metadata, httpService.getClientExecutor());
                BadRptIntrospectionResponse badResponse = badService.requestRptStatus("Bearer " + umaTokenService.getPat(oxdId).getToken(), rpt, "");

                LOG.trace("Handled compatibility issue. Response: " + badResponse);

                final List<CorrectUmaPermission> permissions = new ArrayList<>();

                CorrectRptIntrospectionResponse response = new CorrectRptIntrospectionResponse();
                response.setActive(badResponse.getActive());
                response.setClientId(badResponse.getClientId());
                response.setJti(badResponse.getJti());
                response.setExpiresAt(dateToSeconds(badResponse.getExpiresAt()));
                response.setIssuedAt(dateToSeconds(badResponse.getIssuedAt()));
                response.setNbf(dateToSeconds(badResponse.getNbf()));
                response.setPermissions(permissions);

                if (badResponse.getPermissions() != null) {
                    for (BadUmaPermission badPermission : badResponse.getPermissions()) {
                        CorrectUmaPermission p = new CorrectUmaPermission();
                        p.setExpiresAt(dateToSeconds(badPermission.getExpiresAt()));
                        p.setResourceId(badPermission.getResourceId());
                        p.setScopes(badPermission.getScopes());

                        permissions.add(p);
                    }
                }

                return response;
            }
            throw e;
        }
    }

    public static Integer dateToSeconds(Date date) {
        return date != null ? (int) (date.getTime() / 1000) : null;
    }
}
