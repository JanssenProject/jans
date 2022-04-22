/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.external.context;

import io.jans.as.client.RegisterRequest;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.error.IErrorType;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.register.RegisterErrorResponseType;
import io.jans.as.model.util.CertUtils;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.service.cdi.util.CdiUtil;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 */
public class DynamicClientRegistrationContext extends ExternalScriptContext {

    private static final Logger log = LoggerFactory.getLogger(DynamicClientRegistrationContext.class);

    private CustomScriptConfiguration script;
    private JSONObject registerRequestJson;
    private RegisterRequest registerRequest;
    private Jwt softwareStatement;
    private Jwt dcr;
    private Client client;
    private ErrorResponseFactory errorResponseFactory;
    private X509Certificate certificate;

    public DynamicClientRegistrationContext(HttpServletRequest httpRequest, JSONObject registerRequest, CustomScriptConfiguration script) {
        this(httpRequest, registerRequest, script, null);
    }

    public DynamicClientRegistrationContext(HttpServletRequest httpRequest, JSONObject registerRequest, CustomScriptConfiguration script, Client client) {
        super(httpRequest);
        this.script = script;
        this.registerRequestJson = registerRequest;
        this.client = client;
    }

    public Jwt getDcr() {
        return dcr;
    }

    public void setDcr(Jwt dcr) {
        this.dcr = dcr;
    }

    public Jwt getSoftwareStatement() {
        return softwareStatement;
    }

    public void setSoftwareStatement(Jwt softwareStatement) {
        this.softwareStatement = softwareStatement;
    }

    public CustomScriptConfiguration getScript() {
        return script;
    }

    public void setScript(CustomScriptConfiguration script) {
        this.script = script;
    }

    public JSONObject getRegisterRequestJson() {
        return registerRequestJson;
    }

    public void setRegisterRequestJson(JSONObject registerRequestJson) {
        this.registerRequestJson = registerRequestJson;
    }

    public RegisterRequest getRegisterRequest() {
        return registerRequest;
    }

    public void setRegisterRequest(RegisterRequest registerRequest) {
        this.registerRequest = registerRequest;
    }

    public Map<String, SimpleCustomProperty> getConfigurationAttibutes() {
        final Map<String, SimpleCustomProperty> attrs = script.getConfigurationAttributes();

        if (httpRequest != null) {
            final String cert = httpRequest.getHeader("X-ClientCert");
            if (StringUtils.isNotBlank(cert)) {
                SimpleCustomProperty certProperty = new SimpleCustomProperty();
                certProperty.setValue1(cert);
                attrs.put("certProperty", certProperty);
            }
        }
        return attrs != null ? new HashMap<>(attrs) : new HashMap<>();
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public void validateSSA() {
        validateSSANotNull();
        validateSSARedirectUri();
        validateSoftwareId();
        validateCertSubjectHasCNAndOU();
        validateCNEqualsSoftwareId();
        validateOUEqualsOrgId();
        validateIssuer();
    }

    public void validateIssuer() {
        final List<String> dcrIssuers = CdiUtil.bean(AppConfiguration.class).getDcrIssuers();
        if (dcrIssuers.isEmpty()) { // nothing to check
            return;
        }

        final String issuer = softwareStatement.getClaims().getClaimAsString("iss");
        if (!dcrIssuers.contains(issuer)) {
            throwWebApplicationException("SSA Issuer is not allowed.", RegisterErrorResponseType.INVALID_CLIENT_METADATA);
        }

        final String certificateIssuer = certificate.getIssuerX500Principal().getName();
        if (!dcrIssuers.contains(certificateIssuer)) {
            throwWebApplicationException("Certificate Issuer is not allowed.", RegisterErrorResponseType.INVALID_CLIENT_METADATA);
        }
    }

    public void validateCertSubjectHasCNAndOU() {
        validateCNIsNotBlank();
        validateOUIsNotBlank();
    }

    public String validateOUIsNotBlank() {
        final String ou = CertUtils.getAttr(certificate, BCStyle.OU);
        if (StringUtils.isBlank(ou)) {
            throwWebApplicationException("OU of certificate is not set.", RegisterErrorResponseType.INVALID_CLIENT_METADATA);
        }
        return ou;
    }

    public String validateCNIsNotBlank() {
        final String cn = CertUtils.getAttr(certificate, BCStyle.CN);
        if (StringUtils.isBlank(cn)) {
            throwWebApplicationException("CN of certificate is not set.", RegisterErrorResponseType.INVALID_CLIENT_METADATA);
        }
        return cn;
    }

    public void throwWebApplicationException(String message, IErrorType errorType) {
        log.error(message);
        throwWebApplicationExceptionIfSet();
        throw createWebApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), errorResponseFactory.getErrorAsJson(errorType));
    }

    public void validateCNEqualsSoftwareId() {
        final String cn = validateCNIsNotBlank();
        final String softwareId = softwareStatement.getClaims().getClaimAsString("software_id");

        if (StringUtils.isBlank(softwareId)) {
            throwWebApplicationException("softwareId is not set in SSA", RegisterErrorResponseType.INVALID_CLIENT_METADATA);
            return;
        }

        if (cn.equals(softwareId)) // success
            return;

        throwWebApplicationException("CN does not equals to softwareId in SSA. CN: " + cn + ", softwareId: " + softwareId, RegisterErrorResponseType.INVALID_CLIENT_METADATA);
    }

    public void validateOUEqualsOrgId() {
        final String ou = validateOUIsNotBlank();
        final String orgId = softwareStatement.getClaims().getClaimAsString("org_id");

        if (StringUtils.isBlank(orgId)) {
            throwWebApplicationException("orgId is not set in SSA", RegisterErrorResponseType.INVALID_CLIENT_METADATA);
            return;
        }

        if (ou.equals(orgId)) // success
            return;

        throwWebApplicationException("OU does not equals to orgId in SSA. OU: " + ou + ", orgId: " + orgId, RegisterErrorResponseType.INVALID_CLIENT_METADATA);
    }

    public void validateSSARedirectUri() {
        validateSSARedirectUri("software_redirect_uris");
    }

    public void validateSSARedirectUri(String ssaRedirectUriClaimName) {
        if (!softwareStatement.getClaims().hasClaim(ssaRedirectUriClaimName))
            return; // skip validation, redirect_uris are not set in SSA

        final List<String> ssaRedirectUris = softwareStatement.getClaims().getClaimAsStringList(ssaRedirectUriClaimName);
        final List<String> redirectUris = registerRequest.getRedirectUris();
        if (ssaRedirectUris.containsAll(redirectUris))
            return;

        throwWebApplicationException("SSA redirect_uris does not match redirect_uris of the request. SSA redirect_uris: " + ssaRedirectUris + ", request redirectUris: " + redirectUris, RegisterErrorResponseType.INVALID_REDIRECT_URI);
    }

    public void validateSSANotNull() {
        if (softwareStatement == null) {
            throwWebApplicationException("SSA is null", RegisterErrorResponseType.INVALID_SOFTWARE_STATEMENT);
        }
    }

    public void validateSoftwareId() {
        final String softwareId = registerRequest.getSoftwareId();
        if (StringUtils.isBlank(softwareId))
            return;

        final String ssaSoftwareId = softwareStatement.getClaims().getClaimAsString("software_id");
        if (softwareId.equals(ssaSoftwareId))
            return;

        throwWebApplicationException(String.format("SSA softwareId (%s), does not match to softwareId in request (%s)", ssaSoftwareId, softwareId), RegisterErrorResponseType.INVALID_CLIENT_METADATA);
    }

    public ErrorResponseFactory getErrorResponseFactory() {
        return errorResponseFactory;
    }

    public void setErrorResponseFactory(ErrorResponseFactory errorResponseFactory) {
        this.errorResponseFactory = errorResponseFactory;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(X509Certificate certificate) {
        this.certificate = certificate;
    }

    @Override
    public String toString() {
        return "DynamicClientRegistrationContext{" +
                "softwareStatement=" + softwareStatement +
                "registerRequest=" + registerRequestJson +
                "script=" + script +
                "} " + super.toString();
    }
}
