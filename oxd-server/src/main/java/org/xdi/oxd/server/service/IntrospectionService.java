package org.xdi.oxd.server.service;

import com.google.inject.Inject;
import org.jboss.resteasy.client.ClientResponseFailure;
import org.jboss.resteasy.client.ProxyFactory;
import org.jboss.resteasy.spi.ReaderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.model.common.IntrospectionResponse;
import org.xdi.oxd.server.introspection.BackCompatibleIntrospectionResponse;
import org.xdi.oxd.server.introspection.BackCompatibleIntrospectionService;
import org.xdi.oxd.server.introspection.ClientFactory;

import java.util.Date;

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
        final String introspectionEndpoint = discoveryService.getConnectDiscoveryResponseByOxdId(oxdId).getIntrospectionEndpoint();
        final org.xdi.oxauth.client.service.IntrospectionService introspectionService = ProxyFactory.create(org.xdi.oxauth.client.service.IntrospectionService.class, introspectionEndpoint, httpService.getClientExecutor());

        try {
            IntrospectionResponse response = introspectionService.introspectToken("Bearer " + umaTokenService.getPat(oxdId).getToken(), accessToken);
            return response; // we need local variable to force convertion here
        } catch (ClientResponseFailure e) {
            int status = e.getResponse().getStatus();
            LOG.debug("Failed to introspect token. Entity: " + e.getResponse().getEntity(String.class) + ", status: " + status, e);
            if (status == 400 || status == 401) {
                LOG.debug("Try maybe PAT is lost on AS, force refresh PAT and re-try ...");
                umaTokenService.obtainPat(oxdId); // force to refresh PAT
                return introspectionService.introspectToken("Bearer " + umaTokenService.getPat(oxdId).getToken(), accessToken);
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
                response.setSubject(backResponse.getSubject());
                response.setAudience(backResponse.getAudience());
                response.setTokenType(backResponse.getTokenType());
                response.setActive(backResponse.isActive());
                response.setScopes(backResponse.getScopes());
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

    public static Integer dateToSeconds(Date date) {
        return date != null ? (int) (date.getTime() / 1000) : null;
    }
}
