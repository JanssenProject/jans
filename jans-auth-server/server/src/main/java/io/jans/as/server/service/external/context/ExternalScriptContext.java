/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.external.context;

import io.jans.as.model.util.Util;
import io.jans.as.server.util.ServerUtil;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.model.base.CustomEntry;
import org.apache.commons.net.util.SubnetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Holds object required in custom scripts
 *
 * @author Yuriy Movchan  Date: 07/01/2015
 */

public class ExternalScriptContext extends io.jans.service.external.context.ExternalScriptContext {

    private static final Logger log = LoggerFactory.getLogger(ExternalScriptContext.class);

    private final PersistenceEntryManager ldapEntryManager;

    private WebApplicationException webApplicationException;

    public ExternalScriptContext(HttpServletRequest httpRequest) {
        this(httpRequest, null);
    }

    public ExternalScriptContext(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        super(httpRequest, httpResponse);
        this.ldapEntryManager = ServerUtil.getLdapManager();
    }

    public PersistenceEntryManager getPersistenceEntryManager() {
        return ldapEntryManager;
    }

    public boolean isInNetwork(String cidrNotation) {
        final String ip = getIpAddress();
        if (Util.allNotBlank(ip, cidrNotation)) {
            final SubnetUtils utils = new SubnetUtils(cidrNotation);
            return utils.getInfo().isInRange(ip);
        }
        return false;
    }

    protected CustomEntry getEntryByDn(String dn, String... ldapReturnAttributes) {
        try {
            return ldapEntryManager.find(dn, CustomEntry.class, ldapReturnAttributes);
        } catch (EntryPersistenceException epe) {
            log.error("Failed to find entry '{}'", dn);
        }

        return null;
    }

    protected String getEntryAttributeValue(String dn, String attributeName) {
        final CustomEntry entry = getEntryByDn(dn, attributeName);
        if (entry != null) {
            return entry.getCustomAttributeValue(attributeName);
        }

        return "";
    }

    public WebApplicationException getWebApplicationException() {
        return webApplicationException;
    }

    public void setWebApplicationException(WebApplicationException webApplicationException) {
        this.webApplicationException = webApplicationException;
    }

    public WebApplicationException createWebApplicationException(int status, String entity) {
        this.webApplicationException = new WebApplicationException(Response
                .status(status)
                .entity(entity)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build());
        return this.webApplicationException;
    }

    public void throwWebApplicationExceptionIfSet() {
        if (webApplicationException != null)
            throw webApplicationException;
    }
}
