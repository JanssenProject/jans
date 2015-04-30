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

public class LicenseIdMetadataDialog_MyUiBinderImpl implements UiBinder<com.google.gwt.user.client.ui.Widget, org.xdi.oxd.license.admin.client.dialogs.LicenseIdMetadataDialog>, org.xdi.oxd.license.admin.client.dialogs.LicenseIdMetadataDialog.MyUiBinder {

  interface Template extends SafeHtmlTemplates {
    @Template("<h3>Number of license ids:</h3>")
    SafeHtml html1();
     
    @Template("<h3>Name:</h3>")
    SafeHtml html2();
     
    @Template("<h3>Support features:</h3>")
    SafeHtml html3();
     
    @Template("<h3>Threads count:</h3>")
    SafeHtml html4();
     
    @Template("<h3>Multi-server (not supported yet):</h3>")
    SafeHtml html5();
     
    @Template("<br>")
    SafeHtml html6();
     
    @Template("OK")
    SafeHtml html7();
     
    @Template("Close")
    SafeHtml html8();
     
  }

  Template template = GWT.create(Template.class);


  public com.google.gwt.user.client.ui.Widget createAndBindUi(final org.xdi.oxd.license.admin.client.dialogs.LicenseIdMetadataDialog owner) {


    return new Widgets(owner).get_dialogContent();
  }

  /**
   * Encapsulates the access to all inner widgets
   */
  class Widgets {
    private final org.xdi.oxd.license.admin.client.dialogs.LicenseIdMetadataDialog owner;


    public Widgets(final org.xdi.oxd.license.admin.client.dialogs.LicenseIdMetadataDialog owner) {
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

    /**
     * Getter for clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay called 0 times. Type: GENERATED_BUNDLE. Build precedence: 1.
     */
    private org.xdi.oxd.license.admin.client.dialogs.LicenseIdMetadataDialog_MyUiBinderImpl_GenBundle get_clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay() {
      return build_clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay();
    }
    private org.xdi.oxd.license.admin.client.dialogs.LicenseIdMetadataDialog_MyUiBinderImpl_GenBundle build_clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay() {
      // Creation section.
      final org.xdi.oxd.license.admin.client.dialogs.LicenseIdMetadataDialog_MyUiBinderImpl_GenBundle clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay = (org.xdi.oxd.license.admin.client.dialogs.LicenseIdMetadataDialog_MyUiBinderImpl_GenBundle) GWT.create(org.xdi.oxd.license.admin.client.dialogs.LicenseIdMetadataDialog_MyUiBinderImpl_GenBundle.class);
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
      dialogContent.add(get_numberOfLicenseIdsLabel());
      dialogContent.add(get_numberOfLicenseIds());
      dialogContent.add(get_f_HTML1());
      dialogContent.add(get_licenseName());
      dialogContent.add(get_f_HTML2());
      dialogContent.add(get_licenseFeatures());
      dialogContent.add(get_f_HTML3());
      dialogContent.add(get_threadsCount());
      dialogContent.add(get_f_HTML4());
      dialogContent.add(get_multiServer());
      dialogContent.add(get_f_HTMLPanel5());
      dialogContent.add(get_errorMessage());
      dialogContent.add(get_f_HorizontalPanel6());
      dialogContent.setWidth("200px");


      this.owner.dialogContent = dialogContent;

      return dialogContent;
    }

    /**
     * Getter for numberOfLicenseIdsLabel called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.HTML get_numberOfLicenseIdsLabel() {
      return build_numberOfLicenseIdsLabel();
    }
    private com.google.gwt.user.client.ui.HTML build_numberOfLicenseIdsLabel() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTML numberOfLicenseIdsLabel = (com.google.gwt.user.client.ui.HTML) GWT.create(com.google.gwt.user.client.ui.HTML.class);
      // Setup section.
      numberOfLicenseIdsLabel.setHTML(template_html1().asString());


      this.owner.numberOfLicenseIdsLabel = numberOfLicenseIdsLabel;

      return numberOfLicenseIdsLabel;
    }

    /**
     * Getter for numberOfLicenseIds called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.TextBox get_numberOfLicenseIds() {
      return build_numberOfLicenseIds();
    }
    private com.google.gwt.user.client.ui.TextBox build_numberOfLicenseIds() {
      // Creation section.
      final com.google.gwt.user.client.ui.TextBox numberOfLicenseIds = (com.google.gwt.user.client.ui.TextBox) GWT.create(com.google.gwt.user.client.ui.TextBox.class);
      // Setup section.


      this.owner.numberOfLicenseIds = numberOfLicenseIds;

      return numberOfLicenseIds;
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
      f_HTML1.setHTML(template_html2().asString());


      return f_HTML1;
    }

    /**
     * Getter for licenseName called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.TextBox get_licenseName() {
      return build_licenseName();
    }
    private com.google.gwt.user.client.ui.TextBox build_licenseName() {
      // Creation section.
      final com.google.gwt.user.client.ui.TextBox licenseName = (com.google.gwt.user.client.ui.TextBox) GWT.create(com.google.gwt.user.client.ui.TextBox.class);
      // Setup section.


      this.owner.licenseName = licenseName;

      return licenseName;
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
      f_HTML2.setHTML(template_html3().asString());


      return f_HTML2;
    }

    /**
     * Getter for licenseFeatures called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.ListBox get_licenseFeatures() {
      return build_licenseFeatures();
    }
    private com.google.gwt.user.client.ui.ListBox build_licenseFeatures() {
      // Creation section.
      final com.google.gwt.user.client.ui.ListBox licenseFeatures = (com.google.gwt.user.client.ui.ListBox) GWT.create(com.google.gwt.user.client.ui.ListBox.class);
      // Setup section.
      licenseFeatures.setMultipleSelect(true);
      licenseFeatures.setWidth("300px");
      licenseFeatures.setVisibleItemCount(6);


      this.owner.licenseFeatures = licenseFeatures;

      return licenseFeatures;
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
     * Getter for threadsCount called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.TextBox get_threadsCount() {
      return build_threadsCount();
    }
    private com.google.gwt.user.client.ui.TextBox build_threadsCount() {
      // Creation section.
      final com.google.gwt.user.client.ui.TextBox threadsCount = (com.google.gwt.user.client.ui.TextBox) GWT.create(com.google.gwt.user.client.ui.TextBox.class);
      // Setup section.


      this.owner.threadsCount = threadsCount;

      return threadsCount;
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
     * Getter for multiServer called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.CheckBox get_multiServer() {
      return build_multiServer();
    }
    private com.google.gwt.user.client.ui.CheckBox build_multiServer() {
      // Creation section.
      final com.google.gwt.user.client.ui.CheckBox multiServer = (com.google.gwt.user.client.ui.CheckBox) GWT.create(com.google.gwt.user.client.ui.CheckBox.class);
      // Setup section.
      multiServer.setChecked(false);


      this.owner.multiServer = multiServer;

      return multiServer;
    }

    /**
     * Getter for f_HTMLPanel5 called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.HTMLPanel get_f_HTMLPanel5() {
      return build_f_HTMLPanel5();
    }
    private com.google.gwt.user.client.ui.HTMLPanel build_f_HTMLPanel5() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTMLPanel f_HTMLPanel5 = new com.google.gwt.user.client.ui.HTMLPanel(template_html6().asString());
      // Setup section.


      return f_HTMLPanel5;
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
     * Getter for f_HorizontalPanel6 called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.HorizontalPanel get_f_HorizontalPanel6() {
      return build_f_HorizontalPanel6();
    }
    private com.google.gwt.user.client.ui.HorizontalPanel build_f_HorizontalPanel6() {
      // Creation section.
      final com.google.gwt.user.client.ui.HorizontalPanel f_HorizontalPanel6 = (com.google.gwt.user.client.ui.HorizontalPanel) GWT.create(com.google.gwt.user.client.ui.HorizontalPanel.class);
      // Setup section.
      f_HorizontalPanel6.add(get_okButton());
      f_HorizontalPanel6.add(get_closeButton());


      return f_HorizontalPanel6;
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
      okButton.setHTML(template_html7().asString());


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
      closeButton.setHTML(template_html8().asString());


      this.owner.closeButton = closeButton;

      return closeButton;
    }
  }
}
