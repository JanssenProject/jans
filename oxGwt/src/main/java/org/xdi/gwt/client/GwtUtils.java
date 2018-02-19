/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.gwt.client;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Widget;
import com.sencha.gxt.widget.core.client.box.MessageBox;
import com.sencha.gxt.widget.core.client.form.FieldLabel;

import java.util.Date;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 20/05/2013
 */

public final class GwtUtils {

    /**
     * Avoid initialization
     */
    private GwtUtils() {
    }

    public static FieldLabel createFieldLabel(Widget widget, String label) {
        final FieldLabel f = new FieldLabel(widget, label);
        f.setLabelSeparator("");
        return f;
    }

    public static void showInformation(String message) {
        final MessageBox d = new MessageBox("Information", message);
        d.setIcon(MessageBox.ICONS.info());
        d.setHideOnButtonClick(true);
        d.show();
    }

    public static void showError(String message) {
        final MessageBox d = new MessageBox("Error", message);
        d.setIcon(MessageBox.ICONS.error());
        d.setHideOnButtonClick(true);
        d.show();
    }

    public static boolean contains(String string, String toSearch) {
        return !isEmpty(string) && !isEmpty(toSearch) && string.contains(toSearch);
    }

    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static String asString(Date date) {
        return date != null ? getDateTimeFormat().format(date) : "";
    }

    public static DateTimeFormat getDateTimeFormat() {
        return DateTimeFormat.getFormat("HH:mm:ss dd.MM.yyyy");
    }
}

