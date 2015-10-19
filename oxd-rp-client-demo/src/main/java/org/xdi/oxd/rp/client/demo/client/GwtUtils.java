package org.xdi.oxd.rp.client.demo.client;

import com.google.gwt.user.client.Window;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 19/10/2015
 */

public class GwtUtils {

    private GwtUtils() {
    }

    public static void showError(String error) {
        Window.alert(error);
    }

    public static void showInformation(String information) {
        Window.alert(information);
    }
}
