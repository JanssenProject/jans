package io.jans.kc.api.admin.client.model;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.representations.idm.ClientRepresentation;

public class ManagedSamlClient {
    
    public enum AuthenticatorType {
        CLIENT_SECRET("client-secret"),
        CLIENT_JWT("client-jwt");

        private final String value;

        private AuthenticatorType(final String value) {
            this.value = value;
        }

        public String value() {

            return this.value;
        }
    }

    public enum Protocol {
        SAML("saml"),
        OPENID("openid");

        private final String value;
        private Protocol(final String value) {
            this.value = value;
        }

        public String value() {

            return this.value;
        }
    }

    public enum AuthnFlowOverrideType {
        BROWSER("browser"),
        DIRECT_GRANT("direct-grant");

        private final String value;

        private AuthnFlowOverrideType(final String value){
            this.value = value;
        }

        public String value() {

            return this.value;
        }
    }

    private String externalRef;
    private ClientRepresentation clientRepresentation;
    private final Map<String,String> authnFlowBindingOverrides = new HashMap<String,String>();
    private final Map<String,String> attributes = new HashMap<String,String>();
    
    //saml client attributes

    // sign assertions (should assertions inside saml documents be signed? not needed if the document is already signed)
    //default: "false"
    private static final String ATTR_SAML_ASSERTION_SIGNATURE = "saml.assertion.signature";
    //saml client encryption private key (seems not useful in our context)
    //default: ""
    private static final String ATTR_SAML_ENCRYPTION_PRIVATE_KEY = "saml.encryption.private.key";
    //always use POST binding for responses
    //default: "false"
    private static final String ATTR_SAML_FORCE_POST_BINDING = "saml.force.post.binding";
    //encrypt assertions (saml assertions will be encrypted with the client's key)
    //default: "false"
    private static final String ATTR_SAML_ENCRYPT = "saml.encrypt";
    //post logout redirect uris
    //a list of uris delimited by ## 
    //default: ""
    private static final String ATTR_POST_LOGOUT_REDIRECT_URIS = "post.logout.redirect.uris"; 
    // sign documents (should documents be signed by the realm?)
    //default: "false"
    private static final String ATTR_SAML_SERVER_SIGNATURE = "saml.server.signature";

    //unsure what this does
    //default: "false"
    private static final String ATTR_SAML_SERVER_SIGNATURE_KEYINFO_EXT = "saml.server.signature.keyinfo.ext";
    //saml client signing certificate 
    //default: ""
    private static final String ATTR_SAML_SIGNING_CERTIFICATE = "saml.signing.certificate";
    //unsure what this does 
    //default: unknown 
    private static final String ATTR_SAML_ARTIFACT_BINDING_ID = "saml.artifact.binding.identifier";
    //force artifact binding
    //default: "false"
    private static final String ATTR_SAML_ARTIFACT_BINDING = "saml.artifact.binding";
    //idp side signature algorithm
    //default: "RSA_SHA256"
    private static final String ATTR_SAML_SIGNATURE_ALGORITHM = "saml.signature.algorithm";
    //ignore requested nameid format and use the one configured in keycloak (ui)
    //default: "false"
    private static final String ATTR_SAML_FORCE_NAME_ID_FORMAT = "saml.force_name_id_format";
    //client signature required (if clients signs their saml requests and responses and should be validated)
    //default: "false"
    private static final String ATTR_SAML_CLIENT_SIGNATURE = "saml.client.signature";
    //saml client public key used for encryption 
    //default: ""
    private static final String ATTR_SAML_ENCRYPTION_CERTIFICATE = "saml.encryption.certificate";
    //include authnstatement
    //default: "true"
    private static final String ATTR_SAML_AUTHNSTATEMENT = "saml.authnstatement";
    //nameid formats (supported values are username, email,persistent,transient,unspecified)
    //default: "unspecified"
    private static final String ATTR_SAML_NAMEID_FORMAT = "saml_name_id_format";
    //saml client private key used for signing (not sure it's used somewhere)
    //default: ""
    private static final String ATTR_SAML_SIGNING_PRIVATE_KEY = "saml.signing.private.key";
    //allow ecp flow
    //default: "false"
    private static final String ATTR_SAML_ALLOW_ECP_FLOW = "saml.allow.ecp.flow";
    //canonicalization method for xml signatures
    //default: "http://www.w3.org/2001/10/xml-exc-c14n#"
    private static final String ATTR_SAML_SIGNATURE_C14N_METHOD = "saml_signature_canonicalization_method";
    //should a OneTimeUse condition be included in the login response ? 
    //default: "false"
    private static final String ATTR_SAML_ONETIMEUSE_CONDITION = "saml.onetimeuse.condition";

    public ManagedSamlClient(String externalRef) {

        this.externalRef = externalRef;
        clientRepresentation = new ClientRepresentation();
        initClientRepresentation();
    }

    public ManagedSamlClient(ClientRepresentation clientRepresentation, String externalRef) {

        this.clientRepresentation = clientRepresentation;
        this.externalRef = externalRef;
        initClientRepresentation();
    }

    private void initClientRepresentation() {

        if(clientRepresentation != null) {
            clientRepresentation.setEnabled(true);
            clientRepresentation.setProtocol(Protocol.SAML.value());
            clientRepresentation.setAlwaysDisplayInConsole(false);
            clientRepresentation.setClientAuthenticatorType(AuthenticatorType.CLIENT_JWT.value());
            clientRepresentation.setConsentRequired(false);
            clientRepresentation.setAttributes(attributes);
            clientRepresentation.setAuthenticationFlowBindingOverrides(authnFlowBindingOverrides);

            //set default saml attributes
            samlShoulDocumentsBeSigned(true);
            samlSignAssertions(true);
            samlForcePostBinding(false);
            samlEncryptAssertions(false);
            samlForceArtifactBinding(false);
            samlSignatureAlgorithm(SamlSignatureAlgorithm.RSA_SHA256);
            samlForceNameIdFormat(false);
            samlClientSignatureRequired(false);
            samlIncludeAuthnStatement(true);
            samlNameIDFormat(SamlNameIDFormat.USERNAME);
            samlAllowEcpFLow(false);
            samlXmlSignatureCanonicalizationMethod("http://www.w3.org/2001/10/xml-exc-c14n#");
            samlIncludeOneTimeUseCondition(false);
        }
    }

    public String externalRef() {

        return this.externalRef;
    }

    public ClientRepresentation clientRepresentation() {

        return this.clientRepresentation;
    }

    public boolean correspondsToExternalRef(String externalRef) {

        return this.externalRef.equals(externalRef);
    }

    public String keycloakId() {

        return this.clientRepresentation.getId();
    }

    public ManagedSamlClient setKeycloakId(String keycloakid) {

        this.clientRepresentation.setId(keycloakid);
        return this;
    }

    public ManagedSamlClient setName(final String name) {

        this.clientRepresentation.setName(name);
        return this;
    }

    public ManagedSamlClient setDescription(final String description) {

        this.clientRepresentation.setDescription(description);
        return this;
    }

    public ManagedSamlClient setClientId(final String clientid) {

        this.clientRepresentation.setClientId(clientid);
        return this;
    }

    public String clientId() {

        return this.clientRepresentation.getClientId();
    }

    public ManagedSamlClient setEnabled(final Boolean enabled) {

        this.clientRepresentation.setEnabled(enabled);
        return this;
    }

    public ManagedSamlClient setBrowserFlow(final String browserflowid) {

        this.authnFlowBindingOverrides.put(AuthnFlowOverrideType.BROWSER.value(),browserflowid);
        return this;
    }

    public ManagedSamlClient setDirectGrantFlow(final String directgrantflowid) {

        this.authnFlowBindingOverrides.put(AuthnFlowOverrideType.DIRECT_GRANT.value(),directgrantflowid);
        return this;
    }

    public ManagedSamlClient setSamlRedirectUris(List<String> uris) {

        clientRepresentation.setRedirectUris(uris);
        return this;
    }

    public ManagedSamlClient samlSignAssertions(final Boolean sign) {

        attributes.put(ATTR_SAML_ASSERTION_SIGNATURE,sign.toString());
        return this;
    }

    public ManagedSamlClient samlEncryptionPrivateKey(final String privatekey) {

        attributes.put(ATTR_SAML_ENCRYPTION_PRIVATE_KEY,privatekey);
        return this;
    }

    public ManagedSamlClient samlForcePostBinding(final Boolean forcepostbinding) {

        attributes.put(ATTR_SAML_FORCE_POST_BINDING,forcepostbinding.toString());
        return this;
    }

    public ManagedSamlClient samlEncryptAssertions(final Boolean encryptassertions) {

        attributes.put(ATTR_SAML_ENCRYPT,encryptassertions.toString());
        return this;
    }

    public ManagedSamlClient samlPostLogoutRedirectUrls(final List<String> urls) {

        attributes.put(ATTR_POST_LOGOUT_REDIRECT_URIS,String.join("##",urls));
        return this;
    }

    public ManagedSamlClient samlShoulDocumentsBeSigned(final Boolean signed) {

        attributes.put(ATTR_SAML_SERVER_SIGNATURE,signed.toString());
        return this;
    }

    public ManagedSamlClient samlClientSigningCertificate(final String certificate) {

        attributes.put(ATTR_SAML_SIGNING_CERTIFICATE,certificate);
        return this;
    }

    public ManagedSamlClient samlForceArtifactBinding(final Boolean force) {

        attributes.put(ATTR_SAML_ARTIFACT_BINDING,force.toString());
        return this;
    }

    public ManagedSamlClient samlSignatureAlgorithm(final SamlSignatureAlgorithm algorithm) {

        attributes.put(ATTR_SAML_SIGNATURE_ALGORITHM,algorithm.value());
        return this;
    }

    public ManagedSamlClient samlForceNameIdFormat(final Boolean force) {

        attributes.put(ATTR_SAML_FORCE_NAME_ID_FORMAT,force.toString());
        return this;
    }

    public ManagedSamlClient samlClientSignatureRequired(final Boolean required) {

        attributes.put(ATTR_SAML_CLIENT_SIGNATURE,required.toString());
        return this;
    }

    public ManagedSamlClient samlClientEncryptionCertificate(final String certificate) {

        attributes.put(ATTR_SAML_ENCRYPTION_CERTIFICATE,certificate);
        return this;
    }

    public ManagedSamlClient samlIncludeAuthnStatement(final Boolean include) {
        
        attributes.put(ATTR_SAML_AUTHNSTATEMENT,include.toString());
        return this;
    }

    public ManagedSamlClient samlNameIDFormat(final SamlNameIDFormat format) {

        attributes.put(ATTR_SAML_NAMEID_FORMAT,format.value());
        return this;
    }

    public ManagedSamlClient samlClientPrivateKey(final String privatekey) {

        attributes.put(ATTR_SAML_SIGNING_PRIVATE_KEY,privatekey);
        return this;
    }

    public ManagedSamlClient samlAllowEcpFLow(final Boolean allow) {

        attributes.put(ATTR_SAML_ALLOW_ECP_FLOW,allow.toString());
        return this;
    }

    public ManagedSamlClient samlXmlSignatureCanonicalizationMethod(final String method) {

        attributes.put(ATTR_SAML_SIGNATURE_C14N_METHOD,method);
        return this;
    }

    public ManagedSamlClient samlIncludeOneTimeUseCondition(final Boolean include) {

        attributes.put(ATTR_SAML_ONETIMEUSE_CONDITION,include.toString());
        return this;
    }
}
