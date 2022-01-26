/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class OrgConfigurationResponse extends BaseResponse implements Serializable {

    private String issuer;
    private String authorizationEndpoint;
    private String tokenEndpoint;
    private String revocationEndpoint;
    private String sessionRevocationEndpoint;
    private String userInfoEndpoint;
    private String clientInfoEndpoint;
    private String checkSessionIFrame;
    private String endSessionEndpoint;
    private String jwksUri;
    private String registrationEndpoint;
    private String introspectionEndpoint;
    private String parEndpoint;
    private Boolean requirePar;
    private String deviceAuthzEndpoint;
    private List<String> scopesSupported;
    private List<String> responseTypesSupported;
    private List<String> responseModesSupported;
    private List<String> grantTypesSupported;
    private List<String> acrValuesSupported;
    private List<String> subjectTypesSupported;
    private List<String> authorizationSigningAlgValuesSupported;
    private List<String> authorizationEncryptionAlgValuesSupported;
    private List<String> authorizationEncryptionEncValuesSupported;
    private List<String> userInfoSigningAlgValuesSupported;
    private List<String> userInfoEncryptionAlgValuesSupported;
    private List<String> userInfoEncryptionEncValuesSupported;
    private List<String> idTokenSigningAlgValuesSupported;
    private List<String> idTokenEncryptionAlgValuesSupported;
    private List<String> idTokenEncryptionEncValuesSupported;
    private List<String> requestObjectSigningAlgValuesSupported;
    private List<String> requestObjectEncryptionAlgValuesSupported;
    private List<String> requestObjectEncryptionEncValuesSupported;
    private List<String> tokenEndpointAuthMethodsSupported;
    private List<String> tokenEndpointAuthSigningAlgValuesSupported;
    private List<String> dpopSigningAlgValuesSupported;
    private List<String> displayValuesSupported;
    private List<String> claimTypesSupported;
    private List<String> claimsSupported;
    private List<String> idTokenTokenBindingCnfValuesSupported;
    private String serviceDocumentation;
    private List<String> claimsLocalesSupported;
    private List<String> uiLocalesSupported;
    private Boolean claimsParameterSupported;
    private Boolean requestParameterSupported;
    private Boolean requestUriParameterSupported;
    private Boolean requireRequestUriRegistration;
    private Boolean tlsClientCertificateBoundAccessTokens;
    private Boolean frontChannelLogoutSupported;
    private Boolean frontChannelLogoutSessionSupported;
    private Boolean backchannelLogoutSupported;
    private Boolean backchannelLogoutSessionSupported;
    private String opPolicyUri;
    private String opTosUri;
    private Map<String, List<String>> scopeToClaimsMapping = new HashMap<>();
    private Map<String, Serializable> mltsAliases = new HashMap<>();

  
}
