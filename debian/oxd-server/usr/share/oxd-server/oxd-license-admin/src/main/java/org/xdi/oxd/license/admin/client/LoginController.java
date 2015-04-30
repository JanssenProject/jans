package org.xdi.oxd.license.admin.client;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.xdi.oxd.license.client.js.Configuration;

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
        final String idTokenParameter = HashParser.getIdTokenFromHash(Window.Location.getHash());
        LOGGER.fine("idTokenParameter=" + idTokenParameter);
        if (!Admin.isEmpty(idTokenParameter)) {
            ID_TOKEN = idTokenParameter;
            return true;
        }
        Scheduler.get().scheduleFixedPeriod(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                redirectToLoginPage();
                return false;
            }
        }, 500);

        return false;
    }

    private static void redirectToLoginPage() {
        Admin.getService().getConfiguration(new AsyncCallback<Configuration>() {
            @Override
            public void onFailure(Throwable caught) {
                LOGGER.log(Level.SEVERE, caught.getMessage(), caught);
            }

            @Override
            public void onSuccess(Configuration result) {
                LOGGER.fine("Redirect to:" + result.getAuthorizeRequest());
                Window.Location.assign(result.getAuthorizeRequest());
            }
        });
    }

    public static boolean logout() {
        ID_TOKEN = null;

        Admin.getService().getConfiguration(new AsyncCallback<Configuration>() {
            @Override
            public void onFailure(Throwable caught) {
                LOGGER.log(Level.SEVERE, caught.getMessage(), caught);
                redirectToLoginPage();
            }

            @Override
            public void onSuccess(Configuration result) {
                String url = result.getLogoutUrl() + ID_TOKEN;
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
