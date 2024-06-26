package io.jans.kc.spi.protocol.mapper.saml;

import io.jans.kc.model.JansUserAttributeModel;
import io.jans.kc.spi.ProviderIDs;
import io.jans.kc.spi.custom.JansThinBridgeOperationException;
import io.jans.kc.spi.custom.JansThinBridgeProvider;

import java.util.List;

import org.jboss.logging.Logger;

import org.keycloak.Config;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;

import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;

import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.mappers.AbstractSAMLProtocolMapper;
import org.keycloak.protocol.saml.mappers.SAMLAttributeStatementMapper;

public class JansSamlUserAttributeMapper extends AbstractSAMLProtocolMapper implements SAMLAttributeStatementMapper {
    
    private static final String DISPLAY_TYPE = "Janssen User Attribute";
    private static final String DISPLAY_CATEGORY = AttributeStatementHelper.ATTRIBUTE_STATEMENT_CATEGORY;
    private static final String HELP_TEXT = "Maps a Janssen User's Attribute to a SAML Attribute";

    private static final String PROVIDER_ID = ProviderIDs.JANS_SAML_USER_ATTRIBUTE_MAPPER_PROVIDER;
    //properties 
    private static final String JANS_ATTR_NAME_PROP_NAME = "jans.attribute.name";
    private static final String JANS_ATTR_NAME_PROP_LABEL = "Jans Attribute";
    private static final String JANS_ATTR_NAME_PROP_HELPTEXT = "Name of the Attribute in Janssen Auth Server";
    private static final List<ProviderConfigProperty> configProperties;

    private static final Logger log = Logger.getLogger(JansSamlUserAttributeMapper.class);

    static {
        configProperties = ProviderConfigurationBuilder.create()
            .property()
                .name(JANS_ATTR_NAME_PROP_NAME)
                .label(JANS_ATTR_NAME_PROP_LABEL)
                .helpText(JANS_ATTR_NAME_PROP_HELPTEXT)
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(null)
                .required(true)
                .add()
            .build();
    }

    @Override
    public void init(Config.Scope scope) {
        //nothing for now to do in init 
    }

    @Override
    public void close() {
        //nothing for now to do in close
    }

    @Override 
    public void postInit(KeycloakSessionFactory factory) {
        //nothing to do for now in postInit
    }

    @Override
    public void transformAttributeStatement(AttributeStatementType attributeStatement, ProtocolMapperModel mappingModel, KeycloakSession session,
        UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
        
        try {
            final JansThinBridgeProvider jansThinBridge = session.getProvider(JansThinBridgeProvider.class);
            final String attributeName = mappingModel.getConfig().get(JANS_ATTR_NAME_PROP_NAME);
            final String loginUsername = userSession.getLoginUsername();
            final JansUserAttributeModel userAttribute = jansThinBridge.getUserAttribute(loginUsername,attributeName);
            if(userAttribute == null) {
                log.info("Could not find jans attribute information for user " + loginUsername);
                return;
            }
            if(!userAttribute.isActive()) {
                log.info("Jans attribute " + attributeName + " is not active");
                return;
            }
            AttributeType keycloakAttribute = userAttribute.asSamlKeycloakAttribute();
            if(keycloakAttribute == null) {
                log.info("Could not convert jans attribute " + attributeName + " into a keycloak attribute");
                return;
            }
            attributeStatement.addAttribute(new AttributeStatementType.ASTChoiceType(keycloakAttribute));
        }catch(JansThinBridgeOperationException e) {
            log.error("Error mapping saml attribute from jans",e);
        }
        
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

    
}
