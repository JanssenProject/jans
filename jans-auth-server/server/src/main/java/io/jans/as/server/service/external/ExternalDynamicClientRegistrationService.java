/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.external;

import io.jans.as.client.RegisterRequest;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.util.CertUtils;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.external.context.DynamicClientRegistrationContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.client.ClientRegistrationType;
import io.jans.service.custom.script.ExternalScriptService;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import jakarta.ejb.DependsOn;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import java.security.cert.X509Certificate;

/**
 * Provides factory methods needed to create external dynamic client registration extension
 *
 * @author Yuriy Movchan Date: 01/08/2015
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class ExternalDynamicClientRegistrationService extends ExternalScriptService {

    private static final long serialVersionUID = 1416361273036208688L;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    public ExternalDynamicClientRegistrationService() {
        super(CustomScriptType.CLIENT_REGISTRATION);
    }

    public boolean executeExternalCreateClientMethod(CustomScriptConfiguration customScriptConfiguration, RegisterRequest registerRequest, Client client, HttpServletRequest httpRequest) {
        try {
            log.trace("Executing python 'createClient' method");
            ClientRegistrationType externalClientRegistrationType = (ClientRegistrationType) customScriptConfiguration.getExternalType();

            DynamicClientRegistrationContext context = new DynamicClientRegistrationContext(httpRequest, null, customScriptConfiguration, client);
            context.setRegisterRequest(registerRequest);
            context.setErrorResponseFactory(errorResponseFactory);
            context.setSoftwareStatement(Jwt.parseSilently(registerRequest.getSoftwareStatement()));

            final String clientCertAsPem = httpRequest.getHeader("X-ClientCert");
            if (StringUtils.isNotBlank(clientCertAsPem)) {
                context.setCertificate(CertUtils.x509CertificateFromPem(clientCertAsPem));
            } else {
                log.trace("Cert is not set for client registration. X-ClientCert header has no value.");
            }

            final boolean result = externalClientRegistrationType.createClient(context);
            context.throwWebApplicationExceptionIfSet();
            return result;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(customScriptConfiguration.getCustomScript(), ex);
        }

        return false;
    }

    public boolean executeExternalCreateClientMethods(RegisterRequest registerRequest, Client client, HttpServletRequest httpRequest) {
        boolean result = true;
        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (customScriptConfiguration.getExternalType().getApiVersion() > 1) {
                result &= executeExternalCreateClientMethod(customScriptConfiguration, registerRequest, client, httpRequest);
                if (!result) {
                    return result;
                }
            }
        }

        return result;
    }

    public boolean executeExternalUpdateClientMethod(HttpServletRequest httpRequest, CustomScriptConfiguration script, RegisterRequest registerRequest, Client client) {
        try {
            log.trace("Executing python 'updateClient' method");
            ClientRegistrationType externalClientRegistrationType = (ClientRegistrationType) script.getExternalType();

            DynamicClientRegistrationContext context = new DynamicClientRegistrationContext(httpRequest, null, script, client);
            context.setRegisterRequest(registerRequest);
            context.setSoftwareStatement(Jwt.parseSilently(registerRequest.getSoftwareStatement()));
            context.setErrorResponseFactory(errorResponseFactory);

            final boolean result = externalClientRegistrationType.updateClient(context);
            context.throwWebApplicationExceptionIfSet();
            return result;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(script.getCustomScript(), ex);
        }

        return false;
    }

    public boolean executeExternalUpdateClientMethods(HttpServletRequest httpRequest, RegisterRequest registerRequest, Client client) {
        boolean result = true;
        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            result &= executeExternalUpdateClientMethod(httpRequest, customScriptConfiguration, registerRequest, client);
            if (!result) {
                return result;
            }
        }

        return result;
    }

    public JSONObject getSoftwareStatementJwks(HttpServletRequest httpRequest, JSONObject registerRequest, Jwt softwareStatement) {
        if (defaultExternalCustomScript == null) {
            return null;
        }

        try {
            log.info("Executing python 'getSoftwareStatementJwks' method, script name: {}", defaultExternalCustomScript.getName());

            DynamicClientRegistrationContext context = new DynamicClientRegistrationContext(httpRequest, registerRequest, defaultExternalCustomScript);
            context.setSoftwareStatement(softwareStatement);
            context.setErrorResponseFactory(errorResponseFactory);

            ClientRegistrationType externalType = (ClientRegistrationType) defaultExternalCustomScript.getExternalType();
            final String result = externalType.getSoftwareStatementJwks(context);
            context.throwWebApplicationExceptionIfSet();
            log.info("Result of python 'getSoftwareStatementJwks' method: {}", result);
            return new JSONObject(result);
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(defaultExternalCustomScript.getCustomScript(), ex);
            return null;
        }
    }

    public String getSoftwareStatementHmacSecret(HttpServletRequest httpRequest, JSONObject registerRequest, Jwt softwareStatement) {
        if (defaultExternalCustomScript == null) {
            return "";
        }

        try {
            log.trace("Executing python 'getSoftwareStatementHmacSecret' method");

            DynamicClientRegistrationContext context = new DynamicClientRegistrationContext(httpRequest, registerRequest, defaultExternalCustomScript);
            context.setSoftwareStatement(softwareStatement);
            context.setErrorResponseFactory(errorResponseFactory);

            ClientRegistrationType externalType = (ClientRegistrationType) defaultExternalCustomScript.getExternalType();
            final String result = externalType.getSoftwareStatementHmacSecret(context);
            context.throwWebApplicationExceptionIfSet();
            log.trace("Result of python 'getSoftwareStatementHmacSecret' method: {}", result);
            return result;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(defaultExternalCustomScript.getCustomScript(), ex);
            return "";
        }
    }

    public JSONObject getDcrJwks(HttpServletRequest httpRequest, Jwt dcr) {
        if (defaultExternalCustomScript == null) {
            return null;
        }

        try {
            log.trace("Executing python 'getDcrJwks' method");

            DynamicClientRegistrationContext context = new DynamicClientRegistrationContext(httpRequest, null, defaultExternalCustomScript);
            context.setDcr(dcr);
            context.setErrorResponseFactory(errorResponseFactory);

            ClientRegistrationType externalType = (ClientRegistrationType) defaultExternalCustomScript.getExternalType();
            final String result = externalType.getDcrJwks(context);
            context.throwWebApplicationExceptionIfSet();
            log.trace("Result of python 'getDcrJwks' method: {}", result);
            return new JSONObject(result);
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(defaultExternalCustomScript.getCustomScript(), ex);
            return null;
        }
    }

    public String getDcrHmacSecret(HttpServletRequest httpRequest, Jwt dcr) {
        if (defaultExternalCustomScript == null) {
            return "";
        }

        try {
            log.trace("Executing python 'getDcrHmacSecret' method");

            DynamicClientRegistrationContext context = new DynamicClientRegistrationContext(httpRequest, null, defaultExternalCustomScript);
            context.setDcr(dcr);
            context.setErrorResponseFactory(errorResponseFactory);

            ClientRegistrationType externalType = (ClientRegistrationType) defaultExternalCustomScript.getExternalType();
            final String result = externalType.getDcrHmacSecret(context);
            context.throwWebApplicationExceptionIfSet();
            log.trace("Result of python 'getDcrHmacSecret' method: {}", result);
            return result;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(defaultExternalCustomScript.getCustomScript(), ex);
            return "";
        }
    }

    public boolean isCertValidForClient(X509Certificate cert, DynamicClientRegistrationContext context) {
        if (defaultExternalCustomScript == null) {
            return true;
        }

        try {
            log.trace("Executing python 'isCertValidForClient' method");
            context.setScript(defaultExternalCustomScript);
            context.setErrorResponseFactory(errorResponseFactory);
            ClientRegistrationType externalType = (ClientRegistrationType) defaultExternalCustomScript.getExternalType();
            final boolean result = externalType.isCertValidForClient(cert, context);
            context.throwWebApplicationExceptionIfSet();
            log.trace("Result of python 'isCertValidForClient' method: {}", result);
            return result;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(defaultExternalCustomScript.getCustomScript(), ex);
            return false;
        }
    }

    public boolean modifyPostResponse(JSONObject responseAsJsonObject, ExecutionContext context) {
        if (defaultExternalCustomScript == null) {
            return false;
        }

        CustomScriptConfiguration script = defaultExternalCustomScript;

        try {
            if (log.isTraceEnabled()) {
                log.trace("Executing python 'modifyPostResponse' method, script name: {}, context: {}, response: {}", script.getName(), context, responseAsJsonObject.toString());
            }
            context.setScript(script);

            ClientRegistrationType type = (ClientRegistrationType) script.getExternalType();
            final boolean result = type.modifyPostResponse(responseAsJsonObject, context);
            if (log.isTraceEnabled()) {
                log.trace("Finished 'modifyPostResponse' method, script name: {}, context: {}, result: {}, response: {}", script.getName(), context, result, responseAsJsonObject.toString());
            }

            return result;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(script.getCustomScript(), ex);
        }
        return false;
    }

    public boolean modifyPutResponse(JSONObject responseAsJsonObject, ExecutionContext context) {
        if (defaultExternalCustomScript == null) {
            return false;
        }

        CustomScriptConfiguration script = defaultExternalCustomScript;

        try {
            if (log.isTraceEnabled()) {
                log.trace("Executing python 'modifyPutResponse' method, script name: {}, context: {}, response: {}", script.getName(), context, responseAsJsonObject.toString());
            }
            context.setScript(script);

            ClientRegistrationType type = (ClientRegistrationType) script.getExternalType();
            final boolean result = type.modifyPutResponse(responseAsJsonObject, context);
            if (log.isTraceEnabled()) {
                log.trace("Finished 'modifyPutResponse' method, script name: {}, context: {}, result: {}, response: {}", script.getName(), context, result, responseAsJsonObject.toString());
            }

            return result;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(script.getCustomScript(), ex);
        }
        return false;
    }

    public boolean modifyReadResponse(JSONObject responseAsJsonObject, ExecutionContext context) {
        if (defaultExternalCustomScript == null) {
            return false;
        }

        CustomScriptConfiguration script = defaultExternalCustomScript;

        try {
            if (log.isTraceEnabled()) {
                log.trace("Executing python 'modifyReadResponse' method, script name: {}, context: {}, response: {}", script.getName(), context, responseAsJsonObject.toString());
            }
            context.setScript(script);

            ClientRegistrationType type = (ClientRegistrationType) script.getExternalType();
            final boolean result = type.modifyReadResponse(responseAsJsonObject, context);
            if (log.isTraceEnabled()) {
                log.trace("Finished 'modifyReadResponse' method, script name: {}, context: {}, result: {}, response: {}", script.getName(), context, result, responseAsJsonObject.toString());
            }

            return result;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(script.getCustomScript(), ex);
        }
        return false;
    }
}
