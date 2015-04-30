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

public class CustomerDetailsPanel_MyUiBinderImpl implements UiBinder<com.google.gwt.user.client.ui.Widget, org.xdi.oxd.license.admin.client.ui.CustomerDetailsPanel>, org.xdi.oxd.license.admin.client.ui.CustomerDetailsPanel.MyUiBinder {

  interface Template extends SafeHtmlTemplates {
    @Template("<h3>Name:</h3>")
    SafeHtml html1();
     
    @Template("<h3>Crypt name:</h3>")
    SafeHtml html2();
     
  }

  Template template = GWT.create(Template.class);


  public com.google.gwt.user.client.ui.Widget createAndBindUi(final org.xdi.oxd.license.admin.client.ui.CustomerDetailsPanel owner) {


    return new Widgets(owner).get_rootPanel();
  }

  /**
   * Encapsulates the access to all inner widgets
   */
  class Widgets {
    private final org.xdi.oxd.license.admin.client.ui.CustomerDetailsPanel owner;


    public Widgets(final org.xdi.oxd.license.admin.client.ui.CustomerDetailsPanel owner) {
      this.owner = owner;
    }

    SafeHtml template_html1() {
      return template.html1();
    }
    SafeHtml template_html2() {
      return template.html2();
    }

    /**
     * Getter for clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay called 0 times. Type: GENERATED_BUNDLE. Build precedence: 1.
     */
    private org.xdi.oxd.license.admin.client.ui.CustomerDetailsPanel_MyUiBinderImpl_GenBundle get_clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay() {
      return build_clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay();
    }
    private org.xdi.oxd.license.admin.client.ui.CustomerDetailsPanel_MyUiBinderImpl_GenBundle build_clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay() {
      // Creation section.
      final org.xdi.oxd.license.admin.client.ui.CustomerDetailsPanel_MyUiBinderImpl_GenBundle clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay = (org.xdi.oxd.license.admin.client.ui.CustomerDetailsPanel_MyUiBinderImpl_GenBundle) GWT.create(org.xdi.oxd.license.admin.client.ui.CustomerDetailsPanel_MyUiBinderImpl_GenBundle.class);
      // Setup section.


      return clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay;
    }

    /**
     * Getter for rootPanel called 1 times. Type: DEFAULT. Build precedence: 1.
     */
    private com.google.gwt.user.client.ui.VerticalPanel get_rootPanel() {
      return build_rootPanel();
    }
    private com.google.gwt.user.client.ui.VerticalPanel build_rootPanel() {
      // Creation section.
      final com.google.gwt.user.client.ui.VerticalPanel rootPanel = (com.google.gwt.user.client.ui.VerticalPanel) GWT.create(com.google.gwt.user.client.ui.VerticalPanel.class);
      // Setup section.
      rootPanel.add(get_f_HTML1());
      rootPanel.add(get_nameField());
      rootPanel.add(get_f_HTML2());
      rootPanel.add(get_cryptNameField());


      this.owner.rootPanel = rootPanel;

      return rootPanel;
    }

    /**
     * Getter for f_HTML1 called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.HTML get_f_HTML1() {
      return build_f_HTML1();
    }
    private com.google.gwt.user.client.ui.HTML build_f_HTML1() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTML f_HTML1 = (com.google.gwt.user.client.ui.HTML) GWT.create(com.google.gwt.user.client.ui.HTML.class);
      // Setup section.
      f_HTML1.setHTML(template_html1().asString());


      return f_HTML1;
    }

    /**
     * Getter for nameField called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.HTML get_nameField() {
      return build_nameField();
    }
    private com.google.gwt.user.client.ui.HTML build_nameField() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTML nameField = (com.google.gwt.user.client.ui.HTML) GWT.create(com.google.gwt.user.client.ui.HTML.class);
      // Setup section.


      this.owner.nameField = nameField;

      return nameField;
    }

    /**
     * Getter for f_HTML2 called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.HTML get_f_HTML2() {
      return build_f_HTML2();
    }
    private com.google.gwt.user.client.ui.HTML build_f_HTML2() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTML f_HTML2 = (com.google.gwt.user.client.ui.HTML) GWT.create(com.google.gwt.user.client.ui.HTML.class);
      // Setup section.
      f_HTML2.setHTML(template_html2().asString());


      return f_HTML2;
    }

    /**
     * Getter for cryptNameField called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.HTML get_cryptNameField() {
      return build_cryptNameField();
    }
    private com.google.gwt.user.client.ui.HTML build_cryptNameField() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTML cryptNameField = (com.google.gwt.user.client.ui.HTML) GWT.create(com.google.gwt.user.client.ui.HTML.class);
      // Setup section.


      this.owner.cryptNameField = cryptNameField;

      return cryptNameField;
    }
  }
}
