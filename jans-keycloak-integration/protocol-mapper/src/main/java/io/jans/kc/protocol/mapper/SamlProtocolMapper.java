package io.jans.kc.protocol.mapper;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.Config;

import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;

import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.protocol.saml.mappers.AbstractSAMLProtocolMapper;
import org.keycloak.protocol.saml.mappers.SAMLAttributeStatementMapper;




public class JansSamlProtocolMapper extends AbstractSAMLProtocolMapper implements SAMLAttributeStatementMapper {
    
    private static final String DISPLAY_TYPE = "User Attribute";
    private static final String DISPLAY_CATEGORY = "User Attribute Mapper";
    private static final String HELP_TEXT = "Janssen User Attributes Protocol Mapper";
    private static final String PROVIDER_ID = "kc-jans-saml-protocol-mapper";
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    @Override
    public void close() {


    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {

        return configProperties;
    }

    @Override
    public String getId() {

        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {

        return DISPLAY_TYPE;
    }

    @Override 
    public String getDisplayCategory() {

        return DISPLAY_CATEGORY;
    }

    @Override
    public String getHelpText() {

        return HELP_TEXT;
    }

    @Override
    public void init(Config.Scope scope) {


    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {


    }

    @Override
    public void transformAttributeStatement(AttributeStatementType attributeStatement, ProtocolMapperModel mappingModel, KeycloakSession session,
                                                UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
        
        
    }
}