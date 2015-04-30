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

public class TextAreaDialog_MyUiBinderImpl implements UiBinder<com.google.gwt.user.client.ui.Widget, org.xdi.oxd.license.admin.client.dialogs.TextAreaDialog>, org.xdi.oxd.license.admin.client.dialogs.TextAreaDialog.MyUiBinder {

  interface Template extends SafeHtmlTemplates {
    @Template("OK")
    SafeHtml html1();
     
    @Template("Close")
    SafeHtml html2();
     
  }

  Template template = GWT.create(Template.class);


  public com.google.gwt.user.client.ui.Widget createAndBindUi(final org.xdi.oxd.license.admin.client.dialogs.TextAreaDialog owner) {


    return new Widgets(owner).get_dialogContent();
  }

  /**
   * Encapsulates the access to all inner widgets
   */
  class Widgets {
    private final org.xdi.oxd.license.admin.client.dialogs.TextAreaDialog owner;


    public Widgets(final org.xdi.oxd.license.admin.client.dialogs.TextAreaDialog owner) {
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
    private org.xdi.oxd.license.admin.client.dialogs.TextAreaDialog_MyUiBinderImpl_GenBundle get_clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay() {
      return build_clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay();
    }
    private org.xdi.oxd.license.admin.client.dialogs.TextAreaDialog_MyUiBinderImpl_GenBundle build_clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay() {
      // Creation section.
      final org.xdi.oxd.license.admin.client.dialogs.TextAreaDialog_MyUiBinderImpl_GenBundle clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay = (org.xdi.oxd.license.admin.client.dialogs.TextAreaDialog_MyUiBinderImpl_GenBundle) GWT.create(org.xdi.oxd.license.admin.client.dialogs.TextAreaDialog_MyUiBinderImpl_GenBundle.class);
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
      dialogContent.add(get_textArea());
      dialogContent.add(get_f_HorizontalPanel1());
      dialogContent.setWidth("400px");


      this.owner.dialogContent = dialogContent;

      return dialogContent;
    }

    /**
     * Getter for textArea called 1 times. Type: DEFAULT. Build precedence: 2.
     */
    private com.google.gwt.user.client.ui.TextArea get_textArea() {
      return build_textArea();
    }
    private com.google.gwt.user.client.ui.TextArea build_textArea() {
      // Creation section.
      final com.google.gwt.user.client.ui.TextArea textArea = (com.google.gwt.user.client.ui.TextArea) GWT.create(com.google.gwt.user.client.ui.TextArea.class);
      // Setup section.
      textArea.setHeight("400px");
      textArea.setWidth("100%");


      this.owner.textArea = textArea;

      return textArea;
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
      f_HorizontalPanel1.add(get_okButton());
      f_HorizontalPanel1.add(get_closeButton());
      f_HorizontalPanel1.setHeight("50px");


      return f_HorizontalPanel1;
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
      okButton.setHTML(template_html1().asString());


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
      closeButton.setHTML(template_html2().asString());


      this.owner.closeButton = closeButton;

      return closeButton;
    }
  }
}
