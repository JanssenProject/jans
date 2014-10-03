package org.xdi.oxd.license.admin.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import org.xdi.oxd.license.admin.client.service.AdminService;
import org.xdi.oxd.license.admin.client.service.AdminServiceAsync;
import org.xdi.oxd.license.admin.client.ui.MainPanelPresenter;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/09/2014
 */

public class Admin implements EntryPoint {

    private static final EventBus EVENT_BUS = new SimpleEventBus();

    private static final AdminServiceAsync SERVICE = GWT.create(AdminService.class);

    public static SafeHtml asHtml(String str) {
        String s = str != null ? str : "";
        if (s.length() > 40) {
            s = s.substring(0, 40) + "...";
        }
        return SafeHtmlUtils.fromString(s);
    }

    @Override
    public void onModuleLoad() {
        MainPanelPresenter mainPanelPresenter = new MainPanelPresenter();
        mainPanelPresenter.go(RootLayoutPanel.get());
    }

    public static AdminServiceAsync getService() {
        return SERVICE;
    }

    public static EventBus getEventBus() {
        return EVENT_BUS;
    }

    public static boolean isEmpty(String str) {
        return str == null || str.trim().equals("");
    }

}
