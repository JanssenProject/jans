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

public class LicenseCryptDetailsPanel_MyUiBinderImpl implements UiBinder<com.google.gwt.user.client.ui.Widget, org.xdi.oxd.license.admin.client.ui.LicenseCryptDetailsPanel>, org.xdi.oxd.license.admin.client.ui.LicenseCryptDetailsPanel.MyUiBinder {

  interface Template extends SafeHtmlTemplates {
    @Template("<h2>License ID Details</h2>")
    SafeHtml html1();
     
    @Template("Generate License Ids")
    SafeHtml html2();
     
    @Template("Edit (Auto-update)")
    SafeHtml html3();
     
    @Template("Remove")
    SafeHtml html4();
     
    @Template("Refresh")
    SafeHtml html5();
     
    @Template("Show IDs for copy")
    SafeHtml html6();
     
    @Template("<h3>Name:</h3>")
    SafeHtml html7();
     
    @Template("<h3>License Id count:</h3>")
    SafeHtml html8();
     
    @Template("<h3>Private password:</h3>")
    SafeHtml html9();
     
    @Template("<h3>Public password:</h3>")
    SafeHtml html10();
     
    @Template("<h3>License password:</h3>")
    SafeHtml html11();
     
    @Template("<h3>Private key:</h3>")
    SafeHtml html12();
     
    @Template("<h3>Public key:</h3>")
    SafeHtml html13();
     
  }

  Template template = GWT.create(Template.class);


  public com.google.gwt.user.client.ui.Widget createAndBindUi(final org.xdi.oxd.license.admin.client.ui.LicenseCryptDetailsPanel owner) {


    return new Widgets(owner).get_rootPanel();
  }

  /**
   * Encapsulates the access to all inner widgets
   */
  class Widgets {
    private final org.xdi.oxd.license.admin.client.ui.LicenseCryptDetailsPanel owner;


    public Widgets(final org.xdi.oxd.license.admin.client.ui.LicenseCryptDetailsPanel owner) {
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
    SafeHtml template_html11() {
      return template.html11();
    }
    SafeHtml template_html12() {
      return template.html12();
    }
    SafeHtml template_html13() {
      return template.html13();
    }

    /**
     * Getter for clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay called 0 times. Type: GENERATED_BUNDLE. Build precedence: 1.
     */
    private org.xdi.oxd.license.admin.client.ui.LicenseCryptDetailsPanel_MyUiBinderImpl_GenBundle get_clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay() {
      return build_clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay();
    }
    private org.xdi.oxd.license.admin.client.ui.LicenseCryptDetailsPanel_MyUiBinderImpl_GenBundle build_clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay() {
      // Creation section.
      final org.xdi.oxd.license.admin.client.ui.LicenseCryptDetailsPanel_MyUiBinderImpl_GenBundle clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay = (org.xdi.oxd.license.admin.client.ui.LicenseCryptDetailsPanel_MyUiBinderImpl_GenBundle) GWT.create(org.xdi.oxd.license.admin.client.ui.LicenseCryptDetailsPanel_MyUiBinderImpl_GenBundle.class);
      // Setup section.


      return clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay;
    }

    /**
     * Getter for rootPanel called 1 times. Type: DEFAULT. Build precedence: 1.
     */
    private com.google.gwt.user.client.ui.HorizontalPanel get_rootPanel() {
      return build_rootPanel();
    }
    private com.google.gwt.user.client.ui.HorizontalPanel build_rootPanel() {
      // Creation section.
      final com.google.gwt.user.client.ui.HorizontalPanel rootPanel = (com.google.gwt.user.client.ui.HorizontalPanel) GWT.create(com.google.gwt.user.client.ui.HorizontalPanel.class);
      // Setup section.
      rootPanel.add(get_f_VerticalPanel1());
      rootPanel.add(get_f_VerticalPanel4());
      rootPanel.addStyleName("detailsPanel");
      rootPanel.setWidth("100%");


      this.owner.rootPanel = rootPanel;

      return rootPanel;
    }

    /**
     * Getter for f_VerticalPanel1 called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.VerticalPanel get_f_VerticalPanel1() {
      return build_f_VerticalPanel1();
    }
    private com.google.gwt.user.client.ui.VerticalPanel build_f_VerticalPanel1() {
      // Creation section.
      final com.google.gwt.user.client.ui.VerticalPanel f_VerticalPanel1 = (com.google.gwt.user.client.ui.VerticalPanel) GWT.create(com.google.gwt.user.client.ui.VerticalPanel.class);
      // Setup section.
      f_VerticalPanel1.add(get_f_HTML2());
      f_VerticalPanel1.add(get_f_FlowPanel3());
      f_VerticalPanel1.add(get_licenseIds());
      f_VerticalPanel1.setWidth("100%");


      return f_VerticalPanel1;
    }

    /**
     * Getter for f_HTML2 called 1 times. Type: DEFAULT. Build precedence: 3.
     */
    private com.google.gwt.user.client.ui.HTML get_f_HTML2() {
      return build_f_HTML2();
    }
    private com.google.gwt.user.client.ui.HTML build_f_HTML2() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTML f_HTML2 = (com.google.gwt.user.client.ui.HTML) GWT.create(com.google.gwt.user.client.ui.HTML.class);
      // Setup section.
      f_HTML2.setHTML(template_html1().asString());


      return f_HTML2;
    }

    /**
     * Getter for f_FlowPanel3 called 1 times. Type: DEFAULT. Build precedence: 3.
     */
    private com.google.gwt.user.client.ui.FlowPanel get_f_FlowPanel3() {
      return build_f_FlowPanel3();
    }
    private com.google.gwt.user.client.ui.FlowPanel build_f_FlowPanel3() {
      // Creation section.
      final com.google.gwt.user.client.ui.FlowPanel f_FlowPanel3 = (com.google.gwt.user.client.ui.FlowPanel) GWT.create(com.google.gwt.user.client.ui.FlowPanel.class);
      // Setup section.
      f_FlowPanel3.add(get_generateLicenseIdButton());
      f_FlowPanel3.add(get_editButton());
      f_FlowPanel3.add(get_removeButton());
      f_FlowPanel3.add(get_refreshButton());
      f_FlowPanel3.add(get_copyIds());


      return f_FlowPanel3;
    }

    /**
     * Getter for generateLicenseIdButton called 1 times. Type: DEFAULT. Build precedence: 4.
     */
    private com.google.gwt.user.client.ui.Button get_generateLicenseIdButton() {
      return build_generateLicenseIdButton();
    }
    private com.google.gwt.user.client.ui.Button build_generateLicenseIdButton() {
      // Creation section.
      final com.google.gwt.user.client.ui.Button generateLicenseIdButton = (com.google.gwt.user.client.ui.Button) GWT.create(com.google.gwt.user.client.ui.Button.class);
      // Setup section.
      generateLicenseIdButton.setHTML(template_html2().asString());
      generateLicenseIdButton.setEnabled(false);


      this.owner.generateLicenseIdButton = generateLicenseIdButton;

      return generateLicenseIdButton;
    }

    /**
     * Getter for editButton called 1 times. Type: DEFAULT. Build precedence: 4.
     */
    private com.google.gwt.user.client.ui.Button get_editButton() {
      return build_editButton();
    }
    private com.google.gwt.user.client.ui.Button build_editButton() {
      // Creation section.
      final com.google.gwt.user.client.ui.Button editButton = (com.google.gwt.user.client.ui.Button) GWT.create(com.google.gwt.user.client.ui.Button.class);
      // Setup section.
      editButton.setHTML(template_html3().asString());
      editButton.setEnabled(false);


      this.owner.editButton = editButton;

      return editButton;
    }

    /**
     * Getter for removeButton called 1 times. Type: DEFAULT. Build precedence: 4.
     */
    private com.google.gwt.user.client.ui.Button get_removeButton() {
      return build_removeButton();
    }
    private com.google.gwt.user.client.ui.Button build_removeButton() {
      // Creation section.
      final com.google.gwt.user.client.ui.Button removeButton = (com.google.gwt.user.client.ui.Button) GWT.create(com.google.gwt.user.client.ui.Button.class);
      // Setup section.
      removeButton.setHTML(template_html4().asString());
      removeButton.setEnabled(false);


      this.owner.removeButton = removeButton;

      return removeButton;
    }

    /**
     * Getter for refreshButton called 1 times. Type: DEFAULT. Build precedence: 4.
     */
    private com.google.gwt.user.client.ui.Button get_refreshButton() {
      return build_refreshButton();
    }
    private com.google.gwt.user.client.ui.Button build_refreshButton() {
      // Creation section.
      final com.google.gwt.user.client.ui.Button refreshButton = (com.google.gwt.user.client.ui.Button) GWT.create(com.google.gwt.user.client.ui.Button.class);
      // Setup section.
      refreshButton.setHTML(template_html5().asString());


      this.owner.refreshButton = refreshButton;

      return refreshButton;
    }

    /**
     * Getter for copyIds called 1 times. Type: DEFAULT. Build precedence: 4.
     */
    private com.google.gwt.user.client.ui.Button get_copyIds() {
      return build_copyIds();
    }
    private com.google.gwt.user.client.ui.Button build_copyIds() {
      // Creation section.
      final com.google.gwt.user.client.ui.Button copyIds = (com.google.gwt.user.client.ui.Button) GWT.create(com.google.gwt.user.client.ui.Button.class);
      // Setup section.
      copyIds.setHTML(template_html6().asString());


      this.owner.copyIds = copyIds;

      return copyIds;
    }

    /**
     * Getter for licenseIds called 1 times. Type: DEFAULT. Build precedence: 3.
     */
    private com.google.gwt.user.cellview.client.CellTable get_licenseIds() {
      return build_licenseIds();
    }
    private com.google.gwt.user.cellview.client.CellTable build_licenseIds() {
      // Creation section.
      final com.google.gwt.user.cellview.client.CellTable licenseIds = (com.google.gwt.user.cellview.client.CellTable) GWT.create(com.google.gwt.user.cellview.client.CellTable.class);
      // Setup section.


      this.owner.licenseIds = licenseIds;

      return licenseIds;
    }

    /**
     * Getter for f_VerticalPanel4 called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.VerticalPanel get_f_VerticalPanel4() {
      return build_f_VerticalPanel4();
    }
    private com.google.gwt.user.client.ui.VerticalPanel build_f_VerticalPanel4() {
      // Creation section.
      final com.google.gwt.user.client.ui.VerticalPanel f_VerticalPanel4 = (com.google.gwt.user.client.ui.VerticalPanel) GWT.create(com.google.gwt.user.client.ui.VerticalPanel.class);
      // Setup section.
      f_VerticalPanel4.add(get_f_HTML5());
      f_VerticalPanel4.add(get_nameField());
      f_VerticalPanel4.add(get_f_HTML6());
      f_VerticalPanel4.add(get_licenseIdCount());
      f_VerticalPanel4.add(get_f_HTML7());
      f_VerticalPanel4.add(get_privatePassword());
      f_VerticalPanel4.add(get_f_HTML8());
      f_VerticalPanel4.add(get_publicPassword());
      f_VerticalPanel4.add(get_f_HTML9());
      f_VerticalPanel4.add(get_licensePassword());
      f_VerticalPanel4.add(get_f_HTML10());
      f_VerticalPanel4.add(get_privateKey());
      f_VerticalPanel4.add(get_f_HTML11());
      f_VerticalPanel4.add(get_publicKey());
      f_VerticalPanel4.setWidth("200px");


      return f_VerticalPanel4;
    }

    /**
     * Getter for f_HTML5 called 1 times. Type: DEFAULT. Build precedence: 3.
     */
    private com.google.gwt.user.client.ui.HTML get_f_HTML5() {
      return build_f_HTML5();
    }
    private com.google.gwt.user.client.ui.HTML build_f_HTML5() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTML f_HTML5 = (com.google.gwt.user.client.ui.HTML) GWT.create(com.google.gwt.user.client.ui.HTML.class);
      // Setup section.
      f_HTML5.setHTML(template_html7().asString());


      return f_HTML5;
    }

    /**
     * Getter for nameField called 1 times. Type: DEFAULT. Build precedence: 3.
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
     * Getter for f_HTML6 called 1 times. Type: DEFAULT. Build precedence: 3.
     */
    private com.google.gwt.user.client.ui.HTML get_f_HTML6() {
      return build_f_HTML6();
    }
    private com.google.gwt.user.client.ui.HTML build_f_HTML6() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTML f_HTML6 = (com.google.gwt.user.client.ui.HTML) GWT.create(com.google.gwt.user.client.ui.HTML.class);
      // Setup section.
      f_HTML6.setHTML(template_html8().asString());


      return f_HTML6;
    }

    /**
     * Getter for licenseIdCount called 1 times. Type: DEFAULT. Build precedence: 3.
     */
    private com.google.gwt.user.client.ui.HTML get_licenseIdCount() {
      return build_licenseIdCount();
    }
    private com.google.gwt.user.client.ui.HTML build_licenseIdCount() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTML licenseIdCount = (com.google.gwt.user.client.ui.HTML) GWT.create(com.google.gwt.user.client.ui.HTML.class);
      // Setup section.


      this.owner.licenseIdCount = licenseIdCount;

      return licenseIdCount;
    }

    /**
     * Getter for f_HTML7 called 1 times. Type: DEFAULT. Build precedence: 3.
     */
    private com.google.gwt.user.client.ui.HTML get_f_HTML7() {
      return build_f_HTML7();
    }
    private com.google.gwt.user.client.ui.HTML build_f_HTML7() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTML f_HTML7 = (com.google.gwt.user.client.ui.HTML) GWT.create(com.google.gwt.user.client.ui.HTML.class);
      // Setup section.
      f_HTML7.setHTML(template_html9().asString());


      return f_HTML7;
    }

    /**
     * Getter for privatePassword called 1 times. Type: DEFAULT. Build precedence: 3.
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
     * Getter for f_HTML8 called 1 times. Type: DEFAULT. Build precedence: 3.
     */
    private com.google.gwt.user.client.ui.HTML get_f_HTML8() {
      return build_f_HTML8();
    }
    private com.google.gwt.user.client.ui.HTML build_f_HTML8() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTML f_HTML8 = (com.google.gwt.user.client.ui.HTML) GWT.create(com.google.gwt.user.client.ui.HTML.class);
      // Setup section.
      f_HTML8.setHTML(template_html10().asString());


      return f_HTML8;
    }

    /**
     * Getter for publicPassword called 1 times. Type: DEFAULT. Build precedence: 3.
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
     * Getter for f_HTML9 called 1 times. Type: DEFAULT. Build precedence: 3.
     */
    private com.google.gwt.user.client.ui.HTML get_f_HTML9() {
      return build_f_HTML9();
    }
    private com.google.gwt.user.client.ui.HTML build_f_HTML9() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTML f_HTML9 = (com.google.gwt.user.client.ui.HTML) GWT.create(com.google.gwt.user.client.ui.HTML.class);
      // Setup section.
      f_HTML9.setHTML(template_html11().asString());


      return f_HTML9;
    }

    /**
     * Getter for licensePassword called 1 times. Type: DEFAULT. Build precedence: 3.
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
     * Getter for f_HTML10 called 1 times. Type: DEFAULT. Build precedence: 3.
     */
    private com.google.gwt.user.client.ui.HTML get_f_HTML10() {
      return build_f_HTML10();
    }
    private com.google.gwt.user.client.ui.HTML build_f_HTML10() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTML f_HTML10 = (com.google.gwt.user.client.ui.HTML) GWT.create(com.google.gwt.user.client.ui.HTML.class);
      // Setup section.
      f_HTML10.setHTML(template_html12().asString());


      return f_HTML10;
    }

    /**
     * Getter for privateKey called 1 times. Type: DEFAULT. Build precedence: 3.
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
     * Getter for f_HTML11 called 1 times. Type: DEFAULT. Build precedence: 3.
     */
    private com.google.gwt.user.client.ui.HTML get_f_HTML11() {
      return build_f_HTML11();
    }
    private com.google.gwt.user.client.ui.HTML build_f_HTML11() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTML f_HTML11 = (com.google.gwt.user.client.ui.HTML) GWT.create(com.google.gwt.user.client.ui.HTML.class);
      // Setup section.
      f_HTML11.setHTML(template_html13().asString());


      return f_HTML11;
    }

    /**
     * Getter for publicKey called 1 times. Type: DEFAULT. Build precedence: 3.
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
  }
}
