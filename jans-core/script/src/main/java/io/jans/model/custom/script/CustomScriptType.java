/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script;

import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.model.auth.AuthenticationCustomScript;
import io.jans.model.custom.script.type.BaseExternalType;
import io.jans.model.custom.script.type.auth.DummyPersonAuthenticationType;
import io.jans.model.custom.script.type.auth.PersonAuthenticationType;
import io.jans.model.custom.script.type.authz.ConsentGatheringType;
import io.jans.model.custom.script.type.authz.DummyConsentGatheringType;
import io.jans.model.custom.script.type.authzchallenge.AuthorizationChallengeType;
import io.jans.model.custom.script.type.authzchallenge.DummyAuthorizationChallengeType;
import io.jans.model.custom.script.type.authzdetails.AuthzDetailType;
import io.jans.model.custom.script.type.authzdetails.DummyAuthzDetail;
import io.jans.model.custom.script.type.authzen.AccessEvaluationType;
import io.jans.model.custom.script.type.authzen.DummyAccessEvaluationType;
import io.jans.model.custom.script.type.ciba.DummyEndUserNotificationType;
import io.jans.model.custom.script.type.ciba.EndUserNotificationType;
import io.jans.model.custom.script.type.client.ClientAuthnType;
import io.jans.model.custom.script.type.client.ClientRegistrationType;
import io.jans.model.custom.script.type.client.DummyClientAuthnType;
import io.jans.model.custom.script.type.client.DummyClientRegistrationType;
import io.jans.model.custom.script.type.configapi.ConfigApiType;
import io.jans.model.custom.script.type.configapi.DummyConfigApiType;
import io.jans.model.custom.script.type.createuser.CreateUserType;
import io.jans.model.custom.script.type.createuser.DummyCreateUserType;
import io.jans.model.custom.script.type.discovery.DiscoveryType;
import io.jans.model.custom.script.type.discovery.DummyDiscoveryType;
import io.jans.model.custom.script.type.fido2.DummyFido2ExtensionType;
import io.jans.model.custom.script.type.fido2.Fido2ExtensionType;
import io.jans.model.custom.script.type.health.DummyHealthCheck;
import io.jans.model.custom.script.type.health.HealthCheckType;
import io.jans.model.custom.script.type.id.DummyIdGeneratorType;
import io.jans.model.custom.script.type.id.IdGeneratorType;
import io.jans.model.custom.script.type.idp.DummyIdpType;
import io.jans.model.custom.script.type.idp.IdpType;
import io.jans.model.custom.script.type.introspection.DummyIntrospectionType;
import io.jans.model.custom.script.type.introspection.IntrospectionType;
import io.jans.model.custom.script.type.lock.DummyLockExtensionType;
import io.jans.model.custom.script.type.lock.LockExtensionType;
import io.jans.model.custom.script.type.logout.DummyEndSessionType;
import io.jans.model.custom.script.type.logout.EndSessionType;
import io.jans.model.custom.script.type.owner.DummyResourceOwnerPasswordCredentialsType;
import io.jans.model.custom.script.type.owner.ResourceOwnerPasswordCredentialsType;
import io.jans.model.custom.script.type.persistence.DummyPeristenceType;
import io.jans.model.custom.script.type.persistence.PersistenceType;
import io.jans.model.custom.script.type.postauthn.DummyPostAuthnType;
import io.jans.model.custom.script.type.postauthn.PostAuthnType;
import io.jans.model.custom.script.type.revoke.DummyRevokeTokenType;
import io.jans.model.custom.script.type.revoke.RevokeTokenType;
import io.jans.model.custom.script.type.scim.DummyScimType;
import io.jans.model.custom.script.type.scim.ScimType;
import io.jans.model.custom.script.type.scope.DummyDynamicScopeType;
import io.jans.model.custom.script.type.scope.DynamicScopeType;
import io.jans.model.custom.script.type.selectaccount.DummySelectAccountType;
import io.jans.model.custom.script.type.selectaccount.SelectAccountType;
import io.jans.model.custom.script.type.session.ApplicationSessionType;
import io.jans.model.custom.script.type.session.DummyApplicationSessionType;
import io.jans.model.custom.script.type.spontaneous.DummySpontaneousScopeType;
import io.jans.model.custom.script.type.spontaneous.SpontaneousScopeType;
import io.jans.model.custom.script.type.ssa.DummyModifySsaResponseType;
import io.jans.model.custom.script.type.ssa.ModifySsaResponseType;
import io.jans.model.custom.script.type.token.DummyUpdateTokenType;
import io.jans.model.custom.script.type.token.UpdateTokenType;
import io.jans.model.custom.script.type.uma.*;
import io.jans.model.custom.script.type.user.CacheRefreshType;
import io.jans.model.custom.script.type.user.DummyCacheRefreshType;
import io.jans.orm.annotation.AttributeEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * List of supported custom scripts
 *
 * @author Yuriy Movchan Date: 11/11/2014
 */
public enum CustomScriptType implements AttributeEnum {

    PERSON_AUTHENTICATION("person_authentication", "Person Authentication", PersonAuthenticationType.class, AuthenticationCustomScript.class,
            "PersonAuthentication", new DummyPersonAuthenticationType()),
    AUTHORIZATION_CHALLENGE("authorization_challenge", "Authorization Challenge", AuthorizationChallengeType.class, CustomScript.class, "AuthorizationChallenge", new DummyAuthorizationChallengeType()),
    INTROSPECTION("introspection", "Introspection", IntrospectionType.class, CustomScript.class, "Introspection", new DummyIntrospectionType()),
    RESOURCE_OWNER_PASSWORD_CREDENTIALS("resource_owner_password_credentials", "Resource Owner Password Credentials", ResourceOwnerPasswordCredentialsType.class, CustomScript.class, "ResourceOwnerPasswordCredentials", new DummyResourceOwnerPasswordCredentialsType()),
    APPLICATION_SESSION("application_session", "Application Session", ApplicationSessionType.class, CustomScript.class, "ApplicationSession",
            new DummyApplicationSessionType()),
    CACHE_REFRESH("cache_refresh", "Cache Refresh", CacheRefreshType.class, CustomScript.class, "CacheRefresh",
            new DummyCacheRefreshType()),
    CLIENT_REGISTRATION("client_registration", "Client Registration", ClientRegistrationType.class, CustomScript.class, "ClientRegistration",
            new DummyClientRegistrationType()),
    ID_GENERATOR("id_generator", "Id Generator", IdGeneratorType.class, CustomScript.class, "IdGenerator",
            new DummyIdGeneratorType()),
    UMA_RPT_POLICY("uma_rpt_policy", "UMA RPT Policies", UmaRptPolicyType.class, CustomScript.class, "UmaRptPolicy",
            new UmaDummyRptPolicyType()),
    UMA_RPT_CLAIMS("uma_rpt_claims", "UMA RPT Claims", UmaRptClaimsType.class, CustomScript.class, "UmaRptClaims", new UmaDummyRptClaimsType()),
    UMA_CLAIMS_GATHERING("uma_claims_gathering", "UMA Claims Gathering", UmaClaimsGatheringType.class, CustomScript.class, "UmaClaimsGathering",
            new UmaDummyClaimsGatheringType()),
    ACCESS_EVALUATION("access_evaluation", "Access Evaluation", AccessEvaluationType.class, CustomScript.class, "AccessEvaluation",
            new DummyAccessEvaluationType()),
    CONSENT_GATHERING("consent_gathering", "Consent Gathering", ConsentGatheringType.class, CustomScript.class, "ConsentGathering",
            new DummyConsentGatheringType()),
    DYNAMIC_SCOPE("dynamic_scope", "Dynamic Scopes", DynamicScopeType.class, CustomScript.class, "DynamicScope",
            new DummyDynamicScopeType()),
    SPONTANEOUS_SCOPE("spontaneous_scope", "Spontaneous Scopes", SpontaneousScopeType.class, CustomScript.class, "SpontaneousScope", new DummySpontaneousScopeType()),
    END_SESSION("end_session", "End Session", EndSessionType.class, CustomScript.class, "EndSession", new DummyEndSessionType()),
    POST_AUTHN("post_authn", "Post Authentication", PostAuthnType.class, CustomScript.class, "PostAuthn", new DummyPostAuthnType()),
    CLIENT_AUTHN("client_authn", "Client Authentication", ClientAuthnType.class, CustomScript.class, "ClientAuthn", new DummyClientAuthnType()),
    SELECT_ACCOUNT("select_account", "Select Account", SelectAccountType.class, CustomScript.class, "SelectAccount", new DummySelectAccountType()),
    CREATE_USER("create_user", "Create User", CreateUserType.class, CustomScript.class, "CreateUser", new DummyCreateUserType()),
    SCIM("scim", "SCIM", ScimType.class, CustomScript.class, "ScimEventHandler", new DummyScimType()),
    CIBA_END_USER_NOTIFICATION("ciba_end_user_notification", "CIBA End User Notification", EndUserNotificationType.class,
            CustomScript.class, "EndUserNotification", new DummyEndUserNotificationType()),
    REVOKE_TOKEN("revoke_token", "Revoke Token", RevokeTokenType.class, CustomScript.class, "RevokeToken", new DummyRevokeTokenType()),
    PERSISTENCE_EXTENSION("persistence_extension", "Persistence Extension", PersistenceType.class, CustomScript.class, "PersistenceExtension", new DummyPeristenceType()),
    IDP("idp", "Idp Extension", IdpType.class, CustomScript.class, "IdpExtension", new DummyIdpType()),
    DISCOVERY("discovery", "Discovery", DiscoveryType.class, CustomScript.class, "Discovery", new DummyDiscoveryType()),
    HEALTH_CHECK("health_check", "Health Check", HealthCheckType.class, CustomScript.class, "HealthCheck", new DummyHealthCheck()),
    AUTHZ_DETAIL("authz_detail", "Authorization Detail", AuthzDetailType.class, CustomScript.class, "AuthzDetail", new DummyAuthzDetail()),
    UPDATE_TOKEN("update_token", "Update Token", UpdateTokenType.class, CustomScript.class, "UpdateToken", new DummyUpdateTokenType()),
    CONFIG_API("config_api_auth", "Config Api Auth", ConfigApiType.class, CustomScript.class,"ConfigApiAuthorization", new DummyConfigApiType()),
    MODIFY_SSA_RESPONSE("modify_ssa_response", "Modify SSA Response", ModifySsaResponseType.class, CustomScript.class, "ModifySsaResponse", new DummyModifySsaResponseType()),
    FIDO2_EXTENSION("fido2_extension", "Fido2 Extension", Fido2ExtensionType.class, CustomScript.class, "Fido2Extension", new DummyFido2ExtensionType()),
    LOCK_EXTENSION("lock_extension", "Lock Extension", LockExtensionType.class, CustomScript.class, "LockExtension", new DummyLockExtensionType()),
    ;

    private String value;
    private String displayName;
    private Class<? extends BaseExternalType> customScriptType;
    private Class<? extends CustomScript> customScriptModel;
    private String className;
    private BaseExternalType defaultImplementation;

    private static final Map<String, CustomScriptType> MAP_BY_VALUES = new HashMap<>();

    static {
        for (CustomScriptType enumType : values()) {
            MAP_BY_VALUES.put(enumType.getValue(), enumType);
        }
    }

    CustomScriptType(String value, String displayName, Class<? extends BaseExternalType> customScriptType,
                     Class<? extends CustomScript> customScriptModel, String className, BaseExternalType defaultImplementation) {
        this.displayName = displayName;
        this.value = value;
        this.customScriptType = customScriptType;
        this.customScriptModel = customScriptModel;
        this.className = className;
        this.defaultImplementation = defaultImplementation;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public Class<? extends BaseExternalType> getCustomScriptType() {
        return customScriptType;
    }

    public Class<? extends CustomScript> getCustomScriptModel() {
        return customScriptModel;
    }

    public String getClassName() {
        return className;
    }

    public BaseExternalType getDefaultImplementation() {
        return defaultImplementation;
    }

    public static CustomScriptType getByValue(String value) {
        return MAP_BY_VALUES.get(value);
    }

    public Enum<? extends AttributeEnum> resolveByValue(String value) {
        return getByValue(value);
    }

    @Override
    public String toString() {
        return value;
    }

}
