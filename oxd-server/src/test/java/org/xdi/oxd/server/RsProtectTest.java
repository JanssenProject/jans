package org.xdi.oxd.server;

import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.ErrorResponse;
import org.xdi.oxd.common.params.RsCheckAccessParams;
import org.xdi.oxd.common.params.RsProtectParams;
import org.xdi.oxd.common.response.RegisterSiteResponse;
import org.xdi.oxd.common.response.RsCheckAccessResponse;
import org.xdi.oxd.common.response.RsProtectResponse;
import org.xdi.oxd.rs.protect.RsResource;

import java.io.IOException;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 10/06/2016
 */

public class RsProtectTest {

    @Parameters({"host", "port", "redirectUrl", "opHost", "rsProtect"})
    @Test
    public void protect(String host, int port, String redirectUrl, String opHost, String rsProtect) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

            protectResources(client, site, UmaFullTest.resourceList(rsProtect).getResources());
            RsCheckAccessTest.checkAccess(client, site);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    @Parameters({"host", "port", "redirectUrl", "opHost", "rsProtect"})
    @Test
    public void overwriteFalse(String host, int port, String redirectUrl, String opHost, String rsProtect) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

            List<RsResource> resources = UmaFullTest.resourceList(rsProtect).getResources();
            protectResources(client, site, resources);

            final RsProtectParams commandParams = new RsProtectParams();
            commandParams.setOxdId(site.getOxdId());
            commandParams.setResources(resources);

            ErrorResponse errorResponse = client.send(new Command(CommandType.RS_PROTECT).setParamsObject(commandParams)).dataAsResponse(ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals(errorResponse.getError(), "uma_protection_exists");
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    @Parameters({"host", "port", "redirectUrl", "opHost", "rsProtect"})
    @Test
    public void overwriteTrue(String host, int port, String redirectUrl, String opHost, String rsProtect) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

            List<RsResource> resources = UmaFullTest.resourceList(rsProtect).getResources();
            protectResources(client, site, resources);

            final RsProtectParams commandParams = new RsProtectParams();
            commandParams.setOxdId(site.getOxdId());
            commandParams.setResources(resources);
            commandParams.setOverwrite(true); // force overwrite

            RsProtectResponse response = client.send(new Command(CommandType.RS_PROTECT).setParamsObject(commandParams)).dataAsResponse(RsProtectResponse.class);
            assertNotNull(response);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    @Parameters({"host", "port", "redirectUrl", "opHost", "rsProtectScopeExpression"})
    @Test
    public void protectWithScopeExpression(String host, int port, String redirectUrl, String opHost, String rsProtectScopeExpression) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

            protectResources(client, site, UmaFullTest.resourceList(rsProtectScopeExpression).getResources());
            RsCheckAccessTest.checkAccess(client, site);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    @Parameters({"host", "port", "redirectUrl", "opHost", "rsProtectScopeExpressionSecond"})
    @Test
    public void protectWithScopeExpressionSeconds(String host, int port, String redirectUrl, String opHost, String rsProtectScopeExpressionSecond) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

            protectResources(client, site, UmaFullTest.resourceList(rsProtectScopeExpressionSecond).getResources());

            final RsCheckAccessParams params = new RsCheckAccessParams();
            params.setOxdId(site.getOxdId());
            params.setHttpMethod("GET");
            params.setPath("/GetAll");
            params.setRpt("");

            final RsCheckAccessResponse response = client
                    .send(new Command(CommandType.RS_CHECK_ACCESS).setParamsObject(params))
                    .dataAsResponse(RsCheckAccessResponse.class);

            Assert.assertNotNull(response);
            Assert.assertTrue(StringUtils.isNotBlank(response.getAccess()));
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public static RsProtectResponse protectResources(CommandClient client, RegisterSiteResponse site, List<RsResource> resources) {
        final RsProtectParams commandParams = new RsProtectParams();
        commandParams.setOxdId(site.getOxdId());
        commandParams.setResources(resources);

        final RsProtectResponse resp = client
                .send(new Command(CommandType.RS_PROTECT).setParamsObject(commandParams))
                .dataAsResponse(RsProtectResponse.class);
        assertNotNull(resp);
        return resp;
    }
}
