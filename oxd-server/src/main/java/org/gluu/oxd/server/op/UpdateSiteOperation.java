package org.gluu.oxd.server.op;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Injector;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gluu.oxauth.client.RegisterClient;
import org.gluu.oxauth.client.RegisterRequest;
import org.gluu.oxauth.client.RegisterResponse;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.ErrorResponseCode;
import org.gluu.oxd.common.params.UpdateSiteParams;
import org.gluu.oxd.common.response.IOpResponse;
import org.gluu.oxd.common.response.UpdateSiteResponse;
import org.gluu.oxd.server.HttpException;
import org.gluu.oxd.server.service.Rp;

import javax.ws.rs.HttpMethod;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 11/03/2016
 */

public class UpdateSiteOperation extends BaseOperation<UpdateSiteParams> {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateSiteOperation.class);

    /**
     * Base constructor
     *
     * @param command command
     */
    protected UpdateSiteOperation(Command command, final Injector injector) {
        super(command, injector, UpdateSiteParams.class);
    }

    @Override
    public IOpResponse execute(UpdateSiteParams params) {
        final Rp rp = getRp();

        LOG.info("Updating rp ... rp: " + rp);
        persistRp(rp, params);

        UpdateSiteResponse response = new UpdateSiteResponse();
        response.setOxdId(rp.getOxdId());
        return response;
    }

    private void persistRp(Rp rp, UpdateSiteParams params) {

        try {
            updateRegisteredClient(rp, params);
            getRpService().update(rp);

            LOG.info("RP updated: " + rp);
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist RP, params: " + params, e);
        }
    }

    private void updateRegisteredClient(Rp rp, UpdateSiteParams params) {
        if (StringUtils.isBlank(rp.getClientRegistrationClientUri())) {
            LOG.error("Registration client url is blank.");
            throw new HttpException(ErrorResponseCode.INVALID_REGISTRATION_CLIENT_URL);
        }

        final RegisterClient registerClient = new RegisterClient(rp.getClientRegistrationClientUri());
        registerClient.setRequest(createRegisterClientRequest(rp, params));
        registerClient.setExecutor(getHttpService().getClientExecutor());
        final RegisterResponse response = registerClient.exec();
        if (response != null) {
            if (response.getStatus() == 200) {
                LOG.trace("Client updated successfully. for rp - client_id: " + rp.getClientId());
                return;
            } else {
                LOG.error("Response is not OK (200).");
            }
        } else {
            LOG.error("RegisterClient response is null.");
        }
        if (!Strings.isNullOrEmpty(response.getErrorDescription())) {
            LOG.error(response.getErrorDescription());
        }

        throw new RuntimeException("Failed to update client for rp. Details:" + response.getEntity());
    }

    private RegisterRequest createRegisterClientRequest(Rp rp, UpdateSiteParams params) {

        final RegisterRequest request = new RegisterRequest(rp.getClientRegistrationAccessToken());
        request.setHttpMethod(HttpMethod.PUT); // force update

        List<ResponseType> responseTypes = Lists.newArrayList();
        if (params.getResponseTypes() != null && !params.getResponseTypes().isEmpty()) {
            for (String type : params.getResponseTypes()) {
                responseTypes.add(ResponseType.fromString(type));

                request.setResponseTypes(responseTypes);
                rp.setResponseTypes(params.getResponseTypes());
            }
        }

        if (params.getRptAsJwt() != null) {
            request.setRptAsJwt(params.getRptAsJwt());
        }

        List<GrantType> grantTypes = Lists.newArrayList();
        for (String grantType : params.getGrantType() != null ? params.getGrantType() : rp.getGrantType()) {
            GrantType t = GrantType.fromString(grantType);
            if (t != null) {
                grantTypes.add(t);
            }
        }

        request.setGrantTypes(grantTypes);
        rp.setGrantType(params.getGrantType());

        Set<String> redirectUris = new HashSet<>();
        if (StringUtils.isNotBlank(params.getAuthorizationRedirectUri())) {
            redirectUris.add(params.getAuthorizationRedirectUri());
            rp.setAuthorizationRedirectUri(params.getAuthorizationRedirectUri());
        } else if (StringUtils.isNotBlank(rp.getAuthorizationRedirectUri())) {
            redirectUris.add(rp.getAuthorizationRedirectUri());
        }

        if (params.getRedirectUris() != null && !params.getRedirectUris().isEmpty()) {
            redirectUris.addAll(params.getRedirectUris());
            if (params.getPostLogoutRedirectUris() != null && !params.getPostLogoutRedirectUris().isEmpty()) {
                redirectUris.addAll(params.getPostLogoutRedirectUris());
            }

            request.setRedirectUris(Lists.newArrayList(redirectUris));
            rp.setRedirectUris(Lists.newArrayList(redirectUris));
        }

        if (params.getAcrValues() != null && !params.getAcrValues().isEmpty()) {
            rp.setAcrValues(params.getAcrValues());
            request.setDefaultAcrValues(params.getAcrValues());
        } else {
            request.setDefaultAcrValues(rp.getAcrValues());
        }

        if (params.getAccessTokenAsJwt() != null) {
            rp.setAccessTokenAsJwt(params.getAccessTokenAsJwt());
            request.setAccessTokenAsJwt(params.getAccessTokenAsJwt());
        } else {
            request.setAccessTokenAsJwt(rp.getAccessTokenAsJwt());
        }

        if (params.getAccessTokenSigningAlg() != null) {
            rp.setAccessTokenSigningAlg(params.getAccessTokenSigningAlg());
            request.setAccessTokenSigningAlg(SignatureAlgorithm.fromString(params.getAccessTokenSigningAlg()));
        } else {
            request.setAccessTokenSigningAlg(SignatureAlgorithm.fromString(rp.getAccessTokenSigningAlg()));
        }

        if (!Strings.isNullOrEmpty(params.getClientJwksUri())) {
            request.setJwksUri(params.getClientJwksUri());
        }

        if (params.getPostLogoutRedirectUris() != null && !params.getPostLogoutRedirectUris().isEmpty()) {
            request.setPostLogoutRedirectUris(Lists.newArrayList(params.getPostLogoutRedirectUris()));
        }

        if (params.getContacts() != null) {
            request.setContacts(params.getContacts());
            rp.setContacts(params.getContacts());
        } else {
            request.setContacts(rp.getContacts());
        }

        if (params.getScope() != null) {
            request.setScopes(params.getScope());
            rp.setScope(params.getScope());
        } else {
            request.setScopes(rp.getScope());
        }

        if (!Strings.isNullOrEmpty(params.getClientSectorIdentifierUri())) {
            request.setSectorIdentifierUri(params.getClientSectorIdentifierUri());
            rp.setSectorIdentifierUri(params.getClientSectorIdentifierUri());
        }

        if (params.getClientLogoutUri() != null && !params.getClientLogoutUri().isEmpty()) {
            rp.setFrontChannelLogoutUri(Lists.newArrayList(params.getClientLogoutUri()));
            request.setFrontChannelLogoutUris(Lists.newArrayList(params.getClientLogoutUri()));
        } else {
            request.setFrontChannelLogoutUris(rp.getFrontChannelLogoutUri());
        }

        if (params.getClientRequestUris() != null && !params.getClientRequestUris().isEmpty()) {
            request.setRequestUris(params.getClientRequestUris());
        }
        return request;
    }
}