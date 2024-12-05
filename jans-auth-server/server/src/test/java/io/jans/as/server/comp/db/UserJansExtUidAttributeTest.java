/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.comp.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
 * @author Yuriy Movchan Date: 05/12/2024
 */
@ExtendWith(WeldJunit5AutoExtension.class)
@EnableAutoWeld
@TestMethodOrder(OrderAnnotation.class)

@AddBeanClasses(io.jans.service.util.Resources.class)

@AddBeanClasses(value = { io.jans.orm.service.PersistanceFactoryService.class,
		io.jans.orm.ldap.impl.LdapEntryManagerFactory.class, io.jans.orm.sql.impl.SqlEntryManagerFactory.class,
		io.jans.orm.couchbase.impl.CouchbaseEntryManagerFactory.class, io.jans.orm.cloud.spanner.impl.SpannerEntryManagerFactory.class})

@ExplicitParamInjection
public class UserJansExtUidAttributeTest {
	
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
        FileConfiguration cryptoConfiguration = new FileConfiguration(".\\target\\conf\\salt");
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
        persistenceEntryManager.persist(user);
    }

    @Test
	@Order(2)
    public void findUserAfterPersist() {
        User user = persistenceEntryManager.find(User.class, userDn);

        assertNotNull(user);
        assertEquals(user.getUserId(), userId);
    }

    @Test
	@Order(3)
    public void addUserLegacyAuthenticators() {
        User user = persistenceEntryManager.find(User.class, userDn);

        assertNull(user.getAttributeValues("jansExtUid"));
        
        List<String> jansExtUids = new ArrayList<>();
        jansExtUids.add("type1:id1");
        jansExtUids.add("type1:id1");

        user.setAttribute("jansExtUid", jansExtUids);
        persistenceEntryManager.merge(user);

        assertEquals(user.getUserId(), userId);

        assertNotNull(user.getAuthenticator());
        assertNotNull(user.getAuthenticator().getAuthenticators());
        assertEquals(user.getAuthenticator().getAuthenticators().size(), 1);
        assertEquals(userAuthenticatorService.getUserAuthenticatorsByType(user, "type1"), Arrays.asList(new UserAuthenticator("id2", "type2")));
    }

    @Test
	@Order(4)
    public void checkUserLegacyAuthenticators() {
        User user = persistenceEntryManager.find(User.class, userDn);

        // Access as custom attribute
        List<String> jansExtUids = user.getAttributeValues("jansExtUid");
        assertNotNull(jansExtUids);

        jansExtUids.add("type1:id1");
        jansExtUids.add("type2:id2");

        assertEquals(jansExtUids.size(), 2);
        assertEquals(jansExtUids.get(0), "type1:id1");
        assertEquals(jansExtUids.get(1), "type2:id2");

        // Access as generic attribute
        assertNotNull(user.getExternalUid());
        assertEquals(user.getExternalUid().length, 2);
        assertEquals(user.getExternalUid()[0], "type1:id1");
        assertEquals(user.getExternalUid()[0], "type2:id2");
    }

}
