package org.xdi.oxd.license.admin.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import org.xdi.oxd.license.admin.shared.Customer;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 01/10/2014
 */

public class CustomerTab implements IsWidget {

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    interface MyUiBinder extends UiBinder<Widget, CustomerTab> {
    }

    @UiField
    DockLayoutPanel rootPanel;
    @UiField
    CellTable<Customer> table;
    @UiField
    Button removeButton;
    @UiField
    Button addButton;
    @UiField
    LicenseCryptDetailsPanel detailsPanel;
    @UiField
    Button editButton;
    @UiField
    Button refreshButton;

    public CustomerTab() {
        uiBinder.createAndBindUi(this);
        // init table
        table.addColumn(new TextColumn<Customer>() {
            @Override
            public String getValue(Customer object) {
                return object.getName();
            }
        }, "Name");
    }

    public Button getAddButton() {
        return addButton;
    }

    public Button getRemoveButton() {
        return removeButton;
    }

    public Button getRefreshButton() {
        return refreshButton;
    }

    public CellTable<Customer> getTable() {
        return table;
    }

    public DockLayoutPanel getRootPanel() {
        return rootPanel;
    }

    public LicenseCryptDetailsPanel getDetailsPanel() {
        return detailsPanel;
    }

    @Override
    public Widget asWidget() {
        return rootPanel;
    }
}
