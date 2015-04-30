package org.xdi.oxd.license.admin.client.dialogs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.xdi.oxd.license.admin.client.framework.Framework;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 19/10/2014
 */

public class TextAreaDialog {

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    interface MyUiBinder extends UiBinder<Widget, TextAreaDialog> {
    }

    private final DialogBox dialog;

    @UiField
    VerticalPanel dialogContent;
    @UiField
    Button okButton;
    @UiField
    Button closeButton;
    @UiField
    TextArea textArea;

    public TextAreaDialog() {
        uiBinder.createAndBindUi(this);

        dialog = Framework.createDialogBox("");
        dialog.setWidget(dialogContent);

        closeButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                dialog.hide();
            }
        });
        okButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                dialog.hide();
                onOk();
            }
        });
    }

    public TextArea getTextArea() {
        return textArea;
    }

    public void onOk() {
    }

    public void setTitle(String title) {
        dialog.setText(title);
        dialog.setTitle(title);
    }

    public void show() {
        dialog.center();
        dialog.show();
    }
}
