package io.jans.kc.protocol.mapper;

import java.util.ArrayList;
import java.util.List;

import io.jans.orm.search.filter.Filter;

import org.jboss.logging.Logger;

import org.keycloak.Config;

import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;

import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.kc.protocol.mapper.config.PersistenceConfigurationException;
import io.jans.kc.protocol.mapper.config.PersistenceConfigurationFactory;
import io.jans.kc.protocol.mapper.model.JansPerson;
import io.jans.model.JansAttribute;
import io.jans.model.GluuStatus;
import io.jans.orm.PersistenceEntryManager;

import org.keycloak.protocol.saml.mappers.AbstractSAMLProtocolMapper;
import org.keycloak.protocol.saml.mappers.SAMLAttributeStatementMapper;

import org.keycloak.saml.common.constants.JBossSAMLURIConstants;



public class SamlProtocolMapper extends AbstractSAMLProtocolMapper implements SAMLAttributeStatementMapper {
    
    private static final String DISPLAY_TYPE = "Janssen User Attribute";
    private static final String DISPLAY_CATEGORY = AttributeStatementHelper.ATTRIBUTE_STATEMENT_CATEGORY;
    private static final String HELP_TEXT = "Maps a Janssen User's Attribute to a SAML Attribute";
    private static final String PROVIDER_ID = "kc-jans-saml-protocol-mapper";

    //properties 
    private static final String JANS_ATTR_NAME_PROP_NAME = "jans.attribute.name";
    private static final String JANS_ATTR_NAME_PROP_LABEL = "Jans Attribute";
    private static final String JANS_ATTR_NAME_PROP_HELPTEXT = "Name of the Attribute in Janssen Auth Server";
    private static final List<ProviderConfigProperty> configProperties;

    
    private static final Logger log = Logger.getLogger(SamlProtocolMapper.class);

    private final PersistenceConfigurationFactory persistenceConfigurationFactory;

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

    public SamlProtocolMapper() {

        try {
            persistenceConfigurationFactory = PersistenceConfigurationFactory.create();
            PersistenceEntryManager persistenceEntryManager = persistenceConfigurationFactory.getPersistenceEntryManager();
            
        }catch(PersistenceConfigurationException e) {
            throw new RuntimeException("Could not instantiate protocol mapper",e);
        }
    }
    
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
        
        final String attributename = mappingModel.getConfig().get(JANS_ATTR_NAME_PROP_NAME);
        log.infov("Transform attribute statement. Attribute name: {0}",attributename);
        JansAttribute attr = findJansAttributeByName(attributename);
        if(attr == null) {
            log.infov("No attribute found by name : {0}. No transformation to effect",attributename);
            return;
        }

        if(attr.getStatus() != GluuStatus.ACTIVE) {
            log.infov("Attribute {0} disabled. Skipping it for transformAttributeStatement()");
            return;
        }
        JansPerson person = findJansPersonByUsername(userSession.getLoginUsername(),new String[] {attributename});
        if(person == null) {
            log.infov("No jans User associated with this keycloak session's user {0}",userSession.getLoginUsername());
            return;
        }
        addJansAttributeValueFromPerson(attr,person,attributeStatement,mappingModel,userSession);
    }

    private PersistenceEntryManager getPersistenceEntryManager() {

        return persistenceConfigurationFactory.getPersistenceEntryManager();
    }

    private void addJansAttributeValueFromPerson(JansAttribute jansAttribute, JansPerson jansPerson, 
        AttributeStatementType attributeStatement, ProtocolMapperModel protocolMapper,UserSessionModel userSession) {
        
        if(!jansPerson.hasCustomAttributes()) {
            log.infov("Jans User with keycloak login username {0} returned no custom attributes.",userSession.getLoginUsername());
            return;
        }
        
        AttributeType attributeType = createAttributeType(protocolMapper, jansAttribute);
        List<String> values = jansPerson.customAttributeValues(jansAttribute);
        if(values == null) {
            log.infov("Jans user with keycloak login username {0} returned no values for attribute {1}",
                userSession.getLoginUsername(),jansAttribute.getName());
            return;
        }
        
        values.forEach(attributeType::addAttributeValue);
        attributeStatement.addAttribute(new AttributeStatementType.ASTChoiceType(attributeType));
    }

    private JansAttribute findJansAttributeByName(final String jansAttrName) {

        final String [] attrs = new String [] {
            "displayName",
            "jansAttrTyp",
            "jansClaimName",
            "jansSAML1URI",
            "jansSAML2URI",
            "jansStatus", 
            "jansAttrName"           
        };

        final Filter filter = Filter.createEqualityFilter("jansAttrName",jansAttrName);
        return getPersistenceEntryManager().findEntries("ou=attributes,o=jans",JansAttribute.class,filter,attrs).get(0);
    }

    private JansPerson findJansPersonByUsername(final String username, final String [] returnattributes) {

        final Filter uidsearchfilter = Filter.createEqualityFilter("uid",username);
        final Filter mailsearchfilter = Filter.createEqualityFilter("mail",username);
        final Filter usersearchfilter = Filter.createORFilter(uidsearchfilter,mailsearchfilter);

        return getPersistenceEntryManager().findEntries("ou=people,o=jans",JansPerson.class,usersearchfilter,returnattributes).get(0);
    }

    private AttributeType createAttributeType(ProtocolMapperModel model, JansAttribute jansAttributeMeta) {

        String attributeName = jansAttributeMeta.getSaml2Uri();
        String attributeNameFormat = JBossSAMLURIConstants.ATTRIBUTE_FORMAT_URI.get();
        if(jansAttributeMeta.getSaml2Uri() == null || jansAttributeMeta.getSaml1Uri().isEmpty()) {
            attributeName = jansAttributeMeta.getName();
            attributeNameFormat = JBossSAMLURIConstants.ATTRIBUTE_FORMAT_BASIC.get();
        }

        AttributeType ret = new AttributeType(attributeName);
        ret.setNameFormat(attributeNameFormat);
        if(jansAttributeMeta.getDisplayName() != null && !jansAttributeMeta.getDisplayName().trim().isEmpty()) {
            ret.setFriendlyName(jansAttributeMeta.getDisplayName());
        }
        return ret;
    }

}