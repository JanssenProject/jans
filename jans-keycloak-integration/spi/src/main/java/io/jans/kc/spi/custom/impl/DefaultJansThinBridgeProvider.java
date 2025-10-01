package io.jans.kc.spi.custom.impl;

import java.util.List;

import io.jans.kc.model.JansUserAttributeModel;
import io.jans.kc.model.internal.JansPerson;
import io.jans.kc.spi.custom.JansThinBridgeProvider;
import io.jans.kc.spi.custom.JansThinBridgeOperationException;

import io.jans.model.JansAttribute;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;

import org.jboss.logging.Logger;


public class DefaultJansThinBridgeProvider implements JansThinBridgeProvider {
    
    private static final String JANS_ATTRIBUTES_ROOT_DN = "ou=attributes,o=jans";
    private static final String JANS_PEOPLE_ROOT_DN = "ou=people,o=jans";
    private static final String UID_ATTR_NAME = "uid";
    private static final String MAIL_ATTR_NAME = "mail";
    private static final String INUM_ATTR_NAME = "inum";
    private static final Logger log = Logger.getLogger(DefaultJansThinBridgeProvider.class);
    private static final String [] defaultUserReturnAttributes = new String [] {
        "uid","mail","displayName","givenName","inum","sn", "cn", 
        "jansCreationTimestamp", "jansLastLogonTime","updatedAt", "jansStatus"
    };

    private final PersistenceEntryManager persistenceEntryManager;

    public DefaultJansThinBridgeProvider(final PersistenceEntryManager persistenceEntryManager) {

        this.persistenceEntryManager = persistenceEntryManager;
    }
    
    @Override
    public void close() {
        //for now , nothing to do during the close of the provider
    }

    @Override 
    public JansUserAttributeModel getUserAttribute(final String kcLoginUsername, final String attributeName) {

        try {

            String [] jansAttrReturnAttributes = new String [] {
                "displayName","jansAttrTyp","jansClaimName",
                "jansSAML1URI","jansSAML2URI","jansStatus", "jansAttrName"
            };

            final JansAttribute jansAttr = findAttributeByName(attributeName,jansAttrReturnAttributes);
            if(jansAttr == null) {
                return null;
            }

            String [] jansPersonReturnAttributes = new String [] {
                attributeName
            };
            final JansPerson jansPerson = findPersonByKcLoginUsername(kcLoginUsername, jansPersonReturnAttributes);
            if(jansPerson == null) {
                return null;
            }

            return new JansUserAttributeModel(jansAttr,jansPerson);

        }catch(Exception e) {
            throw new JansThinBridgeOperationException("Could not get attributes for user " + kcLoginUsername,e);
        }
    }

    @Override
    public JansPerson getJansUserByUsername(final String username) {

        try {
            final Filter uidSearchFilter = Filter.createEqualityFilter(UID_ATTR_NAME,username);
            final JansPerson person = findPerson(uidSearchFilter,defaultUserReturnAttributes);
            if(person == null) {
                log.debugv("User with uid {0} not found in janssen",username);
                return null;
            }
            log.debugv("User with uid {0} was found in janssen",username);
            return person;
        }catch(Exception e) {
            throw new JansThinBridgeOperationException("Error fetching jans user with username " + username,e);
        }
    }

    @Override
    public JansPerson getJansUserByEmail(final String email) {

        try {
            final Filter mailSearchFilter = Filter.createEqualityFilter(MAIL_ATTR_NAME, email);
            final JansPerson person = findPerson(mailSearchFilter,defaultUserReturnAttributes);
            if(person == null) {
                log.debugv("User with email {0} not found in janssen",email);
                return null;
            }
            log.debugv("User with email {0} was found in janssen",email);
            return person;
        }catch(Exception e) {
            throw new JansThinBridgeOperationException("Error fetching jans user with email " + email ,e);
        }
    }

    @Override
    public JansPerson getJansUserByInum(final String inum) {

        try {
            final Filter inumSearchFilter = Filter.createEqualityFilter(INUM_ATTR_NAME,inum);
            final JansPerson person = findPerson(inumSearchFilter,defaultUserReturnAttributes);
            if(person == null) {
                log.debugv("User with inum not found in janssen",inum);
                return null;
            }
            log.debugv("User with inum {0} found in janssen",inum);
            return person;
        }catch(Exception e) {
            throw new JansThinBridgeOperationException("Error fetching jans user with inum "+inum,e);
        }
    }

    private JansAttribute findAttributeByName(final String attributeName, final String [] returnAttributes) {

        final Filter searchFilter = Filter.createEqualityFilter("jansAttrName", attributeName);
        List<JansAttribute> searchresult =  persistenceEntryManager.findEntries(JANS_ATTRIBUTES_ROOT_DN,
            JansAttribute.class,searchFilter,returnAttributes);
        
        return (searchresult.isEmpty() ? null: searchresult.get(0));
    }

    private JansPerson findPersonByKcLoginUsername(final String kcLoginUsername,final String [] returnAttributes) {

        final Filter uidSearchFilter  = Filter.createEqualityFilter(UID_ATTR_NAME,kcLoginUsername);
        final Filter mailSearchFilter = Filter.createEqualityFilter(MAIL_ATTR_NAME,kcLoginUsername);
        final Filter searchFilter = Filter.createORFilter(uidSearchFilter,mailSearchFilter);

        return findPerson(searchFilter,returnAttributes);
    }

    private JansPerson findPerson(final Filter searchFilter, final String [] returnAttributes)  {
        
        List<JansPerson> searchresult = persistenceEntryManager.findEntries(JANS_PEOPLE_ROOT_DN,JansPerson.class,searchFilter,returnAttributes);
        return (searchresult.isEmpty() ? null: searchresult.get(0));
    }
}
