package org.xdi.oxd.license.admin.client.dialogs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.xdi.oxd.license.admin.client.Admin;
import org.xdi.oxd.license.admin.client.SuccessCallback;
import org.xdi.oxd.license.admin.client.framework.Framework;
import org.xdi.oxd.license.client.js.LdapLicenseCrypt;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 03/10/2014
 */

public class SelectCryptDialog {

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    interface MyUiBinder extends UiBinder<Widget, SelectCryptDialog> {
    }

    private final SingleSelectionModel<LdapLicenseCrypt> selectionModel = new SingleSelectionModel<LdapLicenseCrypt>();
    private final DialogBox dialog;

    @UiField
    VerticalPanel dialogContent;
    @UiField
    CellTable<LdapLicenseCrypt> table;
    @UiField
    HTML errorMessage;
    @UiField
    Button okButton;
    @UiField
    Button closeButton;

    private LdapLicenseCrypt ldapLicenseCrypt;

    public SelectCryptDialog() {
        uiBinder.createAndBindUi(this);

        dialog = Framework.createDialogBox("Select crypt");
        dialog.setWidget(dialogContent);

        table.setEmptyTableWidget(new Label("No data"));
        table.setSelectionModel(selectionModel);
        table.addColumn(new TextColumn<LdapLicenseCrypt>() {
            @Override
            public String getValue(LdapLicenseCrypt object) {
                return object.getName();
            }
        }, "Name");

        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                ldapLicenseCrypt = selectionModel.getSelectedObject();
                okButton.setEnabled(ldapLicenseCrypt != null);
            }
        });

        closeButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                dialog.hide();
            }
        });
        okButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (ldapLicenseCrypt != null) {
                    dialog.hide();
                    onOk();
                }
            }
        });


        loadTableData();
    }

    public void onOk() {
    }

    public LdapLicenseCrypt getLdapLicenseCrypt() {
        return ldapLicenseCrypt;
    }

    private void loadTableData() {
        Admin.getService().getAllLicenseCryptObjects(new SuccessCallback<List<LdapLicenseCrypt>>() {
            @Override
            public void onSuccess(List<LdapLicenseCrypt> result) {
                table.setRowData(result);
                table.setRowCount(result.size());
            }
        });
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
