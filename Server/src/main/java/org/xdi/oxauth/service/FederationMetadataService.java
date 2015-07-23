/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.federation.FederationMetadata;
import org.xdi.oxauth.model.federation.FederationOP;
import org.xdi.oxauth.model.federation.FederationRP;
import org.xdi.oxauth.util.LdapUtils;

import com.unboundid.ldap.sdk.Filter;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 13/09/2012
 */
@Scope(ScopeType.STATELESS)
@Name("federationMetadataService")
@AutoCreate
public class FederationMetadataService {

    public static class InvalidIdException extends Exception {
    }

    @Logger
    private Log log;

    @In
    private LdapEntryManager ldapEntryManager;

    public List<FederationMetadata> getMetadataList() {
        try {
            final FederationMetadata m = new FederationMetadata();
            m.setDn(ConfigurationFactory.instance().getBaseDn().getFederationMetadata());

            final List<FederationMetadata> entries = ldapEntryManager.findEntries(m);
            if (entries != null) {
                return entries;
            }
        } catch (Exception e) {
            // catch all exception because ldap manager may throw any exception inside
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public FederationMetadata getMetadata(String p_federationId, boolean p_loadSatellites) throws InvalidIdException {
        if (StringUtils.isBlank(p_federationId)) {
            throw new InvalidIdException();
        }

        try {
            final FederationMetadata m = new FederationMetadata();
            m.setDn(ConfigurationFactory.instance().getBaseDn().getFederationMetadata());
            m.setId(p_federationId);

            final List<FederationMetadata> entries = ldapEntryManager.findEntries(m);
            if (entries != null && !entries.isEmpty()) {
                final int size = entries.size();
                if (size == 1) {
                    final FederationMetadata result = entries.get(0);
                    if (p_loadSatellites) {
                        // load rps
                        final List<String> rps = result.getRps();
                        if (rps != null && !rps.isEmpty()) {
                            final Filter rpFilter = LdapUtils.createAnyFilterFromDnList("inum", rps);
                            if (rpFilter != null) {
                                final List<FederationRP> rpList = ldapEntryManager.findEntries(
                                        ConfigurationFactory.instance().getBaseDn().getFederationRP(),
                                        FederationRP.class, rpFilter);
                                result.setRpList(rpList);
                            } else {
                                log.trace("Skip loading of RPs for metadataId: {0}", p_federationId);
                            }
                        }

                        // load ops
                        final List<String> ops = result.getOps();
                        if (ops != null && !ops.isEmpty()) {
                            final Filter opFilter = LdapUtils.createAnyFilterFromDnList("inum", ops);
                            if (opFilter != null) {
                                final List<FederationOP> opList = ldapEntryManager.findEntries(
                                        ConfigurationFactory.instance().getBaseDn().getFederationOP(),
                                        FederationOP.class, opFilter);
                                result.setOpList(opList);
                            } else {
                                log.trace("Skip loading of OPs for metadataId: {0}", p_federationId);
                            }
                        }

                    }
                    return result;
                } else {
                    log.error("There is more then one federation metadata object with id {0}", p_federationId);
                }
            } else {
                log.trace("Invalid federation metadata id: {0}", p_federationId);
                throw new InvalidIdException();
            }
        } catch (InvalidIdException e) {
            throw e; // rethrow exception
        } catch (Exception e) {
            // catch all exception because ldap manager may throw any exception inside
            log.error(e.getMessage(), e);
        }
        return null;
    }
}
