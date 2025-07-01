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
import io.jans.configapi.util.AuthUtil;
import io.jans.configapi.core.util.Jackson;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.jsonpatch.JsonPatch;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonpatch.JsonPatchOperation;

@Interceptor
@RequestAuditInterceptor
@Priority(Interceptor.Priority.APPLICATION)
public class AuditLogInterceptor {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("audit");
    private static final Logger LOG = LoggerFactory.getLogger(AuditLogInterceptor.class);

    @Inject
    AuthUtil authUtil;

    @Inject
    ApiAppConfiguration apiAppConfiguration;

    @SuppressWarnings({ "all" })
    @AroundInvoke
    public Object aroundReadFrom(InvocationContext context) throws Exception {

        try {

            
            //return if audit disabled
            if (apiAppConfiguration.isDisableAuditLogger()) {
                AUDIT_LOG.debug("Audit is disabled by disableAuditLogger config.");
                return context.proceed();
            }
            
            //Log for audit
            HttpServletRequest request = ((BaseResource) context.getTarget()).getHttpRequest();
            HttpHeaders httpHeaders = ((BaseResource) context.getTarget()).getHttpHeaders();
            UriInfo uriInfo = ((BaseResource) context.getTarget()).getUriInfo();
            
            
            // Get Audit config
            AuditLogConf auditLogConf = getAuditLogConf();
            processRequest(context);
            // Log if enabled
            if (auditLogConf.isEnabled() && !ignoreMethod(context, auditLogConf)) {
                
                // Request audit
                String method = request.getMethod();
                String client = httpHeaders.getHeaderString("jans-client");
                String userInum = httpHeaders.getHeaderString("User-inum");
                LOG.error("Audit log method:{}, client:{}, userInum:{}", method, client, userInum);
              
               // if (StringUtils.isNotBlank(method) && !method.equals("GET") ) {
                    StringBuilder data = new StringBuilder(getResource(uriInfo.getPath()));
                  //  if(method.equals("PATCH")) { 
                        data.append("{");
                        processRequest(context);
                       // data.append(processRequest(context));
                        data.append("}");
                    //}
                        //AUDIT_LOG
                    AUDIT_LOG.info("User:{} {} {} using client:{}", userInum, getAction(method), data.toString(), client);
               // }
            }

        } catch (Exception ex) {
            LOG.error("Not able to log audit details due to error:{}", ex);
        }
        return context.proceed();
    }
    
    private void processRequest(InvocationContext context) {
        AUDIT_LOG.error("Process Audit Log Interceptor - context:{}", context);

        Object[] ctxParameters = context.getParameters();
        Method method = context.getMethod();
        int paramCount = method.getParameterCount();
        Parameter[] parameters = method.getParameters();
        Class[] clazzArray = method.getParameterTypes();

        AUDIT_LOG.error("Processing  Data -  paramCount:{} , parameters:{}, clazzArray:{} ",
                paramCount, parameters, clazzArray);

        if (clazzArray != null && clazzArray.length > 0) {
            for (int i = 0; i < clazzArray.length; i++) {
                Class<?> clazz = clazzArray[i];
                String propertyName = parameters[i].getName();
                AUDIT_LOG.error("propertyName:{}, clazz:{} , clazz.isPrimitive():{} ", propertyName, clazz,
                        clazz.isPrimitive());

                Object obj = ctxParameters[i];
                AUDIT_LOG.error(" patched String :{}",getPatchFields(obj));
                if (obj != null && (!obj.toString().toUpperCase().contains("PASSWORD")
                        || !obj.toString().toUpperCase().contains("SECRET"))) {
                    AUDIT_LOG.error("final - obj -  obj:{} ", obj);
                }

            }
        }
    }
    private String getData(InvocationContext context) {
        AUDIT_LOG.error("Process Audit Log Interceptor - context:{}", context);
        jakarta.ws.rs.Path pathAnnotation = context.getMethod().getAnnotation(jakarta.ws.rs.Path.class);
        if(pathAnnotation!=null) {
            AUDIT_LOG.error("pathAnnotation.value():{} ", pathAnnotation.value()); 
        }
        StringBuilder sb = new StringBuilder();
        Parameter[] parameters = context.getMethod().getParameters();
        AUDIT_LOG.error("parameters():{} ", parameters); 
        if (parameters != null && parameters.length > 0) {
            for (int i = 0; i < parameters.length; i++) {               
                sb.append(parameters[i].getName());
                AUDIT_LOG.error("parameters[i].getName():{} ", parameters[i].getName());
                
                if(i != parameters.length-1) {
                    sb.append(",");
                }
            }
        }
        return sb.toString();
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
        if (StringUtils.isNotBlank(path)) {
            path = path.replace("/", "-");
        }
        return path;
    }
    
    private AuditLogConf getAuditLogConf() {
        return this.authUtil.getAuditLogConf();
    }

    private boolean ignoreMethod(InvocationContext context, AuditLogConf auditLogConf) {
        LOG.debug("Checking if method to be ignored - context:{}, auditLogConf:{}", context, auditLogConf);

        if (auditLogConf == null || context.getMethod().getAnnotations() == null
                || context.getMethod().getAnnotations().length <= 0) {
            return false;
        }

        for (int i = 0; i < context.getMethod().getAnnotations().length; i++) {
            LOG.debug("Check if method is to be ignored - context.getMethod().getAnnotations()[i]:{} ",
                    context.getMethod().getAnnotations()[i]);

            if (context.getMethod().getAnnotations()[i] != null && auditLogConf.getIgnoreHttpMethod() != null
                    && auditLogConf.getIgnoreHttpMethod()
                            .contains(context.getMethod().getAnnotations()[i].toString())) {
                return true;
            }

        }
        return false;
    }
    
    private boolean toProcess() {
        boolean shouldProcess = false;
        
        return shouldProcess;
    }
    
    private String getPatchFields(Object obj) {
        StringBuilder sb = new StringBuilder();
        
        try {
            if(obj==null) {
                return sb.toString();
            }
            
            JsonNode jsonNode = Jackson.asJsonNode(Jackson.asJson(obj));
            AUDIT_LOG.error("jsonNode:{} ", jsonNode);
            AUDIT_LOG.error("jsonNode.get(path):{} ", jsonNode.get("path"));             

            
        }catch(Exception ex) {
            LOG.error("Error while processing :{}", ex);
        }
        return sb.toString();
        
    }

}
