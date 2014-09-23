package org.xdi.oxd.license.admin.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import org.xdi.oxd.license.admin.client.service.AdminService;
import org.xdi.oxd.license.admin.client.service.AdminServiceAsync;
import org.xdi.oxd.license.admin.client.ui.MainPanel;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/09/2014
 */

public class Admin implements EntryPoint {

    private static final EventBus EVENT_BUS = new SimpleEventBus();

    private static final AdminServiceAsync SERVICE = GWT.create(AdminService.class);

    @Override
    public void onModuleLoad() {
        RootLayoutPanel.get().add(new MainPanel());
    }

    public static AdminServiceAsync getService() {
        return SERVICE;
    }

    public static EventBus getEventBus() {
        return EVENT_BUS;
    }
}
