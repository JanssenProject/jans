package org.xdi.oxd.server.op;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Injector;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.RegisterClient;
import org.xdi.oxauth.client.RegisterRequest;
import org.xdi.oxauth.client.RegisterResponse;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.ErrorResponseCode;
import org.xdi.oxd.common.ErrorResponseException;
import org.xdi.oxd.common.params.UpdateSiteParams;
import org.xdi.oxd.common.response.UpdateSiteResponse;
import org.xdi.oxd.server.service.Rp;

import javax.ws.rs.HttpMethod;
import java.io.IOException;
import java.util.Date;
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
    public CommandResponse execute(UpdateSiteParams params) {
        final Rp rp = getRp();

        LOG.info("Updating rp ... rp: " + rp);
        persistRp(rp, params);

        UpdateSiteResponse response = new UpdateSiteResponse();
        response.setOxdId(rp.getOxdId());
        return okResponse(response);
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
            throw new ErrorResponseException(ErrorResponseCode.INVALID_REGISTRATION_CLIENT_URL);
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

        Date clientSecretExpiresAt = params.getClientSecretExpiresAt();
        if (clientSecretExpiresAt != null) {
            // translate it into milliseconds if someone sends it in seconds by miskate
            if (clientSecretExpiresAt.getTime() != 0 && String.valueOf(clientSecretExpiresAt.getTime()).length() < 11) {
                clientSecretExpiresAt = new Date(clientSecretExpiresAt.getTime() * 1000);
            }
            request.setClientSecretExpiresAt(clientSecretExpiresAt);
            rp.setClientSecretExpiresAt(clientSecretExpiresAt);
        }

        List<ResponseType> responseTypes = Lists.newArrayList();
        if (params.getResponseTypes() != null && !params.getResponseTypes().isEmpty()) {
            for (String type : params.getResponseTypes()) {
                responseTypes.add(ResponseType.fromString(type));

                request.setResponseTypes(responseTypes);
                rp.setResponseTypes(params.getResponseTypes());
            }
        }

        List<GrantType> grantTypes = Lists.newArrayList();
        if (params.getGrantType() != null) {
            for (String grantType : params.getGrantType()) {
                GrantType t = GrantType.fromString(grantType);
                if (t != null) {
                    grantTypes.add(t);
                }
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
            if (!Strings.isNullOrEmpty(params.getPostLogoutRedirectUri())) {
                redirectUris.add(params.getPostLogoutRedirectUri());
            }

            request.setRedirectUris(Lists.newArrayList(redirectUris));
            rp.setRedirectUris(Lists.newArrayList(redirectUris));
        }

        if (!Strings.isNullOrEmpty(params.getClientJwksUri())) {
            request.setJwksUri(params.getClientJwksUri());
        }

        if (!Strings.isNullOrEmpty(params.getPostLogoutRedirectUri())) {
            request.setPostLogoutRedirectUris(Lists.newArrayList(params.getPostLogoutRedirectUri()));
        }

        if (params.getContacts() != null && !params.getContacts().isEmpty()) {
            request.setContacts(params.getContacts());
            rp.setContacts(params.getContacts());
        }

        if (params.getScope() != null && !params.getScope().isEmpty()) {
            request.setScopes(params.getScope());
            rp.setScope(params.getScope());
        }

        if (!Strings.isNullOrEmpty(params.getClientSectorIdentifierUri())) {
            request.setSectorIdentifierUri(params.getClientSectorIdentifierUri());
            rp.setSectorIdentifierUri(params.getClientSectorIdentifierUri());
        }

        if (params.getClientLogoutUri() != null && !params.getClientLogoutUri().isEmpty()) {
            request.setFrontChannelLogoutUris(Lists.newArrayList(params.getClientLogoutUri()));
        }

        if (params.getClientRequestUris() != null && !params.getClientRequestUris().isEmpty()) {
            request.setRequestUris(params.getClientRequestUris());
        }
        return request;
    }
}