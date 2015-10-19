package org.xdi.oxd.rp.client.demo.client;

import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.xdi.oxd.rp.client.demo.client.event.LoginEvent;

import java.util.Date;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 19/10/2015
 */

public class LoginController {

    /**
     * Access token cookie name.
     */
    public static final String ACCESS_TOKEN_COOKIE_NAME = "oxd.rp.access_token";

    /**
     * One day
     */
    public static long ONE_DAY_IN_MILIS = (long) 1000.0 * 60 * 60 * 24;

    private LoginController() {
    }


    /**
     * Sets login cookie.
     *
     * @param accessToken access token
     */
    public static void setLoginCookie(String accessToken) {
        final long tomorrowMilis = new Date().getTime() + ONE_DAY_IN_MILIS;
        Cookies.setCookie(ACCESS_TOKEN_COOKIE_NAME, accessToken, new Date(tomorrowMilis));
    }

    public static void login() {
        if (!hasAccessToken()) {
            final String accessToken = parseCode();
            if (accessToken != null && accessToken.length() > 0 && !LoginController.hasAccessToken()) {
                LoginController.setLoginCookie(accessToken);
                Demo.getEventBus().fireEvent(new LoginEvent(true));
                return;
            }

            Demo.getService().getAuthorizationUrl(new AsyncCallback<String>() {
                public void onFailure(Throwable caught) {
                    GwtUtils.showError("Unable to login.");
                }

                public void onSuccess(String result) {
                    if (result != null && result.length() > 0) {
                        Window.Location.assign(result);
                    } else {
                        GwtUtils.showInformation("Unable to login.");
                    }
                }
            });
        }
    }

    private static String parseCode() {
        String hashString = Window.Location.getHash();

        if (hashString != null && hashString.trim().length() > 0) {
            if (hashString.startsWith("#")) {
                hashString = hashString.substring(1); // cut # sign
            }
            final String[] split = hashString.split("&");
            if (split != null && split.length > 0) {
                for (String keyValue : split) {
                    final String[] splitKeyValue = keyValue.split("=");
                    if (splitKeyValue != null && splitKeyValue.length == 2 && splitKeyValue[0].trim().equals("access_token")) {
                        return splitKeyValue[1];
                    }
                }
            }
        }
        return "";
    }

    public static String getAccessToken() {
        return Cookies.getCookie(ACCESS_TOKEN_COOKIE_NAME);
    }

    /**
     * Returns whether access token is saved in cookie.
     *
     * @return whether access token is saved in cookie
     */
    public static boolean hasAccessToken() {
        final String token = Cookies.getCookie(ACCESS_TOKEN_COOKIE_NAME);
        return token != null && token.length() > 0;
    }

    public static void logout() {
        Cookies.removeCookie(ACCESS_TOKEN_COOKIE_NAME);
        Demo.getEventBus().fireEvent(new LoginEvent(false));
    }

}
