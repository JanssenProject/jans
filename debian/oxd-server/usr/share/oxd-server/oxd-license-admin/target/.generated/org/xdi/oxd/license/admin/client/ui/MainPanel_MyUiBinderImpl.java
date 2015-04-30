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

public class MainPanel_MyUiBinderImpl implements UiBinder<com.google.gwt.user.client.ui.Widget, org.xdi.oxd.license.admin.client.ui.MainPanel>, org.xdi.oxd.license.admin.client.ui.MainPanel.MyUiBinder {

  interface Template extends SafeHtmlTemplates {
    @Template("<h2>License Management</h2> <div style='width:100%'></div>")
    SafeHtml html1();
     
    @Template("Logout")
    SafeHtml html2();
     
    @Template("<b>License Crypt</b>")
    SafeHtml html3();
     
    @Template("<b>Customer</b>")
    SafeHtml html4();
     
  }

  Template template = GWT.create(Template.class);


  public com.google.gwt.user.client.ui.Widget createAndBindUi(final org.xdi.oxd.license.admin.client.ui.MainPanel owner) {


    return new Widgets(owner).get_rootPanel();
  }

  /**
   * Encapsulates the access to all inner widgets
   */
  class Widgets {
    private final org.xdi.oxd.license.admin.client.ui.MainPanel owner;


    public Widgets(final org.xdi.oxd.license.admin.client.ui.MainPanel owner) {
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
    private org.xdi.oxd.license.admin.client.ui.MainPanel_MyUiBinderImpl_GenBundle get_clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay() {
      return build_clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay();
    }
    private org.xdi.oxd.license.admin.client.ui.MainPanel_MyUiBinderImpl_GenBundle build_clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay() {
      // Creation section.
      final org.xdi.oxd.license.admin.client.ui.MainPanel_MyUiBinderImpl_GenBundle clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay = (org.xdi.oxd.license.admin.client.ui.MainPanel_MyUiBinderImpl_GenBundle) GWT.create(org.xdi.oxd.license.admin.client.ui.MainPanel_MyUiBinderImpl_GenBundle.class);
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
      rootPanel.addNorth(get_f_HorizontalPanel1(), 56);
      rootPanel.add(get_f_TabLayoutPanel3());


      this.owner.rootPanel = rootPanel;

      return rootPanel;
    }

    /**
     * Getter for f_HorizontalPanel1 called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.HorizontalPanel get_f_HorizontalPanel1() {
      return build_f_HorizontalPanel1();
    }
    private com.google.gwt.user.client.ui.HorizontalPanel build_f_HorizontalPanel1() {
      // Creation section.
      final com.google.gwt.user.client.ui.HorizontalPanel f_HorizontalPanel1 = (com.google.gwt.user.client.ui.HorizontalPanel) GWT.create(com.google.gwt.user.client.ui.HorizontalPanel.class);
      // Setup section.
      f_HorizontalPanel1.add(get_f_HTMLPanel2());
      f_HorizontalPanel1.add(get_logoutButton());


      return f_HorizontalPanel1;
    }

    /**
     * Getter for f_HTMLPanel2 called 1 times. Type: DEFAULT. Build precedence: 3.
     */
    private com.google.gwt.user.client.ui.HTMLPanel get_f_HTMLPanel2() {
      return build_f_HTMLPanel2();
    }
    private com.google.gwt.user.client.ui.HTMLPanel build_f_HTMLPanel2() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTMLPanel f_HTMLPanel2 = new com.google.gwt.user.client.ui.HTMLPanel(template_html1().asString());
      // Setup section.


      return f_HTMLPanel2;
    }

    /**
     * Getter for logoutButton called 1 times. Type: DEFAULT. Build precedence: 3.
     */
    private com.google.gwt.user.client.ui.Button get_logoutButton() {
      return build_logoutButton();
    }
    private com.google.gwt.user.client.ui.Button build_logoutButton() {
      // Creation section.
      final com.google.gwt.user.client.ui.Button logoutButton = (com.google.gwt.user.client.ui.Button) GWT.create(com.google.gwt.user.client.ui.Button.class);
      // Setup section.
      logoutButton.setHTML(template_html2().asString());


      this.owner.logoutButton = logoutButton;

      return logoutButton;
    }

    /**
     * Getter for f_TabLayoutPanel3 called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.TabLayoutPanel get_f_TabLayoutPanel3() {
      return build_f_TabLayoutPanel3();
    }
    private com.google.gwt.user.client.ui.TabLayoutPanel build_f_TabLayoutPanel3() {
      // Creation section.
      final com.google.gwt.user.client.ui.TabLayoutPanel f_TabLayoutPanel3 = new com.google.gwt.user.client.ui.TabLayoutPanel(3, com.google.gwt.dom.client.Style.Unit.EM);
      // Setup section.
      f_TabLayoutPanel3.add(get_licenseCryptTab(), template_html3().asString(), true);
      f_TabLayoutPanel3.add(get_customerTab(), template_html4().asString(), true);


      return f_TabLayoutPanel3;
    }

    /**
     * Getter for licenseCryptTab called 1 times. Type: DEFAULT. Build precedence: 3.
     */
    private org.xdi.oxd.license.admin.client.ui.LicenseCryptTab get_licenseCryptTab() {
      return build_licenseCryptTab();
    }
    private org.xdi.oxd.license.admin.client.ui.LicenseCryptTab build_licenseCryptTab() {
      // Creation section.
      final org.xdi.oxd.license.admin.client.ui.LicenseCryptTab licenseCryptTab = (org.xdi.oxd.license.admin.client.ui.LicenseCryptTab) GWT.create(org.xdi.oxd.license.admin.client.ui.LicenseCryptTab.class);
      // Setup section.


      this.owner.licenseCryptTab = licenseCryptTab;

      return licenseCryptTab;
    }

    /**
     * Getter for customerTab called 1 times. Type: DEFAULT. Build precedence: 3.
     */
    private org.xdi.oxd.license.admin.client.ui.CustomerTab get_customerTab() {
      return build_customerTab();
    }
    private org.xdi.oxd.license.admin.client.ui.CustomerTab build_customerTab() {
      // Creation section.
      final org.xdi.oxd.license.admin.client.ui.CustomerTab customerTab = (org.xdi.oxd.license.admin.client.ui.CustomerTab) GWT.create(org.xdi.oxd.license.admin.client.ui.CustomerTab.class);
      // Setup section.


      this.owner.customerTab = customerTab;

      return customerTab;
    }
  }
}
