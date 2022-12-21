/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.interceptor;

import io.jans.configapi.core.interceptor.RequestAuditInterceptor;
import io.jans.configapi.model.configuration.AuditLogConf;
import io.jans.configapi.util.AuthUtil;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@RequestAuditInterceptor
@Priority(Interceptor.Priority.APPLICATION)
public class AuditLogInterceptor {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("audit");

    @Context
    UriInfo info;

    @Context
    HttpServletRequest request;

    @Context
    private HttpHeaders httpHeaders;

    @Inject
    AuthUtil authUtil;

    @SuppressWarnings({ "all" })
    @AroundInvoke
    public Object aroundReadFrom(InvocationContext context) throws Exception {
        System.out.println("\n\n SOP - AuditLogInterceptor::aroundReadFrom() - context = "+context);
        AUDIT_LOG.info("\n\n AuditLogInterceptor::aroundReadFrom() - context:{}", context);
        try {
            processRequest(context);

        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
        return context.proceed();
    }

    private void processRequest(InvocationContext context) {
        System.out.println("\n\n SOP - AuditLogInterceptor::processRequest() - context = "+context);
        AUDIT_LOG.info("\n\n AuditLogInterceptor::processRequest() - context:{}", context);
        Object[] ctxParameters = context.getParameters();
        Method method = context.getMethod();
        Class[] clazzArray = method.getParameterTypes();
        
        System.out.println("\n\n SOP - AuditLogInterceptor::processRequest() - ctxParameters ="+ctxParameters+", method ="+method+" , clazzArray = "+clazzArray);
        AUDIT_LOG.info("\n\n AuditLogInterceptor::processRequest() - ctxParameters:{}, method:{}, clazzArray:{}", ctxParameters, method, clazzArray);
        if (clazzArray != null && clazzArray.length > 0) {
            for (int i = 0; i < clazzArray.length; i++) {

                Object obj = ctxParameters[i];
                System.out.println("\n\n SOP - AuditLogInterceptor::processRequest() - obj ="+obj);
                AUDIT_LOG.info("\n\n AuditLogInterceptor::processRequest() - obj:{}", obj);
                // Audit log
                logAuditData(context, obj);

            }
        }
    }

    private <T> void logAuditData(InvocationContext context, T obj) {
        try {
            AuditLogConf auditLogConf = getAuditLogConf();
            System.out.println("\n\n SOP - AuditLogInterceptor::logAuditData() - auditLogConf ="+auditLogConf);
            AUDIT_LOG.info("\n\n AuditLogInterceptor::logAuditData() - auditLogConf:{}", auditLogConf);
            if (auditLogConf != null && auditLogConf.isEnabled()) {
                System.out.println("\n\n SOP - AuditLogInterceptor::logAuditData() - endpoint ="+info.getPath()+" , method = "+context.getMethod()+", from = "+request.getRemoteAddr()+" , user = "+httpHeaders.getHeaderString("User-inum")+" , data = "+obj);
                AUDIT_LOG.info("====== Request for endpoint:{}, method:{}, from:{}, user:{}, data:{} ", info.getPath(),
                        context.getMethod(), request.getRemoteAddr(), httpHeaders.getHeaderString("User-inum"), obj);
                Map<String, String> attributeMap = getAuditHeaderAttributes(auditLogConf);
                AUDIT_LOG.info("attributeMap:{} ", attributeMap);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private AuditLogConf getAuditLogConf() {
        return this.authUtil.getAuditLogConf();
    }

    private Map<String, String> getAuditHeaderAttributes(AuditLogConf auditLogConf) {
        System.out.println("\n\n SOP - AuditLogInterceptor::getAuditHeaderAttributes() - auditLogConf ="+auditLogConf);
        AUDIT_LOG.info("\n\n AuditLogInterceptor::getAuditHeaderAttributes() - auditLogConf:{}", auditLogConf);
        if (auditLogConf == null) {
            return Collections.emptyMap();
        }
        List<String> attributes = auditLogConf.getHeaderAttributes();
        System.out.println("\n\n SOP - AuditLogInterceptor::getAuditHeaderAttributes() - attributes ="+attributes);
        AUDIT_LOG.info("\n\n AuditLogInterceptor::getAuditHeaderAttributes() - attributes:{}", attributes);
        Map<String, String> attributeMap = null;
        if (attributes != null && !attributes.isEmpty()) {
            attributeMap = new HashMap<>();
            for (String attributeName : attributes) {

                String attributeValue = httpHeaders.getHeaderString(attributeName);
                attributeMap.put(attributeName, attributeValue);
            }
        }
        System.out.println("\n\n SOP - AuditLogInterceptor::getAuditHeaderAttributes() - attributeMap ="+attributeMap);
        AUDIT_LOG.info("\n\n AuditLogInterceptor::getAuditHeaderAttributes() - attributeMap:{}", attributeMap);
        return attributeMap;
    }

}
