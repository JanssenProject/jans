package org.xdi.oxd.rp.client.demo.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sencha.gxt.widget.core.client.container.MarginData;
import com.sencha.gxt.widget.core.client.container.Viewport;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 19/10/2015
 */

public class Demo implements EntryPoint {

    private static final ServiceAsync SERVICE = GWT.create(Service.class);

    private static final EventBus EVENT_BUS = new SimpleEventBus();

    @Override
    public void onModuleLoad() {
        final Viewport viewport = new Viewport();
        viewport.add(createContent(), new MarginData(0));
        RootLayoutPanel.get().add(viewport);

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                refresh();
            }
        });
    }

    private void refresh() {
    }

    private Widget createContent() {
        return new HTML("Hi-ho!");
    }

    public static ServiceAsync getService() {
        return SERVICE;
    }

    public static EventBus getEventBus() {
        return EVENT_BUS;
    }
}
