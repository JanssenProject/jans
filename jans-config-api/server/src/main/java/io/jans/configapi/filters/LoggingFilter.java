/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.filters;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.configapi.model.configuration.AuditLogConf;
import io.jans.configapi.util.AuthUtil;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import java.util.Enumeration;

@Provider
public class LoggingFilter implements ContainerRequestFilter {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("audit");

    @Context
    UriInfo info;

    @Context
    HttpServletRequest request;

    @Context
    private HttpHeaders httpHeaders;

    @Inject
    AuthUtil authUtil;

    @Inject
    Logger logger;

    public void filter(ContainerRequestContext context) {
        logger.info("***********************************************************************");
        logger.info(" request:{}, path:{}, from:{} ", context.getMethod(), info.getPath(), request.getRemoteAddr());

        // Request object audit
        processRequest(context);
    }

    private void processRequest(ContainerRequestContext context) {
        logger.info("Audit Log  - context:{}, info:{}, request:{}, httpHeaders:{}, AUDIT_LOG:{}", context, info,
                request, httpHeaders, AUDIT_LOG);

        // Get Audit config
        AuditLogConf auditLogConf = getAuditLogConf();
        logger.info("auditLogConf:{}", auditLogConf);

        // Log if enabled
        if (auditLogConf != null && auditLogConf.isEnabled()) {
            AUDIT_LOG.info("\n ********************** Request Start ********************** ");
            // Request audit
            String beanClassName = context.getClass().getName();
            String methodName = context.getMethod();

            AUDIT_LOG.info("requestType:{}, endpoint:{}, beanClassName:{}, method:{}, from:{}, user:{} ",
                    request.getMethod(), info.getPath(), beanClassName, methodName, request.getRemoteAddr(),
                    httpHeaders.getHeaderString("User-inum"));

            // Header attribute audit
            Map<String, String> headerData = getAuditHeaderAttributes(auditLogConf);
            AUDIT_LOG.info("headerData:{} ", headerData);

            logRequestData();
        }

        AUDIT_LOG.info("\n ********************** Request End ********************** ");
    }

    private AuditLogConf getAuditLogConf() {
        return this.authUtil.getAuditLogConf();
    }

    private Map<String, String> getAuditHeaderAttributes(AuditLogConf auditLogConf) {
        logger.info("AuditLog::getAuditHeaderAttributes() - auditLogConf:{}", auditLogConf);
        if (auditLogConf == null) {
            return Collections.emptyMap();
        }
        List<String> attributes = auditLogConf.getHeaderAttributes();
        logger.info("AuditLog::getAuditHeaderAttributes() - attributes:{}", attributes);

        Map<String, String> attributeMap = null;
        if (attributes != null && !attributes.isEmpty()) {
            attributeMap = new HashMap<>();
            for (String attributeName : attributes) {

                String attributeValue = httpHeaders.getHeaderString(attributeName);
                attributeMap.put(attributeName, attributeValue);
            }
        }

        logger.info("AuditLog::getAuditHeaderAttributes() - attributeMap:{}", attributeMap);
        return attributeMap;
    }

    private void logRequestData() {
        Enumeration<String> attrNames = request.getAttributeNames();
        logger.debug("Audit Log  process - attrNames:{}", attrNames);

        // Attributes
        if (attrNames != null) {
            while (attrNames.hasMoreElements()) {
                String attrName = attrNames.nextElement();
                Object obj = request.getAttribute(attrName);
                String objClass = (obj != null ? obj.getClass().getName() : null);

                logger.info("Request attribute obj:{}", request.getAttribute(attrName));
                AUDIT_LOG.info("attrNames:{}, objectType:{}, obj:{} ", attrNames, objClass, obj);

            }
        }

        Enumeration<String> paramNames = request.getParameterNames();
        logger.debug("Audit Log  process - paramNames:{}", paramNames);
        // Attributes
        if (paramNames != null) {
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                Object obj = request.getAttribute(paramName);
                String objClass = (obj != null ? obj.getClass().getName() : null);

                logger.info("Request parameter obj:{}", request.getAttribute(paramName));
                AUDIT_LOG.info("paramName:{}, objectType:{}, obj:{} ", paramName, objClass, obj);

            }
        }
    }
}
