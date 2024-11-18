/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.comp;

import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import io.jans.as.common.model.common.User;
import io.jans.as.server.BaseComponentTest;
import io.jans.model.user.authenticator.UserAuthenticator;
import io.jans.orm.model.base.CustomObjectAttribute;

/**
 * @author Yuriy Movchan Date: 18/11/2024
 */

public class UserServiceTest extends BaseComponentTest {
	
	private String userId, userDn;
	
	@BeforeClass
	public void init() {
		long currentTimeMillis = System.currentTimeMillis();
		this.userId = "sample_user_" + currentTimeMillis;
		this.userDn = String.format("inum=%s,ou=people,o=jans", currentTimeMillis);
	}

    @Test
    public void testUserPersist() {
    	User user = new User();
        user.setDn(userDn);
        user.setUserId(userId);
        user.getCustomAttributes().add(new CustomObjectAttribute("address", Arrays.asList("New York", "Texas")));
        getUserAuthenticatorService().addUserAuthenticator(user, new UserAuthenticator("id1", "type1"));
        getPersistenceEntryManager().persist(user);
    }

    @Test(dependsOnMethods = "testUserPersist")
    public void findUserAfterPersist() {
        User user = getPersistenceEntryManager().find(User.class, userDn);

        Assert.assertNotNull(user);

        Assert.assertEquals(user.getUserId(), userId);

        Assert.assertNotNull(user.getAuthenticator());
        Assert.assertNotNull(user.getAuthenticator().getAuthenticators());
        Assert.assertEquals(user.getAuthenticator().getAuthenticators().size(), 1);
        Assert.assertEquals(getUserAuthenticatorService().getUserAuthenticatorById(user, "type1"), new UserAuthenticator("id1", "type1"));

        Assert.assertNotNull(user.getExternalUid());
        Assert.assertEquals(user.getExternalUid(), new String[] { "type1:id1" });

        Assert.assertNotNull(user.getCustomAttributes());
        Assert.assertEquals(user.getCustomAttributes().size(), 1);
        Assert.assertEquals(user.getAttributeObject("address"), Arrays.asList("New York", "Texas"));
    }

    @Test(dependsOnMethods = "findUserAfterPersist")
    public void testUserRemoveAuthenticator() {
        User user = getPersistenceEntryManager().find(User.class, userDn);
        getUserAuthenticatorService().removeUserAuthenticator(user, "type1");

        getPersistenceEntryManager().merge(user);
    }

    @Test(dependsOnMethods = "testUserRemoveAuthenticator")
    public void findUserAfterAuthenticatorRemoval() {
        User user = getPersistenceEntryManager().find(User.class, userDn);

        Assert.assertNotNull(user);

        Assert.assertEquals(user.getUserId(), userId);

        Assert.assertNotNull(user.getAuthenticator());
        Assert.assertNull(user.getAuthenticator().getAuthenticators());
        
        Assert.assertNull(user.getExternalUid());

        Assert.assertNotNull(user.getCustomAttributes());
        Assert.assertEquals(user.getCustomAttributes().size(), 1);
        Assert.assertEquals(user.getAttributeObject("address"), Arrays.asList("New York", "Texas"));
    }

    @Test(dependsOnMethods = "findUserAfterAuthenticatorRemoval")
    public void testUserSetNullAuthenticator() {
        User user = getPersistenceEntryManager().find(User.class, userDn);
        user.setAuthenticator(null);

        getPersistenceEntryManager().merge(user);
    }

    @Test(dependsOnMethods = "testUserRemoveAuthenticator")
    public void findUserAfterAuthenticatorSetNulll() {
        User user = getPersistenceEntryManager().find(User.class, userDn);

        Assert.assertNotNull(user);

        Assert.assertEquals(user.getUserId(), userId);

        Assert.assertNull(user.getAuthenticator());
        Assert.assertNull(user.getExternalUid());

        Assert.assertNotNull(user.getCustomAttributes());
        Assert.assertEquals(user.getCustomAttributes().size(), 1);
        Assert.assertEquals(user.getAttributeObject("address"), Arrays.asList("New York", "Texas"));
    }

    @Test(dependsOnMethods = "findUserAfterAuthenticatorSetNulll")
    public void testUserAddCustomAttribute() {
        User user = getPersistenceEntryManager().find(User.class, userDn);
        user.getCustomAttributes().add(new CustomObjectAttribute("transientId", "transientId"));

        getPersistenceEntryManager().merge(user);
    }

    @Test(dependsOnMethods = "testUserAddCustomAttribute")
    public void findUserAfterAddCustomAttribute() {
        User user = getPersistenceEntryManager().find(User.class, userDn);

        Assert.assertNotNull(user);

        Assert.assertEquals(user.getUserId(), userId);

        Assert.assertNull(user.getAuthenticator());
        Assert.assertNull(user.getExternalUid());

        Assert.assertNotNull(user.getCustomAttributes());
        Assert.assertEquals(user.getCustomAttributes().size(), 2);
        Assert.assertEquals(user.getAttributeObject("address"), Arrays.asList("New York", "Texas"));
        Assert.assertEquals(user.getAttributeObject("transientId"), Arrays.asList("transientId"));
    }

    @Test(dependsOnMethods = "findUserAfterAddCustomAttribute")
    public void testUserRemoveCustomAttribute1() {
        User user = getPersistenceEntryManager().find(User.class, userDn);
        user.removeAttributeValue("address");

        getPersistenceEntryManager().merge(user);
    }

    @Test(dependsOnMethods = "testUserRemoveCustomAttribute1")
    public void findUserRemoveCustomAttribute1() {
        User user = getPersistenceEntryManager().find(User.class, userDn);

        Assert.assertNotNull(user);

        Assert.assertEquals(user.getUserId(), userId);

        Assert.assertNull(user.getAuthenticator());
        Assert.assertNull(user.getExternalUid());

        Assert.assertNotNull(user.getCustomAttributes());
        Assert.assertEquals(user.getCustomAttributes().size(), 1);
        Assert.assertEquals(user.getAttributeObject("transientId"), Arrays.asList("transientId"));
    }

    @Test(dependsOnMethods = "findUserRemoveCustomAttribute1")
    public void testUserRemoveCustomAttribute2() {
        User user = getPersistenceEntryManager().find(User.class, userDn);
        user.removeAttributeValue("transientId");

        getPersistenceEntryManager().merge(user);
    }

    @Test(dependsOnMethods = "testUserRemoveCustomAttribute2")
    public void findUserRemoveCustomAttribute2() {
        User user = getPersistenceEntryManager().find(User.class, userDn);

        Assert.assertNotNull(user);

        Assert.assertEquals(user.getUserId(), userId);

        Assert.assertNull(user.getAuthenticator());
        Assert.assertNull(user.getExternalUid());

        Assert.assertNotNull(user.getCustomAttributes());
        Assert.assertEquals(user.getCustomAttributes().size(), 0);
    }
}
