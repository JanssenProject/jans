/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.fido2.model.metric;

import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserMetricsUpdateRequestTest {

    @Test
    void testDefaultConstructorLeavesFieldsUnset() {
        UserMetricsUpdateRequest req = new UserMetricsUpdateRequest();

        assertNull(req.getUserId());
        assertNull(req.getUsername());
        assertFalse(req.isSuccess(), "boolean field must default to false, not auto-initialized");
        assertNull(req.getAuthenticatorType());
        assertNull(req.getDeviceType());
        assertNull(req.getBrowser());
        assertNull(req.getOs());
        assertNull(req.getDurationMs());
        assertNull(req.getIpAddress());
        assertNull(req.getUserAgent());
        assertNull(req.getFallbackMethod());
        assertNull(req.getFallbackReason());
    }

    @Test
    void testThreeArgConstructorWiresUserIdAndUsernameAndSuccess() {
        UserMetricsUpdateRequest req = new UserMetricsUpdateRequest("u-123", "alice", true);

        assertEquals("u-123", req.getUserId(), "first arg must wire to userId (catches userId/username swap)");
        assertEquals("alice", req.getUsername(), "second arg must wire to username (catches userId/username swap)");
        assertTrue(req.isSuccess(), "third arg must wire to success");

        assertNull(req.getAuthenticatorType());
        assertNull(req.getDeviceType());
        assertNull(req.getBrowser());
        assertNull(req.getOs());
        assertNull(req.getDurationMs());
        assertNull(req.getIpAddress());
        assertNull(req.getUserAgent());
        assertNull(req.getFallbackMethod());
        assertNull(req.getFallbackReason());
    }

    @Test
    void testGetterSetterRoundTripCoversEachValueType() {
        UserMetricsUpdateRequest req = new UserMetricsUpdateRequest();

        req.setAuthenticatorType("PLATFORM");
        assertEquals("PLATFORM", req.getAuthenticatorType());

        req.setSuccess(true);
        assertTrue(req.isSuccess());
        req.setSuccess(false);
        assertFalse(req.isSuccess());

        req.setDurationMs(987L);
        assertEquals(987L, req.getDurationMs());
    }

    @Test
    void testIsSuccessUsesBooleanGetterNamingConvention() throws NoSuchMethodException {
        Method isSuccess = UserMetricsUpdateRequest.class.getMethod("isSuccess");
        assertEquals(boolean.class, isSuccess.getReturnType(),
                "isSuccess() must return primitive boolean for JavaBeans/Jackson introspection");

        assertThrows(NoSuchMethodException.class,
                () -> UserMetricsUpdateRequest.class.getMethod("getSuccess"),
                "boolean getter must be named isSuccess, not getSuccess — locks the JavaBeans naming convention");
    }

    @Test
    void testSerializableContractIsIntact() throws NoSuchFieldException, IllegalAccessException {
        assertTrue(Serializable.class.isAssignableFrom(UserMetricsUpdateRequest.class),
                "UserMetricsUpdateRequest must implement Serializable");

        Field serialVersionUID = UserMetricsUpdateRequest.class.getDeclaredField("serialVersionUID");
        int mods = serialVersionUID.getModifiers();
        assertTrue(Modifier.isPrivate(mods), "serialVersionUID must be private");
        assertTrue(Modifier.isStatic(mods), "serialVersionUID must be static");
        assertTrue(Modifier.isFinal(mods), "serialVersionUID must be final");
        assertEquals(long.class, serialVersionUID.getType(), "serialVersionUID must be primitive long");

        serialVersionUID.setAccessible(true);
        assertEquals(1L, serialVersionUID.getLong(null),
                "serialVersionUID must remain 1L — bumping it breaks deserialization of in-flight payloads");
    }
}
