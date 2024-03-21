package io.jans.kc.api.config.client;

import io.jans.config.api.client.ApiClient;
import io.jans.config.api.client.SamlTrustRelationshipApi;
import io.jans.config.api.client.ApiException;
import io.jans.config.api.client.model.TrustRelationship;
import io.jans.kc.api.config.client.model.JansTrustRelationship;

import io.jans.saml.metadata.builder.SAMLMetadataBuilder;
import io.jans.saml.metadata.builder.SPSSODescriptorBuilder;
import io.jans.saml.metadata.parser.ParseError;
import io.jans.saml.metadata.model.SAMLBinding;
import io.jans.saml.metadata.model.SAMLMetadata;
import io.jans.saml.metadata.parser.SAMLMetadataParser;


import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JansConfigApi {

    private static final Logger log = LoggerFactory.getLogger(JansConfigApi.class);
    private SamlTrustRelationshipApi trApi;

    private JansConfigApi() {
        
    }

    public boolean trustRelationshipExists(String inum) {

        try {
            TrustRelationship tr = trApi.getTrustRelationshipById(inum);
            return (tr != null);
        }catch(ApiException e) {
            throw new JansConfigApiError("trustRelationshipExists() failed",e);
        }
    }

    public List<JansTrustRelationship> findAllTrustRelationships() {

        try {
            List<TrustRelationship> trlist = trApi.getTrustRelationships();
            return trlist.stream()
                .map(JansConfigApi::toJansTrustRelationship)
                .collect(Collectors.toList());
        }catch(ApiException e) {
            throw new JansConfigApiError("getAllTrustRelationships() failed",e);
        }
    }

    public SAMLMetadata getTrustRelationshipSamlMetadata(JansTrustRelationship trustrelationship) {

        if(trustrelationship.metadataIsFile()) {
            return getTrustRelationshipFileMetadata(trustrelationship);
        }else if(trustrelationship.metadataIsManual()) {
            return getTrustRelationshipManualMetadata(trustrelationship);
        }
        throw new JansConfigApiError("Unsupported Saml metadata type specified");
    }

    private SAMLMetadata getTrustRelationshipFileMetadata(JansTrustRelationship trustrelationship) {

        try {
            File samlmdfile = trApi.getTrustRelationshipFileMetadata(trustrelationship.getInum());
            log.info(samlmdfile.getAbsolutePath());
            SAMLMetadataParser parser = new SAMLMetadataParser();
            return parser.parse(samlmdfile);
        }catch(ApiException e) {
            throw new JansConfigApiError("getTrustRelationshipSamlMetadata() failed",e);
        }catch(ParseError e) {
            log.info("Parse error",e);
            throw new JansConfigApiError("SAML metadata parsing failed",e);
        }
    }

    private SAMLMetadata getTrustRelationshipManualMetadata(JansTrustRelationship trustrelationship) {

        io.jans.config.api.client.model.SAMLMetadata samlmd = trustrelationship.getManualSamlMetadata();
        if(samlmd == null) {
            throw new JansConfigApiError("Trustrelationship contains no manual metadata");
        }

        SAMLMetadataBuilder builder = new SAMLMetadataBuilder();

        SPSSODescriptorBuilder spssobuilder = builder.entityDescriptor()
            .entityId(samlmd.getEntityId())
            .spssoDescriptor();
        
        spssobuilder.authnRequestsSigned(false)
            .wantAssertionsSigned(false);

        if(samlmd.getSingleLogoutServiceUrl() != null) {
            spssobuilder.singleLogoutService()
                .binding(SAMLBinding.HTTP_REDIRECT)
                .location(samlmd.getSingleLogoutServiceUrl());
        }

        if(samlmd.getNameIDPolicyFormat() != null) {
            spssobuilder.nameIDFormats(Arrays.asList(samlmd.getNameIDPolicyFormat()));
        }

        if(samlmd.getJansAssertionConsumerServiceGetURL() != null) {
            spssobuilder.assertionConsumerService()
                .index(0)
                .isDefault(true)
                .binding(SAMLBinding.HTTP_REDIRECT)
                .location(samlmd.getJansAssertionConsumerServiceGetURL());
        }
        
        if(samlmd.getJansAssertionConsumerServicePostURL() != null) {
            spssobuilder.assertionConsumerService()
                .index(1)
                .isDefault(samlmd.getJansAssertionConsumerServiceGetURL()==null)
                .binding(SAMLBinding.HTTP_POST)
                .location(samlmd.getJansAssertionConsumerServicePostURL());
        }
        return builder.build();
    }

    public static JansConfigApi createInstance(String endpoint,ApiCredentials credentials) {

        JansConfigApi client = new JansConfigApi();
        client.trApi = newSamlTrustRelationshipApi(endpoint,credentials);
        return client;
    }

    private static SamlTrustRelationshipApi newSamlTrustRelationshipApi(String endpoint,ApiCredentials credentials) {

        SamlTrustRelationshipApi ret = new SamlTrustRelationshipApi();
        ret.setApiClient(createApiClient(endpoint,credentials));
        return ret;
    }

    private static ApiClient createApiClient(String endpoint, ApiCredentials credentials) {

        ApiClient apiclient = new ApiClient();
        apiclient.setAccessToken(credentials.bearerToken());
        apiclient.setBasePath(endpoint);
        return apiclient;
    }

    private static JansTrustRelationship toJansTrustRelationship(TrustRelationship tr) {

        JansTrustRelationship ret = new JansTrustRelationship(tr);
        return ret;
    }
}
