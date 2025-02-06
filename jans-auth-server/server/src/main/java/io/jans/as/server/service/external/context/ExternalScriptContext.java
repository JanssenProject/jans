/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.external.context;

import io.jans.as.model.authzdetails.AuthzDetail;
import io.jans.as.model.util.Util;
import io.jans.as.server.authorize.ws.rs.AuthzRequest;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.util.ServerUtil;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.model.base.CustomEntry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.net.util.SubnetUtils;
import org.jboss.resteasy.spi.NoLogWebApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds object required in custom scripts
 *
 * @author Yuriy Movchan  Date: 07/01/2015
 */

public class ExternalScriptContext extends io.jans.service.external.context.ExternalScriptContext {

    private static final Logger log = LoggerFactory.getLogger(ExternalScriptContext.class);

    private final PersistenceEntryManager persistenceEntryManager;

    private ExecutionContext executionContext;

    private NoLogWebApplicationException webApplicationException;

    public ExternalScriptContext(ExecutionContext executionContext) {
        this(executionContext.getHttpRequest(), executionContext.getHttpResponse());
        this.executionContext = executionContext;
    }

    public ExternalScriptContext(HttpServletRequest httpRequest) {
        this(httpRequest, null);
    }

    public ExternalScriptContext(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        super(httpRequest, httpResponse);
        this.persistenceEntryManager = ServerUtil.getLdapManager();
    }

    public String getRequestParameter(String parameterName) {
        return httpRequest != null ? httpRequest.getParameter(parameterName) : null;
    }

    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    public AuthzRequest getAuthzRequest() {
        return executionContext != null ? executionContext.getAuthzRequest() : null;
    }

    public AuthzDetail getAuthzDetail() {
        return executionContext != null ? executionContext.getAuthzDetail() : null;
    }

    public PersistenceEntryManager getPersistenceEntryManager() {
        return persistenceEntryManager;
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
            return persistenceEntryManager.find(dn, CustomEntry.class, ldapReturnAttributes);
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

    public NoLogWebApplicationException getWebApplicationException() {
        return webApplicationException;
    }

    public void setWebApplicationException(NoLogWebApplicationException webApplicationException) {
        this.webApplicationException = webApplicationException;
    }

    public NoLogWebApplicationException createWebApplicationException(Response response) {
        return new NoLogWebApplicationException(response);
    }

    public NoLogWebApplicationException createWebApplicationException(int status, String entity) {
        final CacheControl cacheControl = new CacheControl();
        cacheControl.setNoStore(true);

        this.webApplicationException = new NoLogWebApplicationException(Response
                .status(status)
                .entity(entity)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .cacheControl(cacheControl)
                .build());
        return this.webApplicationException;
    }

    public void throwWebApplicationExceptionIfSet() {
        if (webApplicationException != null)
            throw webApplicationException;
    }
}
