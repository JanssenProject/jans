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
 * @version 0.9, 23/09/2014
 */

public class MainPanel implements IsWidget {

//    private static final Logger LOGGER = Logger.getLogger(MainPanel.class.getName());

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    interface MyUiBinder extends UiBinder<DockLayoutPanel, MainPanel> {
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
    DetailsPanel detailsPanel;
    @UiField
    Button editButton;

    public MainPanel() {
        uiBinder.createAndBindUi(this);

        // init table
        table.addColumn(new TextColumn<Customer>() {
            @Override
            public String getValue(Customer object) {
                return object.getName();
            }
        }, "Name");
    }

    @Override
    public Widget asWidget() {
        return rootPanel;
    }

    public Button getAddButton() {
        return addButton;
    }

    public Button getRemoveButton() {
        return removeButton;
    }

    public CellTable<Customer> getTable() {
        return table;
    }

    public DockLayoutPanel getRootPanel() {
        return rootPanel;
    }

    public DetailsPanel getDetailsPanel() {
        return detailsPanel;
    }
}
