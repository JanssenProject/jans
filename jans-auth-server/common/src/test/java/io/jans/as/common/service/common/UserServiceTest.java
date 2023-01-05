package io.jans.as.common.service.common;

import io.jans.as.common.model.common.User;
import io.jans.model.GluuStatus;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.base.CustomAttribute;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.service.DataSourceTypeService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

@Listeners(MockitoTestNGListener.class)
public class UserServiceTest {

    @InjectMocks
    protected TestUserService userService;
    @Mock
    protected PersistenceEntryManager persistenceEntryManager;
    @Mock
    protected DataSourceTypeService dataSourceTypeService;
    @Mock
    private Logger log;
    @Mock
    private InumService inumService;

    @Test
    private void getUserByDn_nullDn_null() {
        User user = userService.getUserByDn(null, null);
        assertNull(user);
    }

    @Test
    private void getUserByDn_emptyDn_null() {
        User user = userService.getUserByDn("", null);
        assertNull(user);
    }

    @Test
    private void getUserByDn_validDnWithAttributes_user() {
        when(persistenceEntryManager.find(anyString(), any(), any())).thenReturn(new User());
        User user = userService.getUserByDn("123", null);
        assertNotNull(user);
    }

    @Test
    private void getUserByInum_nullInum_null() {
        User user = userService.getUserByInum(null, null);
        assertNull(user);
    }

    @Test
    private void getUserByInum_emptyInum_null() {
        User user = userService.getUserByInum("", null);
        assertNull(user);
    }

    @Test
    private void getUserByInum_validDnWithAttributes_user() {
        when(persistenceEntryManager.find(anyString(), any(), any())).thenReturn(new User());
        User user = userService.getUserByInum("123", null);
        assertNotNull(user);
    }

    @Test
    private void getUser_nullUserId_user() {
        User user = userService.getUser(null, null);
        assertNull(user);
    }

    @Test
    private void getUser_isSpannerPeopleBaseDn_user() {
        String userId = "123";
        String baseDn = "dn123";
        when(dataSourceTypeService.isSpanner(anyString())).thenReturn(true);
        when(persistenceEntryManager.findEntries(anyString(), any(), any(), any())).thenReturn(getListBasicOneUser(userId, baseDn));
        User user = userService.getUser(userId, null);
        assertNotNull(user);
        assertEquals(user.getUserId(), userId);
        assertEquals(user.getBaseDn(), baseDn);
    }

    @Test
    private void getUser_noSpannerPeopleBaseDn_user() {
        String userId = "123";
        String baseDn = "dn123";
        when(dataSourceTypeService.isSpanner(anyString())).thenReturn(false);
        when(persistenceEntryManager.findEntries(anyString(), any(), any(), any())).thenReturn(getListBasicOneUser(userId, baseDn));
        User user = userService.getUser(userId, null);
        assertNotNull(user);
        assertEquals(user.getUserId(), userId);
        assertEquals(user.getBaseDn(), baseDn);
    }

    @Test
    private void getUser_findEmpty_null() {
        String userId = "123";
        when(dataSourceTypeService.isSpanner(anyString())).thenReturn(true);
        when(persistenceEntryManager.findEntries(anyString(), any(), any(), any())).thenReturn(new ArrayList<>());
        User user = userService.getUser(userId, null);
        assertNull(user);
    }

    @Test
    private void getUserInum_userNull_null() {
        User user = null;
        String inum = userService.getUserInum(user);
        assertNull(inum);
    }

    @Test
    private void getUserInum_userNoInum_null() {
        User user = spy(getBasicUser("123", "dn123"));
        when(user.getAttribute("inum")).thenReturn(null);
        String inum = userService.getUserInum(user);
        assertNull(inum);
    }

    @Test
    private void getUserInum_userWithInum_inum() {
        User user = spy(getBasicUser("123", "dn123"));
        when(user.getAttribute("inum")).thenReturn("inumExample");
        String inum = userService.getUserInum(user);
        assertNotNull(inum);
        assertEquals(inum, "inumExample");
    }

    @Test
    private void getUserInum_userId_inum() {
        User user = spy(getBasicUser("123", "dn123"));
        when(user.getAttribute("inum")).thenReturn("inumExample");

        when(dataSourceTypeService.isSpanner(anyString())).thenReturn(true);
        when(persistenceEntryManager.findEntries(anyString(), any(), any(), any())).thenReturn(getListBasicOneUser(user));
        String inum = userService.getUserInum("123");
        assertNotNull(inum);
        assertEquals(inum, "inumExample");
    }

    @Test
    private void updateUser_nullUser_nullPointerException() {
        try {
            userService.updateUser(null);
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException);
        }
    }

    @Test
    private void updateUser_validUser_user() {
        User user = getBasicUser("123", "dn123");
        doNothing().when(persistenceEntryManager).merge(user);
        when(persistenceEntryManager.find(anyString(), any(), any())).thenReturn(user);
        User resultUser = userService.updateUser(user);
        assertNotNull(user);
        assertNotNull(user.getUserId());
        assertTrue(resultUser.getUserId().equals(user.getUserId()));
    }

    @Test
    private void addDefaultUser_withUidEmptyPersonCustomObject_user() {
        String uid = UUID.randomUUID().toString();
        User user = getBasicUser(uid, "dn123");
        doNothing().when(persistenceEntryManager).persist(any());
        when(dataSourceTypeService.isSpanner(anyString())).thenReturn(true);
        when(persistenceEntryManager.findEntries(anyString(), any(), any(), any())).thenReturn(getListBasicOneUser(user));
        when(inumService.generatePeopleInum()).thenReturn(uid);
        userService.setReturnTestListPersonCustomObjectClassList(false);
        User resultUser = userService.addDefaultUser(uid);
        assertNotNull(resultUser);
        assertNotNull(resultUser.getUserId());
        assertEquals(resultUser.getUserId(), uid);
    }

    @Test
    private void addDefaultUser_withUidNoEmptyPersonCustomObject_user() {
        String uid = UUID.randomUUID().toString();
        User user = getBasicUser(uid, "dn123");
        user.setCustomObjectClasses(TestUserService.DEFAULT_PERSON_CUSTOM_OBJECT_CLASS_LIST);
        doNothing().when(persistenceEntryManager).persist(any());
        when(dataSourceTypeService.isSpanner(anyString())).thenReturn(true);
        when(persistenceEntryManager.findEntries(anyString(), any(), any(), any())).thenReturn(getListBasicOneUser(user));
        when(inumService.generatePeopleInum()).thenReturn(uid);
        userService.setReturnTestListPersonCustomObjectClassList(true);

        User resultUser = userService.addDefaultUser(uid);
        assertNotNull(resultUser);
        assertNotNull(resultUser.getUserId());
        assertEquals(resultUser.getUserId(), uid);
        assertTrue(resultUser.getCustomObjectClasses().length == 2);
        assertTrue(resultUser.getCustomObjectClasses()[0].equals(user.getCustomObjectClasses()[0]));
    }

    @Test
    private void addUser_noEmptyPersonCustomObject_user() {
        User user = getBasicUser("123", "dn123");
        doNothing().when(persistenceEntryManager).persist(any());
        when(persistenceEntryManager.find(anyString(), any(), any())).thenReturn(user);
        when(inumService.generatePeopleInum()).thenReturn(UUID.randomUUID().toString());
        userService.setReturnTestListPersonCustomObjectClassList(true);

        User resultUser = userService.addUser(user, true);
        assertNotNull(resultUser);
        assertNotNull(resultUser.getUserId());
        assertNotNull(user.getAttribute("jansStatus"));
        assertNotNull(user.getAttribute("inum"));
        assertEquals(user.getAttribute("jansStatus"), GluuStatus.ACTIVE.getValue());
        assertTrue(resultUser.getCustomObjectClasses().length == 2);
        assertTrue(resultUser.getCustomObjectClasses()[0].equals(user.getCustomObjectClasses()[0]));
    }

    @Test
    private void getUserByAttribute_attributeNameNull_nulll() {
        User resultUser = userService.getUserByAttribute(null, null);
        assertNull(resultUser);
    }

    @Test
    private void getUserByAttribute_attributeValueNull_nulll() {
        User resultUser = userService.getUserByAttribute("attr1", null);
        assertNull(resultUser);
    }

    @Test
    private void getUserByAttribute_validAttributeValueAttributeName_user() {
        String userId = "123";
        String baseDn = "baseDn";
        String attributeName = "attribute1";
        String attributeValue = "value1";
        when(persistenceEntryManager.findEntries(anyString(), any(), any(), anyInt())).thenReturn(getListBasicOneUser(userId, baseDn));
        User resultUser = userService.getUserByAttribute(attributeName, attributeValue);
        assertNotNull(resultUser);
    }

    @Test
    private void getUniqueUserByAttributes_attributeNamesNull_null() {
        User resultUser = userService.getUniqueUserByAttributes(null, null);
        assertNull(resultUser);
    }

    @Test
    private void getUniqueUserByAttributes_attributeNamesEmpty_null() {
        List<String> lisAttributes = new ArrayList<>();
        User resultUser = userService.getUniqueUserByAttributes(lisAttributes, null);
        assertNull(resultUser);
    }

    @Test
    private void getUniqueUserByAttributes_attributeNamesSizeOne_null() {
        String userId = "123";
        String baseDn = "baseDn";
        String attributeName = "attribute1";
        String attributeValue = "value1";
        List<String> lisAttributes = new ArrayList<>();
        lisAttributes.add(attributeName);
        when(persistenceEntryManager.findEntries(any())).thenReturn(getListBasicOneUser(userId, baseDn));
        User resultUser = userService.getUniqueUserByAttributes(lisAttributes, attributeValue);
        assertNotNull(resultUser);
    }

    @Test
    private void getUniqueUserByAttributes_exceptionFindEntriesNull_null() {
        String attributeName = "attribute1";
        String attributeValue = "value1";
        List<String> lisAttributes = new ArrayList<>();
        lisAttributes.add(attributeName);
        when(persistenceEntryManager.findEntries(any())).thenReturn(null);
        User resultUser = userService.getUniqueUserByAttributes(lisAttributes, attributeValue);
        assertNull(resultUser);
    }

    @Test
    private void getUsersByAttribute_multiValuedTrue_null() {
        String userId = "123";
        String baseDn = "baseDn";
        String attributeName = "attribute1";
        String attributeValue = "value1";
        List<String> lisAttributes = new ArrayList<>();
        lisAttributes.add(attributeName);
        when(persistenceEntryManager.findEntries(anyString(), any(), any(), anyInt())).thenReturn(getListBasicOneUser(userId, baseDn));
        List<User> resultListUser = userService.getUsersByAttribute(attributeName, attributeValue, true, 1);
        assertNotNull(resultListUser);
        assertTrue(resultListUser.size() == 1);
    }

    @Test
    private void getUserByAttributes_attributeNamesEmpty_null() {
        String[] attributeNames = {};
        User resultUser = userService.getUserByAttributes(null, attributeNames, null);
        assertNull(resultUser);
    }

    @Test
    private void getUserByAttributes_validAttributeNamesSize1IsSpanner_user() {
        String userId = "123";
        String baseDn = "baseDn";
        String[] attributeNames = {"attribute1"};
        String attributeValue = "value1";
        when(dataSourceTypeService.isSpanner(anyString())).thenReturn(true);
        when(persistenceEntryManager.findEntries(anyString(), any(), any(), any(), anyInt())).thenReturn(getListBasicOneUser(userId, baseDn));
        User resultUser = userService.getUserByAttributes(attributeValue, attributeNames, true, null);
        assertNotNull(resultUser);
    }

    @Test
    private void getUserByAttributes_validAttributeNamesSize2NoIsSpanner_user() {
        String userId = "123";
        String baseDn = "baseDn";
        String[] attributeNames = {"attribute1", "attribute2"};
        String attributeValue = "value1";
        when(dataSourceTypeService.isSpanner(anyString())).thenReturn(false);
        when(persistenceEntryManager.findEntries(anyString(), any(), any(), any(), anyInt())).thenReturn(getListBasicOneUser(userId, baseDn));
        User resultUser = userService.getUserByAttributes(attributeValue, attributeNames, true, null);
        assertNotNull(resultUser);
    }

    @Test
    private void getUserByAttributes_nullAttributes_null() {
        User resultUser = userService.getUserByAttributes(null, true, null);
        assertNull(resultUser);
    }

    @Test
    private void getUserByAttributes_oneAttributes_user() {
        String userId = "123";
        String baseDn = "baseDn";
        List<CustomAttribute> attributes = new ArrayList<>();
        CustomAttribute customAttribute1 = new CustomAttribute("attribute1", "value1");
        attributes.add(customAttribute1);
        when(persistenceEntryManager.findEntries(anyString(), any(), any(), any(), anyInt())).thenReturn(getListBasicOneUser(userId, baseDn));
        User resultUser = userService.getUserByAttributes(attributes, true, null);
        assertNotNull(resultUser);
    }

    @Test
    private void getUserByAttributes_twoAttributes_user() {
        String userId = "123";
        String baseDn = "baseDn";
        List<CustomAttribute> attributes = new ArrayList<>();
        CustomAttribute customAttribute1 = new CustomAttribute("attribute1", "value1");
        CustomAttribute customAttribute2 = new CustomAttribute("attribute2", "value2");
        attributes.add(customAttribute1);
        attributes.add(customAttribute2);
        when(persistenceEntryManager.findEntries(anyString(), any(), any(), any(), anyInt())).thenReturn(getListBasicOneUser(userId, baseDn));
        User resultUser = userService.getUserByAttributes(attributes, true, null);
        assertNotNull(resultUser);
    }

    @Test
    private void getUsersBySample_user_listUser() {
        User user = spy(getBasicUser("123", "dn123"));
        int limit = 1;
        when(persistenceEntryManager.findEntries(any(), anyInt())).thenReturn(getListBasicOneUser(user));
        List<User> listResultUser = userService.getUsersBySample(user, limit);
        assertNotNull(listResultUser);
        assertTrue(listResultUser.size() == 1);
    }

    @Test
    private void addUserAttributeByUserInum_inumNull_null() {
        User resultUser = userService.addUserAttributeByUserInum(null, null, null);
        assertNull(resultUser);
    }

    @Test
    private void addUserAttributeByUserInum_newAttribute_user() {
        String inumUser = "inumUser";
        String attributeName = "attribute1";
        String attributeValue = "value1";
        User user = spy(getBasicUser("123", "dn123"));
        user.setCustomAttributes(new ArrayList<>());

        when(persistenceEntryManager.find(anyString(), any(), any())).thenReturn(user);
        doNothing().when(persistenceEntryManager).merge(user);
        User resultUser = userService.addUserAttributeByUserInum(inumUser, attributeName, attributeValue);
        assertNotNull(resultUser);
        assertTrue(resultUser.getCustomAttributes().size() == 1);
        assertEquals(resultUser.getCustomAttributes().get(0).getName(), attributeName);
        assertEquals(resultUser.getCustomAttributes().get(0).getValue(), attributeValue);
    }

    @Test
    private void addUserAttributeByUserInum_existentAttributeNoValue_user() {
        String inumUser = "inumUser";
        User user = spy(getBasicUser("123", "dn123"));
        user.setDn("dn123");
        CustomObjectAttribute customAttribute1 = new CustomObjectAttribute("attribute1", "value1");
        CustomObjectAttribute customAttribute2 = new CustomObjectAttribute("attribute2", "value2");
        user.setCustomAttributes(new ArrayList<>());
        user.getCustomAttributes().add(customAttribute1);
        user.getCustomAttributes().add(customAttribute2);

        when(persistenceEntryManager.find(anyString(), any(), any())).thenReturn(user);
        doNothing().when(persistenceEntryManager).merge(user);
        User resultUser = userService.addUserAttributeByUserInum(inumUser, "attribute2", "value3");

        assertNotNull(resultUser);
        assertTrue(resultUser.getCustomAttributes().size() == 2);
        assertEquals(resultUser.getCustomAttributes().get(0).getName(), "attribute1");
        assertEquals(resultUser.getCustomAttributes().get(0).getValue(), "value1");
        assertEquals(resultUser.getCustomAttributes().get(1).getName(), "attribute2");
        assertTrue(resultUser.getCustomAttributes().get(1).getValues().size() == 2);
        assertEquals(resultUser.getCustomAttributes().get(1).getValues().get(0), "value2");
        assertEquals(resultUser.getCustomAttributes().get(1).getValues().get(1), "value3");
    }

    @Test
    private void addUserAttributeByUserInum_existentAttributeExistentValue_user() {
        String inumUser = "inumUser";
        User user = spy(getBasicUser("123", "dn123"));
        user.setDn("dn123");
        CustomObjectAttribute customAttribute1 = new CustomObjectAttribute("attribute1", "value1");
        CustomObjectAttribute customAttribute2 = new CustomObjectAttribute("attribute2", "value2");
        user.setCustomAttributes(new ArrayList<>());
        user.getCustomAttributes().add(customAttribute1);
        user.getCustomAttributes().add(customAttribute2);

        when(persistenceEntryManager.find(anyString(), any(), any())).thenReturn(user);
        doNothing().when(persistenceEntryManager).merge(user);
        User resultUser = userService.addUserAttributeByUserInum(inumUser, "attribute2", "value2");

        assertNotNull(resultUser);
        assertTrue(resultUser.getCustomAttributes().size() == 2);
        assertEquals(resultUser.getCustomAttributes().get(0).getName(), "attribute1");
        assertEquals(resultUser.getCustomAttributes().get(0).getValue(), "value1");
        assertEquals(resultUser.getCustomAttributes().get(1).getName(), "attribute2");
        assertEquals(resultUser.getCustomAttributes().get(1).getValue(), "value2");
        assertTrue(resultUser.getCustomAttributes().get(1).getValues().size() == 1);
    }

    @Test
    private void addUserAttribute_newAttribute_user() {
        String attributeName = "attribute1";
        String attributeValue = "value1";
        User user = spy(getBasicUser("123", "dn123"));
        user.setCustomAttributes(new ArrayList<>());

        Boolean result = userService.addUserAttribute(user, attributeName, attributeValue, null);
        assertTrue(result);
        assertTrue(user.getCustomAttributes().size() == 1);
        assertEquals(user.getCustomAttributes().get(0).getName(), attributeName);
        assertEquals(user.getCustomAttributes().get(0).getValue(), attributeValue);
    }

    @Test
    private void removeUserAttribute_noExistentAttribute_null() {
        String userId = "123";
        String baseDn = "dn123";
        User user = spy(getBasicUser("123", "dn123"));
        CustomObjectAttribute customAttribute1 = new CustomObjectAttribute("attribute1", "value1");
        user.setCustomAttributes(new ArrayList<>());
        user.getCustomAttributes().add(customAttribute1);

        when(dataSourceTypeService.isSpanner(anyString())).thenReturn(true);
        when(persistenceEntryManager.findEntries(anyString(), any(), any(), any())).thenReturn(getListBasicOneUser(userId, baseDn));

        User resultUser = userService.removeUserAttributeValue(userId, "attribute2", "value2");
        assertNull(resultUser);
    }

    @Test
    private void removeUserAttribute_existentAttribute_user() {
        String userId = "123";
        String baseDn = "dn123";
        User user = spy(getBasicUser("123", "dn123"));
        user.setDn(baseDn);
        CustomObjectAttribute customAttribute1 = new CustomObjectAttribute("attribute1", "value1");
        CustomObjectAttribute customAttribute2 = new CustomObjectAttribute("attribute2", "value2");
        user.setCustomAttributes(new ArrayList<>());
        user.getCustomAttributes().add(customAttribute1);
        user.getCustomAttributes().add(customAttribute2);

        when(dataSourceTypeService.isSpanner(anyString())).thenReturn(true);
        when(persistenceEntryManager.findEntries(anyString(), any(), any(), any())).thenReturn(getListBasicOneUser(user));
        when(persistenceEntryManager.find(anyString(), any(), any())).thenReturn(user);
        doNothing().when(persistenceEntryManager).merge(any());

        User resultUser = userService.removeUserAttributeValue(userId, "attribute1", "value1");
        assertNotNull(resultUser);
        assertTrue(resultUser.getCustomAttributes().get(0).getValues().isEmpty());
//        assertTrue(resultUser.getCustomAttributes().size() == 1);
//        assertEquals(resultUser.getCustomAttributes().get(0).getName(), "attribute2");
    }

    @Test
    private void replaceUserAttribute_existentAttribute_user() {
        String userId = "123";
        String baseDn = "dn123";
        User user = spy(getBasicUser("123", "dn123"));
        user.setDn(baseDn);
        CustomObjectAttribute customAttribute1 = new CustomObjectAttribute("attribute1", "value1");
        CustomObjectAttribute customAttribute2 = new CustomObjectAttribute("attribute2", "value2");
        user.setCustomAttributes(new ArrayList<>());
        user.getCustomAttributes().add(customAttribute1);
        user.getCustomAttributes().add(customAttribute2);

        when(dataSourceTypeService.isSpanner(anyString())).thenReturn(true);
        when(persistenceEntryManager.findEntries(anyString(), any(), any(), any())).thenReturn(getListBasicOneUser(user));
        when(persistenceEntryManager.find(anyString(), any(), any())).thenReturn(user);
        doNothing().when(persistenceEntryManager).merge(any());

        User resultUser = userService.replaceUserAttribute(userId, "attribute1", "value1", "newValue1");
        assertNotNull(resultUser);
        assertEquals(resultUser.getCustomAttributes().get(0).getValue(), "newValue1");
    }

    @Test
    private void getCustomAttribute_noExistentAttribute_null() {
        User user = spy(getBasicUser("123", "dn123"));
        user.setCustomAttributes(new ArrayList<>());

        CustomObjectAttribute resultAttribute = userService.getCustomAttribute(user, "attribute1");
        assertNull(resultAttribute);
    }

    @Test
    private void getCustomAttribute_existentAttribute_user() {

        User user = spy(getBasicUser("123", "dn123"));
        CustomObjectAttribute customAttribute1 = new CustomObjectAttribute("attribute1", "value1");
        CustomObjectAttribute customAttribute2 = new CustomObjectAttribute("attribute2", "value2");
        user.setCustomAttributes(new ArrayList<>());
        user.getCustomAttributes().add(customAttribute1);
        user.getCustomAttributes().add(customAttribute2);

        CustomObjectAttribute resultAttribute = userService.getCustomAttribute(user, "attribute1");
        assertNotNull(resultAttribute);
        assertEquals(resultAttribute.getName(), "attribute1");
        assertEquals(resultAttribute.getValue(), "value1");
    }

    @Test
    private void setCustomAttribute_noExistentAttribute_user() {

        User user = spy(getBasicUser("123", "dn123"));
        user.setCustomAttributes(new ArrayList<>());

        userService.setCustomAttribute(user, "attribute1", "value1");
        assertTrue(user.getCustomAttributes().size() == 1);
        assertEquals(user.getCustomAttributes().get(0).getName(), "attribute1");
        assertEquals(user.getCustomAttributes().get(0).getValue(), "value1");
    }

    @Test
    private void getUsersWithPersistentJwts_noParams_listUsers() {
        String userId = "123";
        String baseDn = "dn123";
        when(persistenceEntryManager.findEntries(anyString(), any(), any())).thenReturn(getListBasicOneUser(userId, baseDn));

        List<User> list = userService.getUsersWithPersistentJwts();
        assertNotNull(list);
        assertTrue(list.size() == 1);
    }

    @Test
    private void getDnForUser_emptyInum_dnForUser() {
        String inum = "";
        String result = userService.getDnForUser(inum);
        assertNotNull(result);
        assertEquals(result, "baseDnTest");
    }

    @Test
    private void getDnForUser_validInum_dnForUser() {
        String inum = "inumTest";
        String result = userService.getDnForUser(inum);
        assertNotNull(result);
        assertEquals(result, "inum=inumTest,baseDnTest");
    }

    @Test
    private void getUserInumByDn_emptyDn_null() {
        String dn = "";
        String result = userService.getUserInumByDn(dn);
        assertNull(result);
    }

    @Test
    private void getUserInumByDn_dnNotEndsWithPeopleBaseDn_null() {
        String dn = "123OtherDnTest";
        String result = userService.getUserInumByDn(dn);
        assertNull(result);
    }

    private void getUserInumByDn_validDn_inum() {
        String inum = "inumTest1";
        String peopleBaseDn = userService.getPeopleBaseDn();
        String dn = "inum=" + inum + "," + peopleBaseDn;
        String result = userService.getUserInumByDn(dn);
        assertNotNull(result);
        assertEquals(result, "inumTest1");
    }

    private <T> List<T> getListBasicOneUser(User user) {
        List<User> list = new ArrayList<>();
        list.add(user);
        return (List<T>) list;
    }

    private <T> List<T> getListBasicOneUser(String userId, String baseDn) {
        List<User> list = new ArrayList<>();
        list.add(getBasicUser(userId, baseDn));
        return (List<T>) list;
    }

    private User getBasicUser(String uerId, String baseDn) {
        User user1 = new User();
        user1.setUserId(uerId);
        user1.setCreatedAt(new Date());
        user1.setBaseDn(baseDn);
        return user1;
    }

}
