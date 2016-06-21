package org.xdi.oxd.client;

import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.ResponseStatus;
import org.xdi.oxd.common.params.RpAuthorizeRptParams;
import org.xdi.oxd.common.params.RpGetRptParams;
import org.xdi.oxd.common.params.RsCheckAccessParams;
import org.xdi.oxd.common.params.RsProtectParams;
import org.xdi.oxd.common.response.RegisterSiteResponse;
import org.xdi.oxd.common.response.RpAuthorizeRptResponse;
import org.xdi.oxd.common.response.RpGetRptResponse;
import org.xdi.oxd.common.response.RsCheckAccessResponse;
import org.xdi.oxd.common.response.RsProtectResponse;
import org.xdi.oxd.rs.protect.Jackson;
import org.xdi.oxd.rs.protect.RsResourceList;

import java.io.IOException;

import static junit.framework.Assert.*;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/06/2016
 */

public class UmaFullTest {

    private RegisterSiteResponse site;
    private CommandClient client;

    @Parameters({"host", "port", "redirectUrl", "opHost", "rsProtect"})
    @Test
    public void test(String host, int port, String redirectUrl, String opHost, String rsProtect) throws Exception {
        this.client = null;
        try {
            this.client = new CommandClient(host, port);

            site = RegisterSiteTest.registerSite(this.client, opHost, redirectUrl);

            //{'resources':[
            // {'path':'/ws/phone','conditions':[
            // {'httpMethods':['GET'],'scopes':['http://photoz.example.com/dev/actions/all','http://photoz.example.com/dev/actions/view'],'ticketScopes':['http://photoz.example.com/dev/actions/view']},
            // {'httpMethods':['PUT', 'POST'],'scopes':['http://photoz.example.com/dev/actions/all','http://photoz.example.com/dev/actions/add'],'ticketScopes':['http://photoz.example.com/dev/actions/add']},
            // {'httpMethods':['DELETE'],'scopes':['http://photoz.example.com/dev/actions/all','http://photoz.example.com/dev/actions/remove'],'ticketScopes':['http://photoz.example.com/dev/actions/remove']}]}]}
            protect(rsProtect);

            RsCheckAccessResponse accessResponse = checkAccess("", "/ws/phone", "GET");
            String ticket = accessResponse.getTicket();

            assertEquals(accessResponse.getAccess(), "denied");
            assertTrue(!Strings.isNullOrEmpty(ticket));

            String rpt = obtainRpt();

            accessResponse = checkAccess(rpt, "/ws/phone", "GET");
            assertEquals(accessResponse.getAccess(), "denied");

            authorizeRpt(rpt, ticket);

            accessResponse = checkAccess(rpt, "/ws/phone", "GET");
            assertEquals(accessResponse.getAccess(), "granted");

            assertNotProtectedError(rpt);
        } finally {
            CommandClient.closeQuietly(this.client);
        }
    }

    private void assertNotProtectedError(String rpt) {
        RsCheckAccessParams params = new RsCheckAccessParams();
               params.setOxdId(site.getOxdId());
               params.setPath("/no/such/path");
               params.setRpt(rpt);
               params.setHttpMethod("GET");
        CommandResponse response = client.send(new Command(CommandType.RS_CHECK_ACCESS, params));

        assertEquals(response.getStatus(), ResponseStatus.ERROR);
        assertEquals(response.getData().get("error").asText(), "invalid_request");
    }

    private void authorizeRpt(String rpt, String ticket) {
        final RpAuthorizeRptParams params = new RpAuthorizeRptParams();
        params.setOxdId(site.getOxdId());
        params.setRpt(rpt);
        params.setTicket(ticket);

        final RpAuthorizeRptResponse resp = client.send(new Command(CommandType.RP_AUTHORIZE_RPT, params)).dataAsResponse(RpAuthorizeRptResponse.class);
        assertNotNull(resp);
        assertNotNull(resp.getOxdId());
    }

    private void protect(String rsProtect) throws IOException {
        final RsProtectParams commandParams = new RsProtectParams();
        commandParams.setOxdId(site.getOxdId());
        commandParams.setResources(resourceList(rsProtect).getResources());

        final RsProtectResponse resp = client.send(new Command(CommandType.RS_PROTECT, commandParams)).dataAsResponse(RsProtectResponse.class);
        assertNotNull(resp);
    }

    private String obtainRpt() {
        final RpGetRptParams params = new RpGetRptParams();
        params.setOxdId(site.getOxdId());

        final RpGetRptResponse resp = client.send(new Command(CommandType.RP_GET_RPT, params)).dataAsResponse(RpGetRptResponse.class);
        assertNotNull(resp);
        assertTrue(!Strings.isNullOrEmpty(resp.getRpt()));
        return resp.getRpt();
    }

    private RsCheckAccessResponse checkAccess(String rpt, String path, String httpMethod) {
        RsCheckAccessParams params = new RsCheckAccessParams();
        params.setOxdId(site.getOxdId());
        params.setPath(path);
        params.setRpt(rpt);
        params.setHttpMethod(httpMethod);
        return checkAccess(params);
    }

    private RsCheckAccessResponse checkAccess(RsCheckAccessParams params) {
        final RsCheckAccessResponse resp = client.send(new Command(CommandType.RS_CHECK_ACCESS, params)).dataAsResponse(RsCheckAccessResponse.class);
        assertNotNull(resp);
        return resp;
    }

    public static RsResourceList resourceList(String rsProtect) throws IOException {
        rsProtect = StringUtils.replace(rsProtect, "'", "\"");
        return Jackson.createJsonMapper().readValue(rsProtect, RsResourceList.class);
    }
}
