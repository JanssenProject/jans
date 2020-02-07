package org.gluu.oxauth.session.ws.rs;

import org.gluu.oxauth.model.util.Util;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.gluu.oxauth.util.ServerUtil.daemonThreadFactory;

/**
 * @author Yuriy Zabrovarnyy
 */
public class EndSessionUtils {

    private static final int MAX_NUMBER_OF_THREADS_FOR_BACKCHANNEL_CALLS = 5;


    private EndSessionUtils() {
    }

    public static ExecutorService getExecutorService(int requestedSize) {
        int maxNumOfThreadPool = requestedSize >= MAX_NUMBER_OF_THREADS_FOR_BACKCHANNEL_CALLS ? MAX_NUMBER_OF_THREADS_FOR_BACKCHANNEL_CALLS : requestedSize;
        return Executors.newFixedThreadPool(maxNumOfThreadPool, daemonThreadFactory());
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
