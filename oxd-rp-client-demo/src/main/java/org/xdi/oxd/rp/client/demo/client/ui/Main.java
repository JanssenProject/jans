package org.xdi.oxd.rp.client.demo.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import org.xdi.oxd.rp.client.demo.client.Demo;
import org.xdi.oxd.rp.client.demo.client.LoginController;
import org.xdi.oxd.rp.client.demo.client.event.LoginEvent;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 19/10/2015
 */

public class Main extends Composite {

    private static MainUiBinder uiBinder = GWT.create(MainUiBinder.class);
    @UiField
    Button logoutButton;
    @UiField
    HTMLPanel loggedInPanel;
    @UiField
    Button loginButton;
    @UiField
    HTMLPanel notLoggedInPanel;
    @UiField
    HTMLPanel rootPanel;
    @UiField
    Button showUserInfoButton;

    interface MainUiBinder extends UiBinder<Widget, Main> {
    }

    public Main() {
        initWidget(uiBinder.createAndBindUi(this));

        setState();
        Demo.getEventBus().addHandler(LoginEvent.TYPE, new LoginEvent.Handler() {
            @Override
            public void update(LoginEvent p_event) {
                setState();
            }
        });
        buttonHandlers();
    }

    private void buttonHandlers() {
        loginButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                LoginController.login();
            }
        });
        showUserInfoButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                showUserInfo();
            }
        });
    }

    private void showUserInfo() {
        Window.alert("Not implemented yet!");
    }

    private void setState() {
        final boolean isLoggedIn = LoginController.hasAccessToken();
        notLoggedInPanel.setVisible(!isLoggedIn);
        loggedInPanel.setVisible(isLoggedIn);
    }

}
