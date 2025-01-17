/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.comp.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;

import org.jboss.weld.junit5.ExplicitParamInjection;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.jboss.weld.junit5.auto.WeldJunit5AutoExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;

import io.jans.as.common.model.common.User;
import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.model.user.authenticator.UserAuthenticator;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.PersistenceEntryManagerFactory;
import io.jans.orm.model.PersistenceConfiguration;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.orm.service.PersistanceFactoryService;
import io.jans.orm.util.properties.FileConfiguration;
import io.jans.service.EncryptionService;
import io.jans.service.UserAuthenticatorService;
import io.jans.util.security.SecurityProviderUtility;
import io.jans.util.security.StringEncrypter;
import io.jans.util.security.StringEncrypter.EncryptionException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/**
 * @author Yuriy Movchan Date: 18/11/2024
 */
@ExtendWith(WeldJunit5AutoExtension.class)
@EnableAutoWeld
@TestMethodOrder(OrderAnnotation.class)

@AddBeanClasses(io.jans.service.util.Resources.class)

@AddBeanClasses(value = { io.jans.orm.service.PersistanceFactoryService.class,
		io.jans.orm.ldap.impl.LdapEntryManagerFactory.class, io.jans.orm.sql.impl.SqlEntryManagerFactory.class,
		io.jans.orm.couchbase.impl.CouchbaseEntryManagerFactory.class, io.jans.orm.cloud.spanner.impl.SpannerEntryManagerFactory.class})

@ExplicitParamInjection
public class UserServiceTest {
	
	static {
		System.setProperty("jans.base", "target");
	}

	@Inject
	Logger log;

	@Inject
	PersistenceEntryManager persistenceEntryManager;
	
	@Inject
	UserAuthenticatorService userAuthenticatorService;
    
	private static String userId, userDn;
	
	@BeforeAll
	public static void init() {
		SecurityProviderUtility.installBCProvider();

		long currentTimeMillis = System.currentTimeMillis();
		userId = "sample_user_" + currentTimeMillis;
		userDn = String.format("inum=%s,ou=people,o=jans", currentTimeMillis);
	}

    @Produces
    @ApplicationScoped
    public StringEncrypter getStringEncrypter() throws EncryptionException {
        String saltFilePath = Paths.get(Paths.get("").toAbsolutePath().toString(), "target/conf/salt").toAbsolutePath().toString();
        FileConfiguration cryptoConfiguration = new FileConfiguration(saltFilePath);
        String encodeSalt = cryptoConfiguration.getString("encodeSalt");

        return StringEncrypter.instance(encodeSalt);
    }

    @Produces
    @ApplicationScoped
    public PersistenceEntryManager createPersistenceEntryManager(@Default PersistanceFactoryService persistanceFactoryService, @Default EncryptionService encryptionService) {
    	PersistenceConfiguration persistenceConfiguration = persistanceFactoryService.loadPersistenceConfiguration("jans.properties");
        FileConfiguration persistenceConfig = persistenceConfiguration.getConfiguration();
        Properties connectionProperties = encryptionService.decryptAllProperties(persistenceConfig.getProperties());

        PersistenceEntryManagerFactory persistenceEntryManagerFactory = persistanceFactoryService.getPersistenceEntryManagerFactory(persistenceConfiguration);
        PersistenceEntryManager persistenceEntryManager = persistenceEntryManagerFactory.createEntryManager(connectionProperties);
        log.info("Created {}: {} with operation service: {}",
                ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME, persistenceEntryManager,
                persistenceEntryManager.getOperationService());


        return persistenceEntryManager;
    }

    @Test
	@Order(1)
    public void testUserPersist() {
    	User user = new User();
        user.setDn(userDn);
        user.setUserId(userId);
        user.getCustomAttributes().add(new CustomObjectAttribute("jansAddress", Arrays.asList("New York", "Texas")));
        userAuthenticatorService.addUserAuthenticator(user, new UserAuthenticator("id1", "type1"));
        persistenceEntryManager.persist(user);
    }

    @Test
	@Order(2)
    public void findUserAfterPersist() {
        User user = persistenceEntryManager.find(User.class, userDn);

        assertNotNull(user);

        assertEquals(user.getUserId(), userId);

        assertNotNull(user.getAuthenticator());
        assertNotNull(user.getAuthenticator().getAuthenticators());
        assertEquals(user.getAuthenticator().getAuthenticators().size(), 1);
        assertEquals(userAuthenticatorService.getUserAuthenticatorsByType(user, "type1"), Arrays.asList(new UserAuthenticator("id1", "type1")));

        assertNotNull(user.getExternalUid());
        assertEquals(user.getExternalUid().length, 1);
        assertEquals(user.getExternalUid()[0], "type1:id1");

        assertNotNull(user.getCustomAttributes());
        assertEquals(user.getCustomAttributes().size(), 1);
        assertEquals(user.getAttributeValues("jansAddress"), Arrays.asList("New York", "Texas"));
    }

    @Test
	@Order(3)
    public void testUserRemoveAuthenticator() {
        User user = persistenceEntryManager.find(User.class, userDn);
        userAuthenticatorService.removeUserAuthenticator(user, "type1");

        persistenceEntryManager.merge(user);
    }

    @Test
	@Order(4)
    public void findUserAfterAuthenticatorRemoval() {
        User user = persistenceEntryManager.find(User.class, userDn);

        assertNotNull(user);

        assertEquals(user.getUserId(), userId);

        assertNull(user.getAuthenticator());
        assertNull(user.getExternalUid());

        assertNotNull(user.getCustomAttributes());
        assertEquals(user.getCustomAttributes().size(), 1);
        assertEquals(user.getAttributeValues("jansAddress"), Arrays.asList("New York", "Texas"));
    }

    @Test
	@Order(5)
    public void testUserSetNullAuthenticator() {
        User user = persistenceEntryManager.find(User.class, userDn);
        user.setAuthenticator(null);

        persistenceEntryManager.merge(user);
    }

    @Test
	@Order(6)
    public void findUserAfterAuthenticatorSetNulll() {
        User user = persistenceEntryManager.find(User.class, userDn);

        assertNotNull(user);

        assertEquals(user.getUserId(), userId);

        assertNull(user.getAuthenticator());
        assertNull(user.getExternalUid());

        assertNotNull(user.getCustomAttributes());
        assertEquals(user.getCustomAttributes().size(), 1);
        assertEquals(user.getAttributeValues("jansAddress"), Arrays.asList("New York", "Texas"));
    }

    @Test
	@Order(7)
    public void testUserAddCustomAttribute() {
        User user = persistenceEntryManager.find(User.class, userDn);
        user.getCustomAttributes().add(new CustomObjectAttribute("transientId", "transientId"));

        persistenceEntryManager.merge(user);
    }

    @Test
	@Order(8)
    public void findUserAfterAddCustomAttribute() {
        User user = persistenceEntryManager.find(User.class, userDn);

        assertNotNull(user);

        assertEquals(user.getUserId(), userId);

        assertNull(user.getAuthenticator());
        assertNull(user.getExternalUid());

        assertNotNull(user.getCustomAttributes());
        assertEquals(user.getCustomAttributes().size(), 2);
        assertEquals(user.getAttributeValues("jansAddress"), Arrays.asList("New York", "Texas"));
        assertEquals(user.getAttributeValues("transientId"), Arrays.asList("transientId"));
    }

    @Test
	@Order(9)
    public void testUserRemoveCustomAttribute1() {
        User user = persistenceEntryManager.find(User.class, userDn);
        user.removeAttributeValue("jansAddress");

        persistenceEntryManager.merge(user);
    }

    @Test
	@Order(10)
    public void findUserRemoveCustomAttribute1() {
        User user = persistenceEntryManager.find(User.class, userDn);

        assertNotNull(user);

        assertEquals(user.getUserId(), userId);

        assertNull(user.getAuthenticator());
        assertNull(user.getExternalUid());

        assertNotNull(user.getCustomAttributes());
        assertEquals(user.getCustomAttributes().size(), 1);
        assertEquals(user.getAttributeValues("transientId"), Arrays.asList("transientId"));
    }

    @Test
	@Order(11)
    public void testUserRemoveCustomAttribute2() {
        User user = persistenceEntryManager.find(User.class, userDn);
        user.removeAttributeValue("transientId");

        persistenceEntryManager.merge(user);
    }

    @Test
	@Order(12)
    public void findUserRemoveCustomAttribute2() {
        User user = persistenceEntryManager.find(User.class, userDn);

        assertNotNull(user);

        assertEquals(user.getUserId(), userId);

        assertNull(user.getAuthenticator());
        assertNull(user.getExternalUid());

        assertNotNull(user.getCustomAttributes());
        assertEquals(user.getCustomAttributes().size(), 0);
    }

    @Test
	@Order(13)
    public void testUserAddAuthenticator() {
        User user = persistenceEntryManager.find(User.class, userDn);
        userAuthenticatorService.addUserAuthenticator(user, new UserAuthenticator("id2", "type2"));

        persistenceEntryManager.merge(user);
    }

    @Test
	@Order(14)
    public void findUserAfterAddAuthenticator() {
        User user = persistenceEntryManager.find(User.class, userDn);

        assertNotNull(user);

        assertEquals(user.getUserId(), userId);

        assertNotNull(user.getAuthenticator());
        assertNotNull(user.getAuthenticator().getAuthenticators());
        assertEquals(user.getAuthenticator().getAuthenticators().size(), 1);
        assertEquals(userAuthenticatorService.getUserAuthenticatorsByType(user, "type1"), Arrays.asList(new UserAuthenticator("id2", "type2")));

    }

}
