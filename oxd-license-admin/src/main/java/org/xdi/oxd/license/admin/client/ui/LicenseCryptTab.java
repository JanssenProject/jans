package org.xdi.oxd.license.admin.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import org.xdi.oxd.license.client.js.LdapLicenseCrypt;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/10/2014
 */

public class LicenseCryptTab implements IsWidget {

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    @UiField
    DockLayoutPanel rootPanel;
    @UiField
    Button addButton;
    @UiField
    Button editButton;
    @UiField
    Button removeButton;
    @UiField
    Button refreshButton;
    @UiField
    CellTable<LdapLicenseCrypt> table;
    @UiField
    LicenseCryptDetailsPanel detailsPanel;

    interface MyUiBinder extends UiBinder<Widget, LicenseCryptTab> {
    }

    public LicenseCryptTab() {
        uiBinder.createAndBindUi(this);
        rootPanel.getWidgetContainerElement(detailsPanel.asWidget()).getStyle().setOverflowY(Style.Overflow.AUTO);
        table.addColumn(new TextColumn<LdapLicenseCrypt>() {
            @Override
            public String getValue(LdapLicenseCrypt object) {
                return object.getName();
            }
        }, "Name");
    }

    public Button getAddButton() {
        return addButton;
    }

    public LicenseCryptDetailsPanel getDetailsPanel() {
        return detailsPanel;
    }

    public Button getEditButton() {
        return editButton;
    }

    public Button getRefreshButton() {
        return refreshButton;
    }

    public Button getRemoveButton() {
        return removeButton;
    }

    public DockLayoutPanel getRootPanel() {
        return rootPanel;
    }

    public CellTable<LdapLicenseCrypt> getTable() {
        return table;
    }

    @Override
    public Widget asWidget() {
        return rootPanel;
    }
}
