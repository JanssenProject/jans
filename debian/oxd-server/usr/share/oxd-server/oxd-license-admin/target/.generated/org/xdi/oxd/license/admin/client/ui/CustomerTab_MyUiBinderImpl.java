package org.xdi.oxd.license.admin.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.uibinder.client.UiBinderUtil;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiBinderUtil;
import com.google.gwt.user.client.ui.Widget;

public class CustomerTab_MyUiBinderImpl implements UiBinder<com.google.gwt.user.client.ui.Widget, org.xdi.oxd.license.admin.client.ui.CustomerTab>, org.xdi.oxd.license.admin.client.ui.CustomerTab.MyUiBinder {

  interface Template extends SafeHtmlTemplates {
    @Template("Add")
    SafeHtml html1();
     
    @Template("Edit")
    SafeHtml html2();
     
    @Template("Remove")
    SafeHtml html3();
     
    @Template("Refresh")
    SafeHtml html4();
     
  }

  Template template = GWT.create(Template.class);


  public com.google.gwt.user.client.ui.Widget createAndBindUi(final org.xdi.oxd.license.admin.client.ui.CustomerTab owner) {


    return new Widgets(owner).get_rootPanel();
  }

  /**
   * Encapsulates the access to all inner widgets
   */
  class Widgets {
    private final org.xdi.oxd.license.admin.client.ui.CustomerTab owner;


    public Widgets(final org.xdi.oxd.license.admin.client.ui.CustomerTab owner) {
      this.owner = owner;
    }

    SafeHtml template_html1() {
      return template.html1();
    }
    SafeHtml template_html2() {
      return template.html2();
    }
    SafeHtml template_html3() {
      return template.html3();
    }
    SafeHtml template_html4() {
      return template.html4();
    }

    /**
     * Getter for clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay called 0 times. Type: GENERATED_BUNDLE. Build precedence: 1.
     */
    private org.xdi.oxd.license.admin.client.ui.CustomerTab_MyUiBinderImpl_GenBundle get_clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay() {
      return build_clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay();
    }
    private org.xdi.oxd.license.admin.client.ui.CustomerTab_MyUiBinderImpl_GenBundle build_clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay() {
      // Creation section.
      final org.xdi.oxd.license.admin.client.ui.CustomerTab_MyUiBinderImpl_GenBundle clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay = (org.xdi.oxd.license.admin.client.ui.CustomerTab_MyUiBinderImpl_GenBundle) GWT.create(org.xdi.oxd.license.admin.client.ui.CustomerTab_MyUiBinderImpl_GenBundle.class);
      // Setup section.


      return clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay;
    }

    /**
     * Getter for rootPanel called 1 times. Type: DEFAULT. Build precedence: 1.
     */
    private com.google.gwt.user.client.ui.DockLayoutPanel get_rootPanel() {
      return build_rootPanel();
    }
    private com.google.gwt.user.client.ui.DockLayoutPanel build_rootPanel() {
      // Creation section.
      final com.google.gwt.user.client.ui.DockLayoutPanel rootPanel = new com.google.gwt.user.client.ui.DockLayoutPanel(com.google.gwt.dom.client.Style.Unit.PX);
      // Setup section.
      rootPanel.addNorth(get_f_FlowPanel1(), 46);
      rootPanel.addEast(get_detailsPanel(), 500);
      rootPanel.add(get_table());


      this.owner.rootPanel = rootPanel;

      return rootPanel;
    }

    /**
     * Getter for f_FlowPanel1 called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.FlowPanel get_f_FlowPanel1() {
      return build_f_FlowPanel1();
    }
    private com.google.gwt.user.client.ui.FlowPanel build_f_FlowPanel1() {
      // Creation section.
      final com.google.gwt.user.client.ui.FlowPanel f_FlowPanel1 = (com.google.gwt.user.client.ui.FlowPanel) GWT.create(com.google.gwt.user.client.ui.FlowPanel.class);
      // Setup section.
      f_FlowPanel1.add(get_addButton());
      f_FlowPanel1.add(get_editButton());
      f_FlowPanel1.add(get_removeButton());
      f_FlowPanel1.add(get_refreshButton());


      return f_FlowPanel1;
    }

    /**
     * Getter for addButton called 1 times. Type: DEFAULT. Build precedence: 3.
     */
    private com.google.gwt.user.client.ui.Button get_addButton() {
      return build_addButton();
    }
    private com.google.gwt.user.client.ui.Button build_addButton() {
      // Creation section.
      final com.google.gwt.user.client.ui.Button addButton = (com.google.gwt.user.client.ui.Button) GWT.create(com.google.gwt.user.client.ui.Button.class);
      // Setup section.
      addButton.setHTML(template_html1().asString());


      this.owner.addButton = addButton;

      return addButton;
    }

    /**
     * Getter for editButton called 1 times. Type: DEFAULT. Build precedence: 3.
     */
    private com.google.gwt.user.client.ui.Button get_editButton() {
      return build_editButton();
    }
    private com.google.gwt.user.client.ui.Button build_editButton() {
      // Creation section.
      final com.google.gwt.user.client.ui.Button editButton = (com.google.gwt.user.client.ui.Button) GWT.create(com.google.gwt.user.client.ui.Button.class);
      // Setup section.
      editButton.setHTML(template_html2().asString());


      this.owner.editButton = editButton;

      return editButton;
    }

    /**
     * Getter for removeButton called 1 times. Type: DEFAULT. Build precedence: 3.
     */
    private com.google.gwt.user.client.ui.Button get_removeButton() {
      return build_removeButton();
    }
    private com.google.gwt.user.client.ui.Button build_removeButton() {
      // Creation section.
      final com.google.gwt.user.client.ui.Button removeButton = (com.google.gwt.user.client.ui.Button) GWT.create(com.google.gwt.user.client.ui.Button.class);
      // Setup section.
      removeButton.setHTML(template_html3().asString());


      this.owner.removeButton = removeButton;

      return removeButton;
    }

    /**
     * Getter for refreshButton called 1 times. Type: DEFAULT. Build precedence: 3.
     */
    private com.google.gwt.user.client.ui.Button get_refreshButton() {
      return build_refreshButton();
    }
    private com.google.gwt.user.client.ui.Button build_refreshButton() {
      // Creation section.
      final com.google.gwt.user.client.ui.Button refreshButton = (com.google.gwt.user.client.ui.Button) GWT.create(com.google.gwt.user.client.ui.Button.class);
      // Setup section.
      refreshButton.setHTML(template_html4().asString());


      this.owner.refreshButton = refreshButton;

      return refreshButton;
    }

    /**
     * Getter for table called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.cellview.client.CellTable get_table() {
      return build_table();
    }
    private com.google.gwt.user.cellview.client.CellTable build_table() {
      // Creation section.
      final com.google.gwt.user.cellview.client.CellTable table = (com.google.gwt.user.cellview.client.CellTable) GWT.create(com.google.gwt.user.cellview.client.CellTable.class);
      // Setup section.
      table.setWidth("100%");


      this.owner.table = table;

      return table;
    }

    /**
     * Getter for detailsPanel called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private org.xdi.oxd.license.admin.client.ui.CustomerDetailsPanel get_detailsPanel() {
      return build_detailsPanel();
    }
    private org.xdi.oxd.license.admin.client.ui.CustomerDetailsPanel build_detailsPanel() {
      // Creation section.
      final org.xdi.oxd.license.admin.client.ui.CustomerDetailsPanel detailsPanel = (org.xdi.oxd.license.admin.client.ui.CustomerDetailsPanel) GWT.create(org.xdi.oxd.license.admin.client.ui.CustomerDetailsPanel.class);
      // Setup section.


      this.owner.detailsPanel = detailsPanel;

      return detailsPanel;
    }
  }
}
