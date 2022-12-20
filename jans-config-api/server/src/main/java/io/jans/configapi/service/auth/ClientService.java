/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service.auth;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.OrganizationService;
import io.jans.as.common.service.common.InumService;
import io.jans.as.common.util.AttributeConstants;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.register.ApplicationType;
import io.jans.configapi.core.model.SearchRequest;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.Serializable;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

/**
 * @author Mougang T.Gasmyr
 *
 */
@ApplicationScoped
public class ClientService implements Serializable {

    private static final long serialVersionUID = 7912416439116338984L;

    @Inject
    private transient PersistenceEntryManager persistenceEntryManager;

    @Inject
    private transient Logger logger;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private transient InumService inumService;

    @Inject
    AttributeService attributeService;

    @Inject
    transient ScopeService scopeService;

    @Inject
    transient AppConfiguration appConfiguration;
    
    @Inject
    ConfigurationService configurationService;

    public boolean contains(String clientDn) {
        return persistenceEntryManager.contains(clientDn, Client.class);
    }

    public void addClient(Client client) {
        setClientDefaultAttributes(client, false);
        persistenceEntryManager.persist(client);
    }

    public void removeClient(Client client) {
        persistenceEntryManager.removeRecursively(client.getDn(), Client.class);
    }

    public void updateClient(Client client) {
        persistenceEntryManager.merge(client);
    }

    public Client getClientByInum(String inum) {
        Client result = null;
        try {
            result = persistenceEntryManager.find(Client.class, getDnForClient(inum));
        } catch (Exception ex) {
            logger.error("Failed to load client entry", ex);
        }
        return result;
    }

    public List<Client> searchClients(String pattern, int sizeLimit) {

        logger.debug("Search Clients with pattern:{}, sizeLimit:{}", pattern, sizeLimit);

        String[] targetArray = new String[] { pattern };
        Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null, targetArray,
                null);
        Filter descriptionFilter = Filter.createSubstringFilter(AttributeConstants.DESCRIPTION, null, targetArray,
                null);
        Filter inumFilter = Filter.createSubstringFilter(AttributeConstants.INUM, null, targetArray, null);
        Filter searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter, inumFilter);

        logger.debug("Search Clients with searchFilter:{}", searchFilter);
        return persistenceEntryManager.findEntries(getDnForClient(null), Client.class, searchFilter, sizeLimit);
    }

    public List<Client> getAllClients(int sizeLimit) {
        return persistenceEntryManager.findEntries(getDnForClient(null), Client.class, null, sizeLimit);
    }

    public List<Client> getAllClients() {
        return persistenceEntryManager.findEntries(getDnForClient(null), Client.class, null);
    }

    public PagedResult<Client> getClients(SearchRequest searchRequest) {
        logger.debug("Search Clients with searchRequest:{}", searchRequest);

        Filter searchFilter = null;
        List<Filter> filters = new ArrayList<>();
        if (searchRequest.getFilterAssertionValue() != null && !searchRequest.getFilterAssertionValue().isEmpty()) {

            for (String assertionValue : searchRequest.getFilterAssertionValue()) {
                String[] targetArray = new String[] { assertionValue };
                Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null,
                        targetArray, null);
                Filter descriptionFilter = Filter.createSubstringFilter(AttributeConstants.DESCRIPTION, null,
                        targetArray, null);
                Filter inumFilter = Filter.createSubstringFilter(AttributeConstants.INUM, null, targetArray, null);
                filters.add(Filter.createORFilter(displayNameFilter, descriptionFilter, inumFilter));
            }
            searchFilter = Filter.createORFilter(filters);
        }

        logger.debug("Clients searchFilter:{}", searchFilter);

        return persistenceEntryManager.findPagedEntries(getDnForClient(null), Client.class, searchFilter, null,
                searchRequest.getSortBy(), SortOrder.getByValue(searchRequest.getSortOrder()),
                searchRequest.getStartIndex(), searchRequest.getCount(), searchRequest.getMaxCount());

    }

    public Client getClientByDn(String dn) {
        try {
            return persistenceEntryManager.find(Client.class, dn);
        } catch (Exception e) {
            logger.warn("", e);
            return null;
        }
    }

    public ApplicationType[] getApplicationType() {
        return ApplicationType.values();
    }

    public SubjectType[] getSubjectTypes() {
        return SubjectType.values();
    }

    public SignatureAlgorithm[] getSignatureAlgorithms() {
        return SignatureAlgorithm.values();
    }

    public String getDnForClient(String inum) {
        String orgDn = organizationService.getDnForOrganization();
        if (StringHelper.isEmpty(inum)) {
            return String.format("ou=clients,%s", orgDn);
        }
        return String.format("inum=%s,ou=clients,%s", inum, orgDn);
    }

    public String generateInumForNewClient() {
        String newInum = null;
        String newDn = null;
        int trycount = 0;
        do {
            if (trycount < InumService.MAX_IDGEN_TRY_COUNT) {
                newInum = inumService.generateId("client");
                trycount++;
            } else {
                newInum = inumService.generateDefaultId();
            }
            newDn = getDnForClient(newInum);
        } while (persistenceEntryManager.contains(newDn, Client.class));
        return newInum;
    }

    public Client setClientDefaultAttributes(Client client, boolean update) {
        logger.debug("Client data - client:{}", client);
        if (client == null) {
            return client;
        }

        logger.trace("client.getApplicationType:{}, client.getRedirectUris():{}, client.getClaimRedirectUris():{}",
                client.getApplicationType(), client.getRedirectUris(), client.getClaimRedirectUris());

        List<String> redirectUris = client.getRedirectUris() != null ? Arrays.asList(client.getRedirectUris()) : null;
        if (redirectUris != null && !redirectUris.isEmpty()) {
            redirectUris = new ArrayList<>(new HashSet<>(redirectUris)); // Remove repeated elements
            client.setRedirectUris(redirectUris.toArray(new String[0]));
        }
        List<String> claimsRedirectUris = client.getClaimRedirectUris() != null
                ? Arrays.asList(client.getClaimRedirectUris())
                : null;
        if (claimsRedirectUris != null && !claimsRedirectUris.isEmpty()) {
            claimsRedirectUris = new ArrayList<>(new HashSet<>(claimsRedirectUris)); // Remove repeated elements
            client.setClaimRedirectUris(claimsRedirectUris.toArray(new String[0]));
        }
        logger.trace(
                "After setting client.getApplicationType:{}, client.getRedirectUris():{}, client.getClaimRedirectUris():{}",
                client.getApplicationType(), client.getRedirectUris(), client.getClaimRedirectUris());

        client.setApplicationType(
                client.getApplicationType() != null ? client.getApplicationType() : ApplicationType.WEB);

        if (StringUtils.isNotBlank(client.getSectorIdentifierUri())) {
            client.setSectorIdentifierUri(client.getSectorIdentifierUri());
        }

        logger.trace("client.getApplicationType():{}, client.getResponseTypes():{}, client.getGrantTypes():{}",
                client.getApplicationType(), client.getResponseTypes(), client.getGrantTypes());
        Set<ResponseType> responseTypeSet = client.getResponseTypes() != null
                ? new HashSet<>(Arrays.asList(client.getResponseTypes()))
                : new HashSet<>();
        Set<GrantType> grantTypeSet = client.getGrantTypes() != null
                ? new HashSet<>(Arrays.asList(client.getGrantTypes()))
                : new HashSet<>();

        if (isTrue(appConfiguration.getGrantTypesAndResponseTypesAutofixEnabled())) {
            if (isTrue(appConfiguration.getClientRegDefaultToCodeFlowWithRefresh())) {
                if (responseTypeSet.isEmpty() && grantTypeSet.isEmpty()) {
                    responseTypeSet.add(ResponseType.CODE);
                }
                if (responseTypeSet.contains(ResponseType.CODE)) {
                    grantTypeSet.add(GrantType.AUTHORIZATION_CODE);
                    grantTypeSet.add(GrantType.REFRESH_TOKEN);
                }
                if (grantTypeSet.contains(GrantType.AUTHORIZATION_CODE)) {
                    responseTypeSet.add(ResponseType.CODE);
                    grantTypeSet.add(GrantType.REFRESH_TOKEN);
                }
            }
            if (responseTypeSet.contains(ResponseType.TOKEN) || responseTypeSet.contains(ResponseType.ID_TOKEN)) {
                grantTypeSet.add(GrantType.IMPLICIT);
            }
            if (grantTypeSet.contains(GrantType.IMPLICIT)) {
                responseTypeSet.add(ResponseType.TOKEN);
            }
        }

        responseTypeSet.retainAll(appConfiguration.getAllResponseTypesSupported());
        grantTypeSet.retainAll(appConfiguration.getGrantTypesSupported());
        logger.trace("After setting - client.getResponseTypes():{}, client.getGrantTypes():{}",
                client.getResponseTypes(), client.getGrantTypes());

        Set<GrantType> dynamicGrantTypeDefault = appConfiguration.getDynamicGrantTypeDefault();
        grantTypeSet.retainAll(dynamicGrantTypeDefault);

        if (!update || (responseTypeSet != null && !responseTypeSet.isEmpty())) {
            client.setResponseTypes(responseTypeSet.toArray(new ResponseType[0]));
        }
        if (!update || (isTrue(appConfiguration.getEnableClientGrantTypeUpdate()))
                && (client.getGrantTypes() != null && client.getGrantTypes().length > 0)) {
            client.setGrantTypes(grantTypeSet.toArray(new GrantType[0]));
        }

        logger.trace("Set client.getResponseTypes():{}, client.getGrantTypes():{}", client.getResponseTypes(),
                client.getGrantTypes());
        List<String> contacts = client.getContacts() != null ? Arrays.asList(client.getContacts()) : null;
        if (contacts != null && !contacts.isEmpty()) {
            contacts = new ArrayList<>(new HashSet<>(contacts)); // Remove repeated elements
            client.setContacts(contacts.toArray(new String[0]));
        }

        logger.trace("client.getTokenEndpointAuthMethod():{}", client.getTokenEndpointAuthMethod());
        if (StringUtils.isBlank(client.getTokenEndpointAuthMethod())) {
            // If omitted, the default is client_secret_basic
            client.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_BASIC.toString());
        }

        logger.trace("client.getDefaultAcrValues():{}", client.getDefaultAcrValues());
        List<String> defaultAcrValues = client.getDefaultAcrValues() != null
                ? Arrays.asList(client.getDefaultAcrValues())
                : null;
        if (defaultAcrValues != null && !defaultAcrValues.isEmpty()) {
            defaultAcrValues = new ArrayList<>(new HashSet<>(defaultAcrValues)); // Remove repeated elements
            client.setDefaultAcrValues(defaultAcrValues.toArray(new String[defaultAcrValues.size()]));
        }

        logger.debug("client.getGroups():{}", client.getGroups());
        final List<String> groups = client.getGroups() != null ? Arrays.asList(client.getGroups()) : null;
        if (groups != null && !groups.isEmpty()) {
            client.setGroups(new HashSet<>(groups).toArray(new String[0])); // remove duplicates
        }

        logger.debug("client.getGroups():{}, client.getPostLogoutRedirectUris():{}", client.getGroups(),
                client.getPostLogoutRedirectUris());
        List<String> postLogoutRedirectUris = client.getPostLogoutRedirectUris() != null
                ? Arrays.asList(client.getPostLogoutRedirectUris())
                : null;
        if (postLogoutRedirectUris != null && !postLogoutRedirectUris.isEmpty()) {
            postLogoutRedirectUris = new ArrayList<>(new HashSet<>(postLogoutRedirectUris)); // Remove repeated elements
            client.setPostLogoutRedirectUris(postLogoutRedirectUris.toArray(new String[postLogoutRedirectUris.size()]));
        }

        List<String> requestUris = client.getRequestUris() != null ? Arrays.asList(client.getRequestUris()) : null;
        if (requestUris != null && !requestUris.isEmpty()) {
            requestUris = new ArrayList<>(new HashSet<>(requestUris)); // Remove repeated elements
            client.setRequestUris(requestUris.toArray(new String[requestUris.size()]));
        }

        List<String> authorizedOrigins = client.getAuthorizedOrigins() != null
                ? Arrays.asList(client.getAuthorizedOrigins())
                : null;
        if (authorizedOrigins != null && !authorizedOrigins.isEmpty()) {
            authorizedOrigins = new ArrayList<>(new HashSet<>(authorizedOrigins)); // Remove repeated elements
            client.setAuthorizedOrigins(authorizedOrigins.toArray(new String[authorizedOrigins.size()]));
        }

        logger.debug("client.getScopes():{}, appConfiguration.getDynamicRegistrationScopesParamEnabled():{}",
                client.getScopes(), appConfiguration.getDynamicRegistrationScopesParamEnabled());

        List<String> claims = client.getClaims() != null ? Arrays.asList(client.getClaims()) : null;
        if (claims != null && !claims.isEmpty()) {
            List<String> claimsDn = attributeService.getAttributesDn(claims);
            client.setClaims(claimsDn.toArray(new String[claimsDn.size()]));
        }
        logger.debug("client.getClaims():{}, client.getAttributes().getAuthorizedAcrValues():{}", client.getClaims(),
                client.getAttributes().getAuthorizedAcrValues());

        List<String> authorizedAcrValues = client.getAttributes().getAuthorizedAcrValues();
        if (authorizedAcrValues != null && !authorizedAcrValues.isEmpty()) {
            authorizedAcrValues = new ArrayList<>(new HashSet<>(authorizedAcrValues)); // Remove repeated elements
            client.getAttributes().setAuthorizedAcrValues(authorizedAcrValues);
        }
        logger.debug("Final client.getAttributes().getAuthorizedAcrValues():{}",
                client.getAttributes().getAuthorizedAcrValues());

        // Custom params
        updateCustomAttributes(client);

        return client;
    }

    private void updateCustomAttributes(Client client) {
        logger.debug(
                "ClientService::updateCustomAttributes() - client:{}, appConfiguration.getDynamicRegistrationCustomObjectClass():{},appConfiguration.getDynamicRegistrationCustomAttributes():{} ",
                client, appConfiguration.getDynamicRegistrationCustomObjectClass(),
                appConfiguration.getDynamicRegistrationCustomAttributes());

        // custom object class
        final String customOC = appConfiguration.getDynamicRegistrationCustomObjectClass();
        String persistenceType = configurationService.getPersistenceType();
        if (PersistenceEntryManager.PERSITENCE_TYPES.ldap.name().equals(persistenceType) && StringUtils.isNotBlank(customOC)) {
            client.setCustomObjectClasses(new String[] { customOC });
        }else {
            client.setCustomObjectClasses(null);
        }

        // custom attributes (custom attributes must be in custom object class)
        final List<String> attrList = appConfiguration.getDynamicRegistrationCustomAttributes();
        if (attrList == null || attrList.isEmpty()) {
            return;
        }

        logger.debug("ClientService::updateCustomAttributes() - client.getCustomAttributes():{}, attrList:{}",
                client.getCustomAttributes(), attrList);
        for (String attr : attrList) {
            logger.debug("ClientService::updateCustomAttributes() - attr:{}", attr);

        }
    }
}
