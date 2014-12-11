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

public class GwtUtils {

    /**
     * Avoid initialization
     */
    private GwtUtils() {
    }

    public static FieldLabel createFieldLabel(Widget p_widget, String p_label) {
        final FieldLabel f = new FieldLabel(p_widget, p_label);
        f.setLabelSeparator("");
        return f;
    }

    public static void showInformation(String p_message) {
        final MessageBox d = new MessageBox("Information", p_message);
        d.setIcon(MessageBox.ICONS.info());
        d.setHideOnButtonClick(true);
        d.show();
    }

    public static void showError(String p_message) {
        final MessageBox d = new MessageBox("Error", p_message);
        d.setIcon(MessageBox.ICONS.error());
        d.setHideOnButtonClick(true);
        d.show();
    }

    public static boolean contains(String p_string, String p_toSearch) {
        return !isEmpty(p_string) && !isEmpty(p_toSearch) && p_string.contains(p_toSearch);
    }

    public static boolean isEmpty(String p_str) {
        return p_str == null || p_str.length() == 0;
    }

    public static String asString(Date p_date) {
        return p_date != null ? getDateTimeFormat().format(p_date) : "";
    }

    public static DateTimeFormat getDateTimeFormat() {
        return DateTimeFormat.getFormat("HH:mm:ss dd.MM.yyyy");
    }
}
