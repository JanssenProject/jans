package org.xdi.oxd.license.admin.client.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.xdi.oxd.license.admin.client.Admin;
import org.xdi.oxd.license.admin.client.SuccessCallback;
import org.xdi.oxd.license.admin.client.dialogs.AddLicenseCryptDialog;
import org.xdi.oxd.license.client.js.LdapLicenseCrypt;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 03/10/2014
 */

public class LicenseCryptTabPresenter {

    private final SingleSelectionModel<LdapLicenseCrypt> selectionModel = new SingleSelectionModel<LdapLicenseCrypt>();
    private final LicenseCryptDetailsPresenter detailsPresenter;
    private LicenseCryptTab view;

    public LicenseCryptTabPresenter(LicenseCryptTab view) {
        this.view = view;
        this.detailsPresenter = new LicenseCryptDetailsPresenter(view.getDetailsPanel());
        this.view.getAddButton().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                AddLicenseCryptDialog dialog = new AddLicenseCryptDialog(null) {
                    @Override
                    public void onSuccess() {
                        loadTableData();
                    }
                };
                dialog.show();
            }
        });
        this.view.getEditButton().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                AddLicenseCryptDialog dialog = new AddLicenseCryptDialog(selectionModel.getSelectedObject()) {
                    @Override
                    public void onSuccess() {
                        loadTableData();
                    }
                };
                dialog.show();
            }
        });
        this.view.getRemoveButton().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                removeEntity();
            }
        });
        this.view.getRefreshButton().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                loadTableData();
            }
        });
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                setButtonsState();
                LicenseCryptTabPresenter.this.detailsPresenter.show(selectionModel.getSelectedObject());
            }
        });

        view.getTable().setEmptyTableWidget(new Label("No data"));
        view.getTable().setSelectionModel(selectionModel);

        loadTableData();

    }

    private void setButtonsState() {
        boolean enabled = selectionModel.getSelectedObject() != null;
        this.view.getEditButton().setEnabled(enabled);
        this.view.getRemoveButton().setEnabled(enabled);
    }

    private void removeEntity() {
        Admin.getService().remove(selectionModel.getSelectedObject(), new SuccessCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                loadTableData();
            }
        });
    }

    private void loadTableData() {
        Admin.getService().getAllLicenseCrypts(new SuccessCallback<List<LdapLicenseCrypt>>() {
            @Override
            public void onSuccess(List<LdapLicenseCrypt> result) {
                view.getTable().setRowData(result);
                view.getTable().setRowCount(result.size());
            }
        });
    }
}
