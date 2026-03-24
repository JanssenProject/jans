/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2025, Gluu, Inc.
 */

package io.jans.configapi.test.util;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.IOException;

import org.testng.annotations.Test;

import com.github.fge.jsonpatch.JsonPatchException;

import io.jans.configapi.core.util.Jackson;
import io.jans.model.GluuAttributeUsageType;
import io.jans.model.GluuUserRole;
import io.jans.model.JansAttribute;
import io.jans.model.attribute.AttributeDataType;

public class JacksonPatchTest {

    private static final String USAGE_TYPE_PATCH = "[{\"op\":\"add\",\"path\":\"/usageType\",\"value\":[\"openid\"]}]";

    @Test
    public void patchUsageTypeUsesApiEnumValues() throws JsonPatchException, IOException {
        JansAttribute attribute = new JansAttribute();
        attribute.setName("departmentNumber");
        attribute.setDisplayName("Department");
        attribute.setDescription("Department");
        attribute.setDataType(AttributeDataType.STRING);
        attribute.setEditType(new GluuUserRole[] { GluuUserRole.ADMIN });
        attribute.setViewType(new GluuUserRole[] { GluuUserRole.ADMIN });

        JansAttribute patchedAttribute = Jackson.applyPatch(USAGE_TYPE_PATCH, attribute);

        assertNotNull(patchedAttribute.getUsageType(), "usageType must be set");
        assertEquals(patchedAttribute.getUsageType().length, 1, "usageType must contain one value");
        assertEquals(patchedAttribute.getUsageType()[0], GluuAttributeUsageType.OPENID);
    }
}
