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

public class AddLicenseCryptDialog_MyUiBinderImpl implements UiBinder<com.google.gwt.user.client.ui.Widget, org.xdi.oxd.license.admin.client.dialogs.AddLicenseCryptDialog>, org.xdi.oxd.license.admin.client.dialogs.AddLicenseCryptDialog.MyUiBinder {

  interface Template extends SafeHtmlTemplates {
    @Template("<h4>Name:</h4>")
    SafeHtml html1();
     
    @Template("<br>")
    SafeHtml html2();
     
    @Template("Generate keys and passwords")
    SafeHtml html3();
     
    @Template("<h4>Private key:</h4>")
    SafeHtml html4();
     
    @Template("<h4>Public key:</h4>")
    SafeHtml html5();
     
    @Template("<h4>Private password:</h4>")
    SafeHtml html6();
     
    @Template("<h4>Public password:</h4>")
    SafeHtml html7();
     
    @Template("<h4>License password:</h4>")
    SafeHtml html8();
     
    @Template("OK")
    SafeHtml html9();
     
    @Template("Close")
    SafeHtml html10();
     
  }

  Template template = GWT.create(Template.class);


  public com.google.gwt.user.client.ui.Widget createAndBindUi(final org.xdi.oxd.license.admin.client.dialogs.AddLicenseCryptDialog owner) {


    return new Widgets(owner).get_dialogContent();
  }

  /**
   * Encapsulates the access to all inner widgets
   */
  class Widgets {
    private final org.xdi.oxd.license.admin.client.dialogs.AddLicenseCryptDialog owner;


    public Widgets(final org.xdi.oxd.license.admin.client.dialogs.AddLicenseCryptDialog owner) {
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
    SafeHtml template_html8() {
      return template.html8();
    }
    SafeHtml template_html9() {
      return template.html9();
    }
    SafeHtml template_html10() {
      return template.html10();
    }

    /**
     * Getter for clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay called 0 times. Type: GENERATED_BUNDLE. Build precedence: 1.
     */
    private org.xdi.oxd.license.admin.client.dialogs.AddLicenseCryptDialog_MyUiBinderImpl_GenBundle get_clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay() {
      return build_clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay();
    }
    private org.xdi.oxd.license.admin.client.dialogs.AddLicenseCryptDialog_MyUiBinderImpl_GenBundle build_clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay() {
      // Creation section.
      final org.xdi.oxd.license.admin.client.dialogs.AddLicenseCryptDialog_MyUiBinderImpl_GenBundle clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay = (org.xdi.oxd.license.admin.client.dialogs.AddLicenseCryptDialog_MyUiBinderImpl_GenBundle) GWT.create(org.xdi.oxd.license.admin.client.dialogs.AddLicenseCryptDialog_MyUiBinderImpl_GenBundle.class);
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
      dialogContent.add(get_f_HTMLPanel2());
      dialogContent.add(get_generateButton());
      dialogContent.add(get_f_HTML3());
      dialogContent.add(get_privateKey());
      dialogContent.add(get_f_HTML4());
      dialogContent.add(get_publicKey());
      dialogContent.add(get_f_HTML5());
      dialogContent.add(get_privatePassword());
      dialogContent.add(get_f_HTML6());
      dialogContent.add(get_publicPassword());
      dialogContent.add(get_f_HTML7());
      dialogContent.add(get_licensePassword());
      dialogContent.add(get_errorMessage());
      dialogContent.add(get_f_HorizontalPanel8());
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
     * Getter for f_HTMLPanel2 called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.HTMLPanel get_f_HTMLPanel2() {
      return build_f_HTMLPanel2();
    }
    private com.google.gwt.user.client.ui.HTMLPanel build_f_HTMLPanel2() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTMLPanel f_HTMLPanel2 = new com.google.gwt.user.client.ui.HTMLPanel(template_html2().asString());
      // Setup section.


      return f_HTMLPanel2;
    }

    /**
     * Getter for generateButton called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.Button get_generateButton() {
      return build_generateButton();
    }
    private com.google.gwt.user.client.ui.Button build_generateButton() {
      // Creation section.
      final com.google.gwt.user.client.ui.Button generateButton = (com.google.gwt.user.client.ui.Button) GWT.create(com.google.gwt.user.client.ui.Button.class);
      // Setup section.
      generateButton.setHTML(template_html3().asString());


      this.owner.generateButton = generateButton;

      return generateButton;
    }

    /**
     * Getter for f_HTML3 called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.HTML get_f_HTML3() {
      return build_f_HTML3();
    }
    private com.google.gwt.user.client.ui.HTML build_f_HTML3() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTML f_HTML3 = (com.google.gwt.user.client.ui.HTML) GWT.create(com.google.gwt.user.client.ui.HTML.class);
      // Setup section.
      f_HTML3.setHTML(template_html4().asString());


      return f_HTML3;
    }

    /**
     * Getter for privateKey called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.HTML get_privateKey() {
      return build_privateKey();
    }
    private com.google.gwt.user.client.ui.HTML build_privateKey() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTML privateKey = (com.google.gwt.user.client.ui.HTML) GWT.create(com.google.gwt.user.client.ui.HTML.class);
      // Setup section.


      this.owner.privateKey = privateKey;

      return privateKey;
    }

    /**
     * Getter for f_HTML4 called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.HTML get_f_HTML4() {
      return build_f_HTML4();
    }
    private com.google.gwt.user.client.ui.HTML build_f_HTML4() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTML f_HTML4 = (com.google.gwt.user.client.ui.HTML) GWT.create(com.google.gwt.user.client.ui.HTML.class);
      // Setup section.
      f_HTML4.setHTML(template_html5().asString());


      return f_HTML4;
    }

    /**
     * Getter for publicKey called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.HTML get_publicKey() {
      return build_publicKey();
    }
    private com.google.gwt.user.client.ui.HTML build_publicKey() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTML publicKey = (com.google.gwt.user.client.ui.HTML) GWT.create(com.google.gwt.user.client.ui.HTML.class);
      // Setup section.


      this.owner.publicKey = publicKey;

      return publicKey;
    }

    /**
     * Getter for f_HTML5 called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.HTML get_f_HTML5() {
      return build_f_HTML5();
    }
    private com.google.gwt.user.client.ui.HTML build_f_HTML5() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTML f_HTML5 = (com.google.gwt.user.client.ui.HTML) GWT.create(com.google.gwt.user.client.ui.HTML.class);
      // Setup section.
      f_HTML5.setHTML(template_html6().asString());


      return f_HTML5;
    }

    /**
     * Getter for privatePassword called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.HTML get_privatePassword() {
      return build_privatePassword();
    }
    private com.google.gwt.user.client.ui.HTML build_privatePassword() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTML privatePassword = (com.google.gwt.user.client.ui.HTML) GWT.create(com.google.gwt.user.client.ui.HTML.class);
      // Setup section.


      this.owner.privatePassword = privatePassword;

      return privatePassword;
    }

    /**
     * Getter for f_HTML6 called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.HTML get_f_HTML6() {
      return build_f_HTML6();
    }
    private com.google.gwt.user.client.ui.HTML build_f_HTML6() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTML f_HTML6 = (com.google.gwt.user.client.ui.HTML) GWT.create(com.google.gwt.user.client.ui.HTML.class);
      // Setup section.
      f_HTML6.setHTML(template_html7().asString());


      return f_HTML6;
    }

    /**
     * Getter for publicPassword called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.HTML get_publicPassword() {
      return build_publicPassword();
    }
    private com.google.gwt.user.client.ui.HTML build_publicPassword() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTML publicPassword = (com.google.gwt.user.client.ui.HTML) GWT.create(com.google.gwt.user.client.ui.HTML.class);
      // Setup section.


      this.owner.publicPassword = publicPassword;

      return publicPassword;
    }

    /**
     * Getter for f_HTML7 called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.HTML get_f_HTML7() {
      return build_f_HTML7();
    }
    private com.google.gwt.user.client.ui.HTML build_f_HTML7() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTML f_HTML7 = (com.google.gwt.user.client.ui.HTML) GWT.create(com.google.gwt.user.client.ui.HTML.class);
      // Setup section.
      f_HTML7.setHTML(template_html8().asString());


      return f_HTML7;
    }

    /**
     * Getter for licensePassword called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.HTML get_licensePassword() {
      return build_licensePassword();
    }
    private com.google.gwt.user.client.ui.HTML build_licensePassword() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTML licensePassword = (com.google.gwt.user.client.ui.HTML) GWT.create(com.google.gwt.user.client.ui.HTML.class);
      // Setup section.


      this.owner.licensePassword = licensePassword;

      return licensePassword;
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
     * Getter for f_HorizontalPanel8 called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.HorizontalPanel get_f_HorizontalPanel8() {
      return build_f_HorizontalPanel8();
    }
    private com.google.gwt.user.client.ui.HorizontalPanel build_f_HorizontalPanel8() {
      // Creation section.
      final com.google.gwt.user.client.ui.HorizontalPanel f_HorizontalPanel8 = (com.google.gwt.user.client.ui.HorizontalPanel) GWT.create(com.google.gwt.user.client.ui.HorizontalPanel.class);
      // Setup section.
      f_HorizontalPanel8.add(get_okButton());
      f_HorizontalPanel8.add(get_closeButton());


      return f_HorizontalPanel8;
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
      okButton.setHTML(template_html9().asString());


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
      closeButton.setHTML(template_html10().asString());


      this.owner.closeButton = closeButton;

      return closeButton;
    }
  }
}
