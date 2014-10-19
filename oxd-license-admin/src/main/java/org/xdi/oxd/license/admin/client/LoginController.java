package org.xdi.oxd.license.admin.client;

import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 19/10/2014
 */

public class LoginController {

    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());

    private static String ID_TOKEN = null;

    public static boolean login() {
        if (isLoggedIn()) {
            LOGGER.fine("Already logged in.");
            return true;
        }
        final String idTokenParameter = Window.Location.getParameter("id_token");
        if (!Admin.isEmpty(idTokenParameter)) {
            ID_TOKEN = idTokenParameter;
            return true;
        }
        redirectToLoginPage();
        return false;
    }

    private static void redirectToLoginPage() {
        Admin.getService().getLoginUrl(new AsyncCallback<String>() {
            @Override
            public void onFailure(Throwable caught) {
                LOGGER.log(Level.SEVERE, caught.getMessage(), caught);
            }

            @Override
            public void onSuccess(String result) {
                LOGGER.fine("Redirect to:" + result);
                Window.Location.assign(result);
            }
        });
    }

    public static boolean logout() {
        ID_TOKEN = null;
        Admin.getService().getLogoutUrl(new AsyncCallback<String>() {
            @Override
            public void onFailure(Throwable caught) {
                LOGGER.log(Level.SEVERE, caught.getMessage(), caught);
                redirectToLoginPage();
            }

            @Override
            public void onSuccess(String result) {
                String url = result + ID_TOKEN;
                LOGGER.fine("Call end session url: " + url);
                RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, url);
                try {
                    builder.send();
                } catch (RequestException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
                redirectToLoginPage();
            }
        });
        return true;
    }

    public static boolean isLoggedIn() {
        return !Admin.isEmpty(ID_TOKEN);
    }
}
