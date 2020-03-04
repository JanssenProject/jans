package org.gluu.oxauth.session.ws.rs;

import org.gluu.oxauth.client.service.ClientFactory;
import org.gluu.oxauth.model.util.Util;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.gluu.oxauth.util.ServerUtil.daemonThreadFactory;

/**
 * @author Yuriy Zabrovarnyy
 */
public class EndSessionUtils {

    private final static Logger log = LoggerFactory.getLogger(EndSessionUtils.class);

    private EndSessionUtils() {
    }

    public static ExecutorService getExecutorService() {
        return Executors.newCachedThreadPool(daemonThreadFactory());
    }

    public static void callRpWithBackchannelUri(final String backchannelLogoutUri, String logoutToken) {
        javax.ws.rs.client.Client client = new ResteasyClientBuilder().httpEngine(ClientFactory.instance().createEngine(true)).build();
        WebTarget target = client.target(backchannelLogoutUri);

        log.debug("Calling RP with backchannel, backchannel_logout_uri: " + backchannelLogoutUri);
        try (Response response = target.request().post(Entity.form(new Form("logout_token", logoutToken)))) {
            log.debug("Backchannel RP response, status: " + response.getStatus() + ", backchannel_logout_uri" + backchannelLogoutUri);
        } catch (Exception e) {
            log.error("Failed to call backchannel_logout_uri" + backchannelLogoutUri + ", message: " + e.getMessage(), e);
        }
    }

    public static String appendSid(String logoutUri, String sid, boolean appendSid) {
        if (!appendSid) {
            return logoutUri;
        }

        if (logoutUri.contains("?")) {
            return logoutUri + "&sid=" + sid;
        } else {
            return logoutUri + "?sid=" + sid;
        }
    }

    public static String createFronthannelHtml(Set<String> logoutUris, String postLogoutUrl, String state) {
        String iframes = "";
        for (String logoutUri : logoutUris) {
            iframes = iframes + String.format("<iframe height=\"0\" width=\"0\" src=\"%s\" sandbox=\"allow-same-origin allow-scripts allow-popups allow-forms\"></iframe>", logoutUri);
        }

        String html = "<!DOCTYPE html>" +
                "<html>" +
                "<head>";

        if (!Util.isNullOrEmpty(postLogoutUrl)) {

            if (!Util.isNullOrEmpty(state)) {
                if (postLogoutUrl.contains("?")) {
                    postLogoutUrl += "&state=" + state;
                } else {
                    postLogoutUrl += "?state=" + state;
                }
            }

            html += "<script>" +
                    "window.onload=function() {" +
                    "window.location='" + postLogoutUrl + "'" +
                    "}" +
                    "</script>";
        }

        html += "<title>Gluu Generated logout page</title>" +
                "</head>" +
                "<body>" +
                "Logout requests sent.<br/>" +
                iframes +
                "</body>" +
                "</html>";
        return html;
    }

}
