package org.xdi.oxd.license.admin.client.dialogs;

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

public class AddCustomerDialog_MyUiBinderImpl implements UiBinder<com.google.gwt.user.client.ui.Widget, org.xdi.oxd.license.admin.client.dialogs.AddCustomerDialog>, org.xdi.oxd.license.admin.client.dialogs.AddCustomerDialog.MyUiBinder {

  interface Template extends SafeHtmlTemplates {
    @Template("<h4>Name:</h4>")
    SafeHtml html1();
     
    @Template("<h4>Crypt:</h4>")
    SafeHtml html2();
     
    @Template("<span style='color:red;'>Warning: On crypt change all dependent license ids as well as licenses are not associated with this customer anymore. </span>")
    SafeHtml html3();
     
    @Template("Select Crypt")
    SafeHtml html4();
     
    @Template("<br>")
    SafeHtml html5();
     
    @Template("OK")
    SafeHtml html6();
     
    @Template("Close")
    SafeHtml html7();
     
  }

  Template template = GWT.create(Template.class);


  public com.google.gwt.user.client.ui.Widget createAndBindUi(final org.xdi.oxd.license.admin.client.dialogs.AddCustomerDialog owner) {


    return new Widgets(owner).get_dialogContent();
  }

  /**
   * Encapsulates the access to all inner widgets
   */
  class Widgets {
    private final org.xdi.oxd.license.admin.client.dialogs.AddCustomerDialog owner;


    public Widgets(final org.xdi.oxd.license.admin.client.dialogs.AddCustomerDialog owner) {
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
    SafeHtml template_html5() {
      return template.html5();
    }
    SafeHtml template_html6() {
      return template.html6();
    }
    SafeHtml template_html7() {
      return template.html7();
    }

    /**
     * Getter for clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay called 0 times. Type: GENERATED_BUNDLE. Build precedence: 1.
     */
    private org.xdi.oxd.license.admin.client.dialogs.AddCustomerDialog_MyUiBinderImpl_GenBundle get_clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay() {
      return build_clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay();
    }
    private org.xdi.oxd.license.admin.client.dialogs.AddCustomerDialog_MyUiBinderImpl_GenBundle build_clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay() {
      // Creation section.
      final org.xdi.oxd.license.admin.client.dialogs.AddCustomerDialog_MyUiBinderImpl_GenBundle clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay = (org.xdi.oxd.license.admin.client.dialogs.AddCustomerDialog_MyUiBinderImpl_GenBundle) GWT.create(org.xdi.oxd.license.admin.client.dialogs.AddCustomerDialog_MyUiBinderImpl_GenBundle.class);
      // Setup section.


      return clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay;
    }

    /**
     * Getter for dialogContent called 1 times. Type: DEFAULT. Build precedence: 1.
     */
    private com.google.gwt.user.client.ui.VerticalPanel get_dialogContent() {
      return build_dialogContent();
    }
    private com.google.gwt.user.client.ui.VerticalPanel build_dialogContent() {
      // Creation section.
      final com.google.gwt.user.client.ui.VerticalPanel dialogContent = (com.google.gwt.user.client.ui.VerticalPanel) GWT.create(com.google.gwt.user.client.ui.VerticalPanel.class);
      // Setup section.
      dialogContent.add(get_f_HTML1());
      dialogContent.add(get_nameField());
      dialogContent.add(get_f_HTML2());
      dialogContent.add(get_f_HorizontalPanel3());
      dialogContent.add(get_f_HTMLPanel4());
      dialogContent.add(get_errorMessage());
      dialogContent.add(get_f_HorizontalPanel5());
      dialogContent.setWidth("700px");


      this.owner.dialogContent = dialogContent;

      return dialogContent;
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
    private com.google.gwt.user.client.ui.TextBox get_nameField() {
      return build_nameField();
    }
    private com.google.gwt.user.client.ui.TextBox build_nameField() {
      // Creation section.
      final com.google.gwt.user.client.ui.TextBox nameField = (com.google.gwt.user.client.ui.TextBox) GWT.create(com.google.gwt.user.client.ui.TextBox.class);
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
     * Getter for f_HorizontalPanel3 called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.HorizontalPanel get_f_HorizontalPanel3() {
      return build_f_HorizontalPanel3();
    }
    private com.google.gwt.user.client.ui.HorizontalPanel build_f_HorizontalPanel3() {
      // Creation section.
      final com.google.gwt.user.client.ui.HorizontalPanel f_HorizontalPanel3 = (com.google.gwt.user.client.ui.HorizontalPanel) GWT.create(com.google.gwt.user.client.ui.HorizontalPanel.class);
      // Setup section.
      f_HorizontalPanel3.add(get_cryptChangeWarning());
      f_HorizontalPanel3.add(get_crypt());
      f_HorizontalPanel3.add(get_selectCryptButton());


      return f_HorizontalPanel3;
    }

    /**
     * Getter for cryptChangeWarning called 1 times. Type: DEFAULT. Build precedence: 3.
     */
    private com.google.gwt.user.client.ui.HTML get_cryptChangeWarning() {
      return build_cryptChangeWarning();
    }
    private com.google.gwt.user.client.ui.HTML build_cryptChangeWarning() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTML cryptChangeWarning = (com.google.gwt.user.client.ui.HTML) GWT.create(com.google.gwt.user.client.ui.HTML.class);
      // Setup section.
      cryptChangeWarning.setHTML(template_html3().asString());
      cryptChangeWarning.setVisible(false);


      this.owner.cryptChangeWarning = cryptChangeWarning;

      return cryptChangeWarning;
    }

    /**
     * Getter for crypt called 1 times. Type: DEFAULT. Build precedence: 3.
     */
    private com.google.gwt.user.client.ui.HTML get_crypt() {
      return build_crypt();
    }
    private com.google.gwt.user.client.ui.HTML build_crypt() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTML crypt = (com.google.gwt.user.client.ui.HTML) GWT.create(com.google.gwt.user.client.ui.HTML.class);
      // Setup section.


      this.owner.crypt = crypt;

      return crypt;
    }

    /**
     * Getter for selectCryptButton called 1 times. Type: DEFAULT. Build precedence: 3.
     */
    private com.google.gwt.user.client.ui.Button get_selectCryptButton() {
      return build_selectCryptButton();
    }
    private com.google.gwt.user.client.ui.Button build_selectCryptButton() {
      // Creation section.
      final com.google.gwt.user.client.ui.Button selectCryptButton = (com.google.gwt.user.client.ui.Button) GWT.create(com.google.gwt.user.client.ui.Button.class);
      // Setup section.
      selectCryptButton.setHTML(template_html4().asString());


      this.owner.selectCryptButton = selectCryptButton;

      return selectCryptButton;
    }

    /**
     * Getter for f_HTMLPanel4 called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.HTMLPanel get_f_HTMLPanel4() {
      return build_f_HTMLPanel4();
    }
    private com.google.gwt.user.client.ui.HTMLPanel build_f_HTMLPanel4() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTMLPanel f_HTMLPanel4 = new com.google.gwt.user.client.ui.HTMLPanel(template_html5().asString());
      // Setup section.


      return f_HTMLPanel4;
    }

    /**
     * Getter for errorMessage called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.HTML get_errorMessage() {
      return build_errorMessage();
    }
    private com.google.gwt.user.client.ui.HTML build_errorMessage() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTML errorMessage = (com.google.gwt.user.client.ui.HTML) GWT.create(com.google.gwt.user.client.ui.HTML.class);
      // Setup section.
      errorMessage.setVisible(false);


      this.owner.errorMessage = errorMessage;

      return errorMessage;
    }

    /**
     * Getter for f_HorizontalPanel5 called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.HorizontalPanel get_f_HorizontalPanel5() {
      return build_f_HorizontalPanel5();
    }
    private com.google.gwt.user.client.ui.HorizontalPanel build_f_HorizontalPanel5() {
      // Creation section.
      final com.google.gwt.user.client.ui.HorizontalPanel f_HorizontalPanel5 = (com.google.gwt.user.client.ui.HorizontalPanel) GWT.create(com.google.gwt.user.client.ui.HorizontalPanel.class);
      // Setup section.
      f_HorizontalPanel5.add(get_okButton());
      f_HorizontalPanel5.add(get_closeButton());


      return f_HorizontalPanel5;
    }

    /**
     * Getter for okButton called 1 times. Type: DEFAULT. Build precedence: 3.
     */
    private com.google.gwt.user.client.ui.Button get_okButton() {
      return build_okButton();
    }
    private com.google.gwt.user.client.ui.Button build_okButton() {
      // Creation section.
      final com.google.gwt.user.client.ui.Button okButton = (com.google.gwt.user.client.ui.Button) GWT.create(com.google.gwt.user.client.ui.Button.class);
      // Setup section.
      okButton.setHTML(template_html6().asString());


      this.owner.okButton = okButton;

      return okButton;
    }

    /**
     * Getter for closeButton called 1 times. Type: DEFAULT. Build precedence: 3.
     */
    private com.google.gwt.user.client.ui.Button get_closeButton() {
      return build_closeButton();
    }
    private com.google.gwt.user.client.ui.Button build_closeButton() {
      // Creation section.
      final com.google.gwt.user.client.ui.Button closeButton = (com.google.gwt.user.client.ui.Button) GWT.create(com.google.gwt.user.client.ui.Button.class);
      // Setup section.
      closeButton.setHTML(template_html7().asString());


      this.owner.closeButton = closeButton;

      return closeButton;
    }
  }
}
