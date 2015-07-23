/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.federation.FederationRequest;
import org.xdi.oxauth.model.federation.FederationScopePolicy;
import org.xdi.oxauth.model.federation.FederationSkipPolicy;
import org.xdi.oxauth.model.federation.FederationTrust;
import org.xdi.oxauth.model.federation.FederationTrustStatus;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.util.Pair;
import org.xdi.oxauth.util.ServerUtil;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 08/10/2012
 */
@Scope(ScopeType.STATELESS)
@Name("federationDataService")
@AutoCreate
public class FederationDataService {

    @Logger
    private Log log;

    @In
    private InumService inumService;

    @In
    private LdapEntryManager ldapEntryManager;

    @In
    private ClientService clientService;

    private Pair<String, String> generateNewDN() {
        return inumService.generateNewDN(ConfigurationFactory.instance().getBaseDn().getFederationRequest());
    }

    public boolean persist(FederationRequest p_federationRequest) {
        if (p_federationRequest != null && StringUtils.isNotBlank(p_federationRequest.getFederationId())) {
            try {
                final Pair<String, String> dn = generateNewDN();
                p_federationRequest.setDn(dn.getSecond());
                p_federationRequest.setId(dn.getFirst());
                ldapEntryManager.persist(p_federationRequest);
                return true;
            } catch (Exception e) {
                log.trace(e.getMessage(), e);
            }
        }
        return false;
    }

    public static FederationDataService instance() {
        if (!Contexts.isEventContextActive() && !Contexts.isApplicationContextActive()) {
            Lifecycle.beginCall();
        }

        return ServerUtil.instance(FederationDataService.class);
    }

    public static boolean skipAuthorization(List<FederationTrust> p_list) {
        if (p_list != null && !p_list.isEmpty()) {
            final FederationSkipPolicy skipPolicy = FederationSkipPolicy.fromStringWithDefault(ConfigurationFactory.instance().getConfiguration().getFederationSkipPolicy());
            if (skipPolicy != null) {
                switch (skipPolicy) {
                    case OR:
                        for (FederationTrust t : p_list) {
                            if (Boolean.TRUE.equals(t.getSkipAuthorization())) {
                                return true;
                            }
                        }
                        break;
                    case AND:
                        for (FederationTrust t : p_list) {
                            if (Boolean.TRUE.equals(t.getSkipAuthorization())) {
                                // do nothing, here we check on TRUE because in skipAuthorization attribute
                                // we may have some 'trash' which needs to be considered as false
                            } else {
                                return false;
                            }
                        }
                        return true;
                    default:
                        break;
                }
            }
        }
        return false;
    }

    public static List<String> getScopes(List<FederationTrust> p_list) {
        final List<String> result = new ArrayList<String>();
        if (p_list != null && !p_list.isEmpty()) {
            final FederationScopePolicy policy = FederationScopePolicy.fromStringWithDefault(ConfigurationFactory.instance().getConfiguration().getFederationScopePolicy());
            if (policy != null) {
                switch (policy) {
                    case JOIN:
                        for (FederationTrust t : p_list) {
                            if (t.getScopes() != null && !t.getScopes().isEmpty()) {
                                result.addAll(t.getScopes());
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        return result;
    }

    public boolean hasAnyActiveTrust(String clientId) {
        if (StringUtils.isNotBlank(clientId)) {
            final Client client = clientService.getClient(clientId);
            if (client != null) {
                return hasAnyActiveTrust(client);
            }
        }
        return false;
    }

    public boolean hasAnyActiveTrust(Client p_client) {
        final List<FederationTrust> list = getTrustByClient(p_client, FederationTrustStatus.ACTIVE);
        return list != null && !list.isEmpty();
    }

    public List<FederationTrust> getTrustByClient(Client p_client, FederationTrustStatus p_status) {
        final List<FederationTrust> result = new ArrayList<FederationTrust>();
        if (p_client != null && p_status != null) {
            final String[] redirectUris = p_client.getRedirectUris();
            if (!ArrayUtils.isEmpty(redirectUris)) {
                final List<FederationTrust> list = getTrustByAnyRedirectUri(Arrays.asList(redirectUris), p_status);
                if (list != null && !list.isEmpty()) {
                    final String uri = p_client.getFederationURI();
                    final String id = p_client.getFederationId();
                    if (StringUtils.isNotBlank(uri)) {
                        if (StringUtils.isNotBlank(id)) {
                            for (FederationTrust t : list) {
                                if (id.equalsIgnoreCase(t.getFederationId()) && uri.equalsIgnoreCase(t.getFederationMetadataUri())) {
                                    result.add(t);
                                }
                            }
                        } else {
                            for (FederationTrust t : list) {
                                if (uri.equalsIgnoreCase(t.getFederationMetadataUri())) {
                                    result.add(t);
                                }
                            }
                        }
                    } else {
                        result.addAll(list);
                    }
                }
            }
        }
        return result;
    }

    public List<FederationTrust> getTrustByAnyRedirectUri(List<String> p_redirectUri, FederationTrustStatus p_status) {
        if (p_redirectUri != null && !p_redirectUri.isEmpty()) {
            final String baseDn = ConfigurationFactory.instance().getBaseDn().getFederationTrust();
            try {
                final Filter filter;
                if (p_status == null) {
                    filter = Filter.create(createFilter(p_redirectUri));
                } else {
                    filter = Filter.create(String.format("&(%s)(oxAuthFederationTrustStatus=%s)", createFilter(p_redirectUri), p_status.getValue()));
                }
                final List<FederationTrust> result = ldapEntryManager.findEntries(baseDn, FederationTrust.class, filter, 100);
                if (result != null) {
                    return result;
                }
            } catch (LDAPException e) {
                log.trace(e.getMessage(), e);
            }
        }
        return Collections.emptyList();
    }

    public static String createFilter(List<String> p_redirectUriList) {
        final StringBuilder sb = new StringBuilder("|");
        if (p_redirectUriList != null && !p_redirectUriList.isEmpty()) {
            for (String uri : p_redirectUriList) {
                if (StringUtils.isNotBlank(uri)) {
                    sb.append(String.format("(oxAuthRedirectURI=%s)", uri));
                }
            }
        }
        return sb.toString();
    }
}
