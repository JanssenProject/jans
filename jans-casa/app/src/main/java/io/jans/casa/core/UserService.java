package io.jans.casa.core;

import io.jans.orm.search.filter.Filter;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.management.AttributeNotFoundException;

import io.jans.casa.conf.MainSettings;
import io.jans.casa.core.model.Person;
import io.jans.casa.core.pojo.User;
import io.jans.casa.credential.CredentialRemovalConflict;
import io.jans.casa.extension.AuthnMethod;
import io.jans.casa.misc.Utils;
import io.jans.casa.misc.WebUtils;
import io.jans.casa.service.SndFactorAuthenticationUtils;
import org.slf4j.Logger;
import org.zkoss.util.Pair;
import org.zkoss.util.resource.Labels;
/**
 * An app. scoped bean that encapsulates logic related to users manipulation (CRUD) at memory level (no LDAP storage)
 * @author jgomer
 */
@Named
@ApplicationScoped
public class UserService implements SndFactorAuthenticationUtils {

    private static final String PREFERRED_METHOD_ATTR = "jansPreferredMethod";
    private static final String ADMIN_LOCK_FILE = System.getProperty("admin.lock");

    @Inject
    private Logger logger;

    @Inject
    private PersistenceService persistenceService;
    
    @Inject
    private ExtensionsManager extManager;

    @Inject
    private ConfigurationHandler confHandler;

    @Inject
    private MainSettings mainSettings;

    public User getUserFromClaims(Map<String, Object> claims) throws AttributeNotFoundException {

        User u = new User();
        u.setClaims(claims);

        String username = u.getUserName();
        String inum = u.getId();
        logger.trace("User instance created. Username is {}", username);

        if (inum == null || username == null) {
            logger.error("Could not obtain minimal user claims!");
            throw new AttributeNotFoundException("Cannot retrieve claims for logged user");
        }

        String img = u.getPictureURL();
        if (Utils.isNotEmpty(img)) {
            u.setPictureURL(WebUtils.validateImageUrl(img));
        }

        Person person = personInstance(inum);
        if (person == null) {
            throw new AttributeNotFoundException("Cannot retrieve user's info from database");
        }

        u.setPreferredMethod(person.getPreferredMethod());
        u.setAdmin(persistenceService.isAdmin(inum) && administrationAllowed());
        
        cleanRandEnrollmentCode(inum);

        return u;

    }

    public List<Pair<AuthnMethod, Integer>> getUserMethodsCount(String userId) {
        return getLiveAuthnMethods().stream()
                .map(aMethod -> new Pair<>(aMethod, aMethod.getTotalUserCreds(userId)))
                .filter(pair -> pair.getY() > 0).collect(Collectors.toList());
    }

    public boolean turn2faOn(User user) {
        return turn2faOn(user, null);
    }
    
    public boolean turn2faOn(User user, String method) {
        return setPreferredMethod(user, method == null ? Long.toString(System.currentTimeMillis()) : method);
    }

    public boolean turn2faOff(User user) {
        return setPreferredMethod(user, null);
    }

    public Pair<CredentialRemovalConflict, String> removalConflict(String credentialType, int nCredsOfType, User user) {

        //Assume removal has no problem
        CredentialRemovalConflict conflict = null;
        String message = null;
        if (user.getPreferredMethod() != null) {

            //Compute how many credentials current user has added
            logger.info("Checking potential conflicts if a credential of type '{}' is removed from '{}'", credentialType, user.getId());
            List<Pair<AuthnMethod, Integer>> userMethodsCount = getUserMethodsCount(user.getId());

            int totalCreds = userMethodsCount.stream().mapToInt(Pair::getY).sum();
            int minCredsFor2FA = mainSettings.getBasic2FASettings().getMinCreds();
            logger.debug("Total number of user creds is {}", totalCreds);

            if (nCredsOfType == 1) {
                List<AuthnMethod> methods = getLiveAuthnMethods().stream()
                        .filter(AuthnMethod::mayBe2faActivationRequisite).collect(Collectors.toList());
                boolean typeOfCredIs2FARequisite = methods.stream().map(AuthnMethod::getAcr).anyMatch(acr -> acr.equals(credentialType));

                if (typeOfCredIs2FARequisite) {
                    logger.debug("Credential belongs to 2FA requisite");
                    //Check if credential being removed is the only one belonging to 2FARequisiteMethods
                    int nCredsBelongingTo2FARequisite = userMethodsCount.stream()
                            .filter(pair -> pair.getX().mayBe2faActivationRequisite()).mapToInt(Pair::getY).sum();

                    if (nCredsBelongingTo2FARequisite == 1) {
                        logger.debug("There is only one credential belonging to 2FA requisite methods for this user");
                        //Compute the names of those authentication methods which are requisite for 2FA activation
                        String commaSepNames = methods.stream().map(aMethod -> Labels.getLabel(aMethod.getUINameKey()))
                                .collect(Collectors.toList()).toString();
                        commaSepNames = commaSepNames.substring(1, commaSepNames.length() - 1);

                        conflict = CredentialRemovalConflict.REQUISITE_NOT_FULFILED;
                        message = Labels.getLabel("usr.del_conflict_requisite", new String[]{ commaSepNames });
                    }
                }
            }
            if (message == null && totalCreds == minCredsFor2FA) {
                logger.debug("Removal of credential would result in less than {} (minimum required for 2FA)", minCredsFor2FA);
                conflict = CredentialRemovalConflict.CREDS2FA_NUMBER_UNDERFLOW;
                message = Labels.getLabel("usr.del_conflict_underflow", new Integer[]{ minCredsFor2FA });
            }
        }
        if (message != null) {
            message = Labels.getLabel("usr.del_conflict_revert", new String[]{message});
        }

        return new Pair<>(conflict, message);

    }
    
    public void notifyEnrollment(User user, String credentialType) {
    	attemptAutoEnable2FA(user, getUserMethodsCount(user.getId()).stream().mapToInt(Pair::getY).sum());
    	//What else to call here?    	
    }
    
    public void attemptAutoEnable2FA(User user, int totalCreds) {

    	try {
			if (user.getPreferredMethod() == null && mainSettings.getBasic2FASettings().isAutoEnable() 
				&& totalCreds >= mainSettings.getBasic2FASettings().getMinCreds()) {
				
				if (turn2faOn(user)) {
					logger.info("2FA has been automatically enabled for user '{}'", user.getUserName());
				}
			}
    	} catch (Exception e) {
    		logger.error(e.getMessage(), e);
    	}
    	
    }

    private boolean setPreferredMethod(User user, String method) {

        boolean success = setPreferredMethod(user.getId(), method);
        if (success) {
            user.setPreferredMethod(method);
        }
        return success;

    }

    /**
     * Resets the preferred method of authentication for the users referenced by LDAP dn
     * @param userInums A List containing user DNs
     * @return The number of modified entries in LDAP
     */
    public int resetPreference(List<String> userInums) {

        int modified = 0;
        try {
            for (String inum : userInums) {
                if (setPreferredMethod(inum, null)) {
                    modified++;
                    logger.info("Turned 2FA off for user '{}'", inum);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return modified;

    }

    /**
     * Builds a list of users whose username, first or last name matches the pattern passed, and at the same time have a
     * preferred authentication method other than password
     * @param searchString Pattern for search
     * @return A collection of SimpleUser instances. Null if an error occurred to compute the list
     */
    public List<Person> searchUsers(String searchString) {

        Stream<Filter> stream = Stream.of("uid", "givenName", "sn")
                .map(attr -> Filter.createSubstringFilter(attr, null, new String[]{ searchString }, null));

        Filter filter = Filter.createANDFilter(
                Filter.createORFilter(stream.collect(Collectors.toList())),
                Filter.createPresenceFilter(PREFERRED_METHOD_ATTR)
        );
        return persistenceService.find(Person.class, persistenceService.getPeopleDn(), filter);

    }

    private boolean setPreferredMethod(String id, String method) {

        boolean success = false;
        try {
            Person person = personInstance(id);
            person.setPreferredMethod(method);
            success = persistenceService.modify(person);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return success;

    }

    public String generateRandEnrollmentCode(String userId) {

        logger.debug("Writing random enrollment code for {}", userId);
        String code = UUID.randomUUID().toString();
        Person person = persistenceService.get(Person.class, persistenceService.getPersonDn(userId));
        person.setEnrollmentCode(code);
        return persistenceService.modify(person) ? code : null;

    }

    public void cleanRandEnrollmentCode(String userId) {
        Person person = persistenceService.get(Person.class, persistenceService.getPersonDn(userId));

        if (Utils.isNotEmpty(person.getEnrollmentCode())) {
            logger.trace("Removing enrollment code for {}", userId);
            person.setEnrollmentCode(null);
            persistenceService.modify(person);
        }
    }

    public List<AuthnMethod> getLiveAuthnMethods() {

        List<AuthnMethod> methods = new ArrayList<>();
        Map<String, String> acrPluginMap = mainSettings.getAcrPluginMap();

        for (String acr : acrPluginMap.keySet()) {
            extManager.getAuthnMethodExts(Collections.singleton(acrPluginMap.get(acr)))
                    .stream().filter(am -> am.getAcr().equals(acr)).findFirst().ifPresent(methods::add);
        }
        return methods;

    }

    private Person personInstance(String id) {
        return persistenceService.get(Person.class, persistenceService.getPersonDn(id));
    }

    /**
     * Administration functionalities are enabled only if .administrable file exists 
     * @return boolean value
     */
    private boolean administrationAllowed() {
        return Files.isReadable(Paths.get(ADMIN_LOCK_FILE));
    }

}
