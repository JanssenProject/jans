/*
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.gluu.oxd.common;

import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/08/2013
 */

public class JsonTest {

    @Test
    public void testCommandType() throws IOException {
        final String json = CoreUtils.asJson(CommandType.GET_AUTHORIZATION_URL);
        Assert.assertEquals(json, "\"obtain_pat\"");
        final CommandType obtainPat = CoreUtils.createJsonMapper().readValue(json, CommandType.class);
        Assert.assertNotNull(obtainPat);
    }

    @Test
    public void testCommand() throws IOException {
        Command c = new Command();
        c.setCommandType(CommandType.GET_USER_INFO);
        c.setParams(JsonNodeFactory.instance.textNode("myParams"));

        final String cJson = CoreUtils.asJson(c);
        Assert.assertTrue(StringUtils.isNotBlank(cJson));

        final String json = "{\"command\":\"register_client\",\"params\": {\"discovery_url\":\"<discovery url>\",\n" +
                "        \"redirect_url\":\"<redirect url>\",\n" +
                "        \"client_name\":\"<client name>\"\n" +
                "    }\n" +
                "}";
        final Command command = CoreUtils.createJsonMapper().readValue(json, Command.class);
        Assert.assertNotNull(command);
    }

    @Test
    public void testErrorResponseJson() throws IOException {
        final String json = CoreUtils.asJson(new ErrorResponse(ErrorResponseCode.INTERNAL_ERROR_UNKNOWN));
        Assert.assertTrue(StringUtils.isNotBlank(json));
    }
}
