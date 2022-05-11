package io.jans.ca.server.service;

import io.jans.as.model.common.IntrospectionResponse;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.ca.common.introspection.CorrectRptIntrospectionResponse;
import io.jans.ca.common.introspection.CorrectUmaPermission;
import io.jans.ca.server.introspection.*;
import io.jans.ca.server.op.OpClientFactoryImpl;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.spi.ReaderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author yuriyz
 */
@ApplicationScoped
public class IntrospectionService {

    private static final Logger LOG = LoggerFactory.getLogger(IntrospectionService.class);
    @Inject
    HttpService httpService;
    @Inject
    UmaTokenService umaTokenService;
    @Inject
    DiscoveryService discoveryService;
    @Inject
    OpClientFactoryImpl opClientFactory;

    public IntrospectionResponse introspectToken(String rpId, String accessToken) {
        return introspectToken(rpId, accessToken, true);
    }

    private IntrospectionResponse introspectToken(String rpId, String accessToken, boolean retry) {
        final String introspectionEndpoint = discoveryService.getConnectDiscoveryResponseByRpId(rpId).getIntrospectionEndpoint();
        LOG.info("Instrospection Endpoint: {}", introspectionEndpoint);
        final ResteasyClient client = ((ResteasyClientBuilder) ResteasyClientBuilder.newBuilder()).httpEngine(httpService.getClientEngine()).build();
        final ResteasyWebTarget target = client.target(UriBuilder.fromPath(introspectionEndpoint));
        final io.jans.as.client.service.IntrospectionService introspectionService = target.proxy(io.jans.as.client.service.IntrospectionService.class);

        try {
            String token = umaTokenService.getOAuthToken(rpId).getToken();
            LOG.info("Token instrospection: {}", token);
            final IntrospectionResponse response = introspectionService.introspectToken("Bearer " + token, accessToken);
            return response; // we need local variable to force convertion here
        } catch (ClientErrorException e) {
            int status = e.getResponse().getStatus();
            LOG.debug("Failed to introspect token. Entity: " + e.getResponse().readEntity(String.class) + ", status: " + status, e);
            if (retry && (status == 400 || status == 401)) {
                LOG.debug("Try maybe OAuthToken is lost on AS, force refresh OAuthToken and re-try ...");
                umaTokenService.obtainOauthToken(rpId); // force to refresh OAuthToken
                return introspectToken(rpId, accessToken, false);
            } else {
                throw e;
            }
        } catch (Throwable e) {
            LOG.trace("Exception during access token introspection.", e);
            if (e instanceof ReaderException) { // dummy construction but checked JsonParseException is thrown inside jackson provider, so we don't have choice
                // trying to handle compatiblity issue.
                LOG.trace("Trying to handle compatibility issue ...");
                BackCompatibleIntrospectionService backCompatibleIntrospectionService = ClientFactory.instance().createBackCompatibleIntrospectionService(introspectionEndpoint, httpService.getClientEngine());
                BackCompatibleIntrospectionResponse backResponse = backCompatibleIntrospectionService.introspectToken("Bearer " + umaTokenService.getOAuthToken(rpId).getToken(), accessToken);
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

    public CorrectRptIntrospectionResponse introspectRpt(String rpId, String rpt) {
        return introspectRpt(rpId, rpt, true);
    }

    private CorrectRptIntrospectionResponse introspectRpt(String rpId, String rpt, boolean retry) {
        final UmaMetadata metadata = discoveryService.getUmaDiscoveryByRpId(rpId);

        try {
            final CorrectRptIntrospectionService introspectionService = opClientFactory.createClientFactory().createCorrectRptStatusService(metadata, httpService.getClientEngine());
            return introspectionService.requestRptStatus("Bearer " + umaTokenService.getPat(rpId).getToken(), rpt, "");
        } catch (ClientErrorException e) {
            int httpStatus = e.getResponse().getStatus();
            if (retry && (httpStatus == 401 || httpStatus == 400 || httpStatus == 403)) {
                umaTokenService.obtainPat(rpId).getToken();
                return introspectRpt(rpId, rpt, false);
            } else {
                throw e;
            }
        } catch (Throwable e) {
            LOG.trace("Exception during rpt introspection, message: " + e.getMessage());
            if (e instanceof ReaderException) { // dummy construction but checked JsonParseException is thrown inside jackson provider, so we don't have choice
                // trying to handle compatiblity issue.
                LOG.trace("Trying to handle compatibility issue ...");
                BadRptIntrospectionService badService = ClientFactory.instance().createBadRptStatusService(metadata, httpService.getClientEngine());
                BadRptIntrospectionResponse badResponse = badService.requestRptStatus("Bearer " + umaTokenService.getPat(rpId).getToken(), rpt, "");

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
