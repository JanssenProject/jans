package org.xdi.oxd.rp.client.demo.client;

import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.xdi.oxd.rp.client.demo.client.event.LoginEvent;
import org.xdi.oxd.rp.client.demo.client.model.LoginType;
import org.xdi.oxd.rp.client.demo.shared.TokenDetails;

import java.util.Date;

/**
 * Just sample, don't make login data static.
 *
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

    private static TokenDetails tokenDetails;

    private LoginController() {
    }

    public static TokenDetails getTokenDetails() {
        return tokenDetails;
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

    public static void login(LoginType loginType) {
        if (!hasAccessToken()) {

            if (hrefHasCodeOrToken()) {
                loadTokenDetails();
            } else {
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
    }

    public static void loadTokenDetails() {
        if (tokenDetails != null && tokenDetails.getAccessToken() != null) {
            return;
        }
        if (!hrefHasCodeOrToken()) {
            return;
        }
        Demo.getService().getTokenDetails(Window.Location.getHref(), new AsyncCallback<TokenDetails>() {
            @Override
            public void onFailure(Throwable throwable) {
                GwtUtils.showError("Failed to get token details. " + throwable.getMessage());
            }

            @Override
            public void onSuccess(TokenDetails tokenDetails) {
                final String accessToken = tokenDetails.getAccessToken();
                if (accessToken != null && accessToken.length() > 0 && !LoginController.hasAccessToken()) {
                    LoginController.tokenDetails = tokenDetails;
                    //LoginController.setLoginCookie(accessToken);
                    Demo.getEventBus().fireEvent(new LoginEvent(tokenDetails));
                }
            }
        });
    }

    private static boolean hrefHasCodeOrToken() {
        final String href = Window.Location.getHref();
        return href.contains("&code=") || href.contains("&access_token=");
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
        LoginController.tokenDetails = null;
        Demo.getEventBus().fireEvent(new LoginEvent(null));
    }

}
