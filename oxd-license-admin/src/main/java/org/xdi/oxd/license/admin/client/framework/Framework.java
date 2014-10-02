package org.xdi.oxd.license.admin.client.framework;

import com.google.gwt.user.client.ui.DialogBox;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/10/2014
 */

public class Framework {

    private Framework() {
    }


    public static DialogBox createDialogBox(String title) {
        DialogBox dialog = new DialogBox();
        dialog.setGlassEnabled(true);
        dialog.setText(title);
        dialog.setTitle(title);
        dialog.setAnimationEnabled(true);
        return dialog;
    }
}
