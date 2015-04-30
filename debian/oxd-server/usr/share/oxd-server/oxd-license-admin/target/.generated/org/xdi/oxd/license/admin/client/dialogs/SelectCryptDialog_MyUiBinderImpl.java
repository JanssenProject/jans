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

public class SelectCryptDialog_MyUiBinderImpl implements UiBinder<com.google.gwt.user.client.ui.Widget, org.xdi.oxd.license.admin.client.dialogs.SelectCryptDialog>, org.xdi.oxd.license.admin.client.dialogs.SelectCryptDialog.MyUiBinder {

  interface Template extends SafeHtmlTemplates {
    @Template("<br>")
    SafeHtml html1();
     
    @Template("OK")
    SafeHtml html2();
     
    @Template("Close")
    SafeHtml html3();
     
  }

  Template template = GWT.create(Template.class);


  public com.google.gwt.user.client.ui.Widget createAndBindUi(final org.xdi.oxd.license.admin.client.dialogs.SelectCryptDialog owner) {


    return new Widgets(owner).get_dialogContent();
  }

  /**
   * Encapsulates the access to all inner widgets
   */
  class Widgets {
    private final org.xdi.oxd.license.admin.client.dialogs.SelectCryptDialog owner;


    public Widgets(final org.xdi.oxd.license.admin.client.dialogs.SelectCryptDialog owner) {
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

    /**
     * Getter for clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay called 0 times. Type: GENERATED_BUNDLE. Build precedence: 1.
     */
    private org.xdi.oxd.license.admin.client.dialogs.SelectCryptDialog_MyUiBinderImpl_GenBundle get_clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay() {
      return build_clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay();
    }
    private org.xdi.oxd.license.admin.client.dialogs.SelectCryptDialog_MyUiBinderImpl_GenBundle build_clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay() {
      // Creation section.
      final org.xdi.oxd.license.admin.client.dialogs.SelectCryptDialog_MyUiBinderImpl_GenBundle clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay = (org.xdi.oxd.license.admin.client.dialogs.SelectCryptDialog_MyUiBinderImpl_GenBundle) GWT.create(org.xdi.oxd.license.admin.client.dialogs.SelectCryptDialog_MyUiBinderImpl_GenBundle.class);
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
      dialogContent.add(get_table());
      dialogContent.add(get_f_HTMLPanel1());
      dialogContent.add(get_errorMessage());
      dialogContent.add(get_f_HorizontalPanel2());
      dialogContent.setWidth("500px");


      this.owner.dialogContent = dialogContent;

      return dialogContent;
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
     * Getter for f_HTMLPanel1 called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.HTMLPanel get_f_HTMLPanel1() {
      return build_f_HTMLPanel1();
    }
    private com.google.gwt.user.client.ui.HTMLPanel build_f_HTMLPanel1() {
      // Creation section.
      final com.google.gwt.user.client.ui.HTMLPanel f_HTMLPanel1 = new com.google.gwt.user.client.ui.HTMLPanel(template_html1().asString());
      // Setup section.


      return f_HTMLPanel1;
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
     * Getter for f_HorizontalPanel2 called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.HorizontalPanel get_f_HorizontalPanel2() {
      return build_f_HorizontalPanel2();
    }
    private com.google.gwt.user.client.ui.HorizontalPanel build_f_HorizontalPanel2() {
      // Creation section.
      final com.google.gwt.user.client.ui.HorizontalPanel f_HorizontalPanel2 = (com.google.gwt.user.client.ui.HorizontalPanel) GWT.create(com.google.gwt.user.client.ui.HorizontalPanel.class);
      // Setup section.
      f_HorizontalPanel2.add(get_okButton());
      f_HorizontalPanel2.add(get_closeButton());


      return f_HorizontalPanel2;
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
      okButton.setHTML(template_html2().asString());
      okButton.setEnabled(false);


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
      closeButton.setHTML(template_html3().asString());


      this.owner.closeButton = closeButton;

      return closeButton;
    }
  }
}
