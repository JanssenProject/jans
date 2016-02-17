/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.common;

import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.RDN;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jboss.seam.Component;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;
import org.xdi.oxauth.model.authorize.JwtAuthorizationRequest;
import org.xdi.oxauth.model.ldap.TokenLdap;
import org.xdi.oxauth.model.ldap.TokenType;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.service.ClientService;
import org.xdi.oxauth.service.GrantService;
import org.xdi.oxauth.service.UserService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version 0.9, 08/14/2014
 */

public class AuthorizationGrantListLdap implements IAuthorizationGrantList {

    private static final Log LOGGER = Logging.getLog(AuthorizationGrantListLdap.class);

    private final GrantService m_grantServive;
    private final UserService m_userService;
    private final ClientService m_clientService;

    private AuthorizationGrantListLdap() {
        m_grantServive = GrantService.instance();
        m_userService = (UserService) Component.getInstance(UserService.class);
        m_clientService = (ClientService) Component.getInstance(ClientService.class);
    }

    public static AuthorizationGrantListLdap instance() {
        return new AuthorizationGrantListLdap();
    }

    @Override
    public List<AuthorizationGrant> getAuthorizationGrants() {
        return Collections.emptyList(); // return nothing in LDAP case
    }

    @Override
    public void removeAuthorizationGrants(List<AuthorizationGrant> authorizationGrants) {
        if (authorizationGrants != null && !authorizationGrants.isEmpty()) {
            for (AuthorizationGrant r : authorizationGrants) {
                m_grantServive.remove(r);
            }
        }
    }

    @Override
    public void addAuthorizationGrant(AuthorizationGrant authorizationGrant) {
        // do nothing
    }

    @Override
    public AuthorizationGrant createAuthorizationGrant(User user, Client client, Date authenticationTime) {
        return new AuthorizationGrant(user, null, client, authenticationTime);
    }

    @Override
    public AuthorizationCodeGrant createAuthorizationCodeGrant(User user, Client client, Date authenticationTime) {
        final AuthorizationCodeGrant grant = new AuthorizationCodeGrant(user, client, authenticationTime);
        final AuthorizationGrantLdap ldapGrant = (AuthorizationGrantLdap) grant.getGrant();
        ldapGrant.persist(grant.getAuthorizationCode());
        return grant;
    }

    @Override
    public ImplicitGrant createImplicitGrant(User user, Client client, Date authenticationTime) {
        return new ImplicitGrant(user, client, authenticationTime);
    }

    @Override
    public ClientCredentialsGrant createClientCredentialsGrant(User user, Client client) {
        return new ClientCredentialsGrant(user, client);
    }

    @Override
    public ResourceOwnerPasswordCredentialsGrant createResourceOwnerPasswordCredentialsGrant(User user, Client client) {
        return new ResourceOwnerPasswordCredentialsGrant(user, client);
    }

    @Override
    public AuthorizationCodeGrant getAuthorizationCodeGrant(String clientId, String authorizationCode) {
        return (AuthorizationCodeGrant) load(clientId, authorizationCode);
    }

    @Override
    public AuthorizationGrant getAuthorizationGrantByRefreshToken(String clientId, String refreshTokenCode) {
        return load(clientId, refreshTokenCode);
    }

    @Override
    public List<AuthorizationGrant> getAuthorizationGrant(String clientId) {
        final List<AuthorizationGrant> result = new ArrayList<AuthorizationGrant>();
        try {
            final List<TokenLdap> entries = m_grantServive.getGrantsOfClient(clientId);
            if (entries != null && !entries.isEmpty()) {
                for (TokenLdap t : entries) {
                    final AuthorizationGrant grant = asGrant(t);
                    if (grant != null) {
                        result.add(grant);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.trace(e.getMessage(), e);
        }
        return result;
    }

    @Override
    public AuthorizationGrant getAuthorizationGrantByAccessToken(String accessToken) {
        final TokenLdap tokenLdap = m_grantServive.getGrantsByCode(accessToken);
        if (tokenLdap != null && (tokenLdap.getTokenTypeEnum() == TokenType.ACCESS_TOKEN || tokenLdap.getTokenTypeEnum() == TokenType.LONG_LIVED_ACCESS_TOKEN)) {
            return asGrant(tokenLdap);
        }
        return null;
    }

    @Override
    public AuthorizationGrant getAuthorizationGrantByIdToken(String idToken) {
        return asGrant(m_grantServive.getGrantsByCode(idToken));
    }

    public AuthorizationGrant load(String clientId, String p_code) {
        return asGrant(m_grantServive.getGrantsByCodeAndClient(p_code, clientId));
    }

    public static String extractClientIdFromTokenDn(String p_dn) {
        try {
            if (StringUtils.isNotBlank(p_dn)) {
                final RDN[] rdNs = DN.getRDNs(p_dn);
                if (ArrayUtils.isNotEmpty(rdNs)) {
                    for (RDN r : rdNs) {
                        final String[] names = r.getAttributeNames();
                        if (ArrayUtils.isNotEmpty(names) && Arrays.asList(names).contains("inum")) {
                            final String[] values = r.getAttributeValues();
                            if (ArrayUtils.isNotEmpty(values)) {
                                return values[0];
                            }
                        }
                    }
                }
            }
        } catch (LDAPException e) {
            LOGGER.trace(e.getMessage(), e);
        }

        return "";
    }

    public AuthorizationGrant asGrant(TokenLdap tokenLdap) {
        if (tokenLdap != null) {
            final AuthorizationGrantType grantType = AuthorizationGrantType.fromString(tokenLdap.getGrantType());
            if (grantType != null) {
                final User user = m_userService.getUser(tokenLdap.getUserId());
                final Client client = m_clientService.getClient(extractClientIdFromTokenDn(tokenLdap.getDn()));
                final Date authenticationTime = org.xdi.oxauth.model.util.StringUtils.parseSilently(tokenLdap.getAuthenticationTime());
                final String nonce = tokenLdap.getNonce();

                AuthorizationGrant result;
                switch (grantType) {
                    case AUTHORIZATION_CODE:
                        result = new AuthorizationCodeGrant(user, client, authenticationTime);
                        break;
                    case CLIENT_CREDENTIALS:
                        result = new ClientCredentialsGrant(user, client);
                        break;
                    case IMPLICIT:
                        result = new ImplicitGrant(user, client, authenticationTime);
                        break;
                    case RESOURCE_OWNER_PASSWORD_CREDENTIALS:
                        result = new ResourceOwnerPasswordCredentialsGrant(user, client);
                        break;
                    default:
                        return null;
                }

                final String grantId = tokenLdap.getGrantId();
                final String jwtRequest = tokenLdap.getJwtRequest();
                final String authMode = tokenLdap.getAuthMode();

                result.setNonce(nonce);
                result.setTokenLdap(tokenLdap);
                if (StringUtils.isNotBlank(grantId)) {
                    result.setGrantId(grantId);
                }
                result.setScopes(Util.splittedStringAsList(tokenLdap.getScope(), " "));

                if (StringUtils.isNotBlank(jwtRequest)) {
                    try {
                        result.setJwtAuthorizationRequest(new JwtAuthorizationRequest(jwtRequest, client));
                    } catch (Exception e) {
                        LOGGER.trace(e.getMessage(), e);
                    }
                }

                result.setAcrValues(authMode);

                if (tokenLdap.getTokenTypeEnum() != null) {
                    switch (tokenLdap.getTokenTypeEnum()) {
                        case AUTHORIZATION_CODE:
                            if (result instanceof AuthorizationCodeGrant) {
                                final AuthorizationCode code = new AuthorizationCode(tokenLdap.getTokenCode(), tokenLdap.getCreationDate(), tokenLdap.getExpirationDate());
                                final AuthorizationCodeGrant g = (AuthorizationCodeGrant) result;
                                g.setAuthorizationCode(code);
                            }
                            break;
                        case REFRESH_TOKEN:
                            final RefreshToken refreshToken = new RefreshToken(tokenLdap.getTokenCode(), tokenLdap.getCreationDate(), tokenLdap.getExpirationDate());
                            result.setRefreshTokens(Arrays.asList(refreshToken));
                            break;
                        case ACCESS_TOKEN:
                            final AccessToken accessToken = new AccessToken(tokenLdap.getTokenCode(), tokenLdap.getCreationDate(), tokenLdap.getExpirationDate());
                            result.setAccessTokens(Arrays.asList(accessToken));
                            break;
                        case ID_TOKEN:
                            final IdToken idToken = new IdToken(tokenLdap.getTokenCode(), tokenLdap.getCreationDate(), tokenLdap.getExpirationDate());
                            result.setIdToken(idToken);
                            break;
                        case LONG_LIVED_ACCESS_TOKEN:
                            final AccessToken longLivedAccessToken = new AccessToken(tokenLdap.getTokenCode(), tokenLdap.getCreationDate(), tokenLdap.getExpirationDate());
                            result.setLongLivedAccessToken(longLivedAccessToken);
                            break;
                    }
                }
                return result;
            }
        }
        return null;
    }
}