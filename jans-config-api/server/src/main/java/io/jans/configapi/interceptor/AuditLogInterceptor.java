/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.interceptor;

import io.jans.configapi.core.interceptor.RequestAuditInterceptor;
import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.model.configuration.ApiAppConfiguration;
import io.jans.configapi.model.configuration.AuditLogConf;
import io.jans.configapi.model.configuration.ObjectDetails;
import io.jans.configapi.util.AuthUtil;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Interceptor
@RequestAuditInterceptor
@Priority(Interceptor.Priority.APPLICATION)
public class AuditLogInterceptor {

    private static final Logger LOG = LogManager.getLogger(AuditLogInterceptor.class);
    private static final Logger AUDIT_LOG = LogManager.getLogger("audit");

    @Inject
    AuthUtil authUtil;

    @Inject
    ApiAppConfiguration apiAppConfiguration;

    @SuppressWarnings({ "all" })
    @AroundInvoke
    public Object aroundReadFrom(InvocationContext context) throws Exception {

        try {

            // return if audit disabled
            if (apiAppConfiguration.isDisableAuditLogger()) {
                AUDIT_LOG.error("Audit is disabled by disableAuditLogger config.");
                return context.proceed();
            }

            // Log for audit
            HttpServletRequest request = ((BaseResource) context.getTarget()).getHttpRequest();
            HttpHeaders httpHeaders = ((BaseResource) context.getTarget()).getHttpHeaders();
            UriInfo uriInfo = ((BaseResource) context.getTarget()).getUriInfo();

            // Get Audit config
            AuditLogConf auditLogConf = getAuditLogConf();
            String method = request.getMethod();
            LOG.trace(" method:{}, ignoreMethod(method, auditLogConf):{}, ignoreAnnotation(method, auditLogConf):{}", method, ignoreHttpMethod(method, auditLogConf), ignoreAnnotation(method, auditLogConf));

            // Log if enabled
            if (auditLogConf.isEnabled() && !ignoreHttpMethod(method, auditLogConf)) {

                // Request audit
                String client = httpHeaders.getHeaderString("jans-client");
                String userInum = httpHeaders.getHeaderString("User-inum");

                // Log request without data
                AUDIT_LOG.error("User:{} {} {} using client:{}", userInum, getAction(method),
                        getResource(uriInfo.getPath()), client);

                if (auditLogConf.isLogData()) {
                    // Log request data
                    processRequest(context, auditLogConf);
                }
            }

        } catch (Exception ex) {
            LOG.error("Not able to log audit details due to error:{}", ex);
        }
        return context.proceed();
    }

    private void processRequest(InvocationContext context, AuditLogConf auditLogConf) {
        LOG.info("Process Audit Log Interceptor - context:{}, auditLogConf:{}", context, auditLogConf);

        Object[] ctxParameters = context.getParameters();
        Method method = context.getMethod();
        int paramCount = method.getParameterCount();
        Parameter[] parameters = method.getParameters();
        Class[] clazzArray = method.getParameterTypes();

        LOG.trace("Processing  Data -  paramCount:{} , parameters:{}, clazzArray:{} ", paramCount, parameters,
                clazzArray);
        HttpServletRequest request = ((BaseResource) context.getTarget()).getHttpRequest();
        getRequestObject(request);

        if (clazzArray != null && clazzArray.length > 0) {
            for (int i = 0; i < clazzArray.length; i++) {
                Class<?> clazz = clazzArray[i];
                String propertyName = parameters[i].getName();
                Object propertyValue = parameters[i].toString();
                LOG.trace("propertyName:{}, propertyValue:{}, clazz:{} , clazz.isPrimitive():{} ", propertyName,
                        propertyValue, clazz, clazz.isPrimitive());

                Object obj = ctxParameters[i];
                if (obj != null && (!obj.toString().toUpperCase().contains("PASSWORD")
                        || !obj.toString().toUpperCase().contains("SECRET"))) {
               

                    LOG.trace("ignoreObject(propertyName, obj, auditLogConf):{} ",
                            ignoreObject(propertyName, obj, auditLogConf));

                    AUDIT_LOG.error("{}:{}", propertyName, obj);
                }
            }
        }
    }

    private AuditLogConf getAuditLogConf() {
        return this.authUtil.getAuditLogConf();
    }

    private boolean ignoreHttpMethod(String method, AuditLogConf auditLogConf) {
        LOG.debug("Checking if method to be ignored - method:{}, auditLogConf:{}", method, auditLogConf);

        if (StringUtils.isBlank(method) || auditLogConf == null || auditLogConf.getIgnoreHttpMethod() == null
                || auditLogConf.getIgnoreHttpMethod().isEmpty()) {
            return false;
        } else if (auditLogConf.getIgnoreHttpMethod().contains(method)) {
            return true;
        }

        return false;
    }

    private boolean ignoreAnnotation(String resourceMethod, AuditLogConf auditLogConf) {
        LOG.trace("Checking if resource method to be ignored - resourceMethod:{}, auditLogConf:{}",
                resourceMethod, auditLogConf);

        if (StringUtils.isBlank(resourceMethod) || auditLogConf == null || auditLogConf.getIgnoreAnnotation() == null
                || auditLogConf.getIgnoreAnnotation().isEmpty()) {
            return false;
        } else if (auditLogConf.getIgnoreAnnotation().contains(resourceMethod)) {
            return true;
        }

        return false;
    }

    private boolean ignoreObject(String objectName, Object objectValue, AuditLogConf auditLogConf) {
        LOG.trace("Checking if object to be ignored - objectName:{}, objectValue:{}, auditLogConf:{}", objectName,
                objectValue, auditLogConf);

        if (StringUtils.isBlank(objectName) || auditLogConf == null || auditLogConf.getIgnoreObjectMapping() == null
                || auditLogConf.getIgnoreObjectMapping().isEmpty()) {
            return false;
        }

        ObjectDetails objectDetails = auditLogConf.getIgnoreObjectMapping().stream().filter(e -> (e!=null && e.getName()!=null && e.getName().equalsIgnoreCase(objectName)))
                .findFirst().orElse(null);

        if (objectDetails == null) {
            return false;
        }
        LOG.trace(
                "objectName:{}, objectValue:{}, objectDetails:{}, objectDetails.getText():{}, objectDetails.getText().contains(objectValue.toString()):{}",
                objectName, objectValue, objectDetails, objectDetails.getText(),
                objectDetails.getText().contains(objectValue.toString()));

        if (objectName.equalsIgnoreCase(objectDetails.getName())) {

            if (objectDetails.getText() == null || objectDetails.getText().isEmpty()) {
                return true;
            }

            if ((StringUtils.isNotBlank(objectValue.toString()))
                    && (objectDetails.getText() != null && objectDetails.getText().contains(objectValue.toString()))) {
                return true;
            }
        }

        return false;
    }

    private String getAction(String method) {
        String action = null;
        if (StringUtils.isNotBlank(method)) {
            switch (method) {
            case "POST":
                action = "added";
                break;
            case "PUT":
            case "PATCH":
                action = "changed";
                break;
            case "DELETE":
                action = "deleted";
                break;
            default:
                action = "fetched";
                break;
            }
        }
        return action;
    }

    private String getResource(String path) {
        LOG.trace(" path:{} ", path);
        if (StringUtils.isNotBlank(path)) {
            path = path.replace("/", "-");
        }
        return path;
    }

    public HttpServletRequest getRequestObject(HttpServletRequest request) {
        if (request == null) {
            return request;
        }

        try {
            InputStream inputStream = request.getInputStream();
            LOG.debug("inputStream.available():{}", inputStream.available());
            if (inputStream.available() > 0) {
                byte[] requestEntity = inputStream.readAllBytes();
                StringBuilder stringBuilder = new StringBuilder(new String(requestEntity)).append("\n");
                LOG.debug(stringBuilder);
            }

        } catch (Exception ex) {
            LOG.error(" Error while reading data - ", ex);
        }
        return request;
    }

}
