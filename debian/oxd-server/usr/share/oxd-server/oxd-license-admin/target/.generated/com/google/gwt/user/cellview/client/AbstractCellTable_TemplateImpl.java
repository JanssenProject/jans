package com.google.gwt.user.cellview.client;

public class AbstractCellTable_TemplateImpl implements com.google.gwt.user.cellview.client.AbstractCellTable.Template {
  
  public com.google.gwt.safehtml.shared.SafeHtml div(com.google.gwt.safehtml.shared.SafeHtml arg0) {
    StringBuilder sb = new java.lang.StringBuilder();
    sb.append("<div style=\"outline:none;\">");
    sb.append(arg0.asString());
    sb.append("</div>");
return new com.google.gwt.safehtml.shared.OnlyToBeUsedInGeneratedCodeStringBlessedAsSafeHtml(sb.toString());
}

public com.google.gwt.safehtml.shared.SafeHtml tbody(com.google.gwt.safehtml.shared.SafeHtml arg0) {
StringBuilder sb = new java.lang.StringBuilder();
sb.append("<table><tbody>");
sb.append(arg0.asString());
sb.append("</tbody></table>");
return new com.google.gwt.safehtml.shared.OnlyToBeUsedInGeneratedCodeStringBlessedAsSafeHtml(sb.toString());
}

public com.google.gwt.safehtml.shared.SafeHtml td(java.lang.String arg0,com.google.gwt.safehtml.shared.SafeHtml arg1) {
StringBuilder sb = new java.lang.StringBuilder();
sb.append("<td class=\"");
sb.append(com.google.gwt.safehtml.shared.SafeHtmlUtils.htmlEscape(arg0));
sb.append("\">");
sb.append(arg1.asString());
sb.append("</td>");
return new com.google.gwt.safehtml.shared.OnlyToBeUsedInGeneratedCodeStringBlessedAsSafeHtml(sb.toString());
}

public com.google.gwt.safehtml.shared.SafeHtml tdBothAlign(java.lang.String arg0,java.lang.String arg1,java.lang.String arg2,com.google.gwt.safehtml.shared.SafeHtml arg3) {
StringBuilder sb = new java.lang.StringBuilder();
sb.append("<td class=\"");
sb.append(com.google.gwt.safehtml.shared.SafeHtmlUtils.htmlEscape(arg0));
sb.append("\" align=\"");
sb.append(com.google.gwt.safehtml.shared.SafeHtmlUtils.htmlEscape(arg1));
sb.append("\" valign=\"");
sb.append(com.google.gwt.safehtml.shared.SafeHtmlUtils.htmlEscape(arg2));
sb.append("\">");
sb.append(arg3.asString());
sb.append("</td>");
return new com.google.gwt.safehtml.shared.OnlyToBeUsedInGeneratedCodeStringBlessedAsSafeHtml(sb.toString());
}

public com.google.gwt.safehtml.shared.SafeHtml tdHorizontalAlign(java.lang.String arg0,java.lang.String arg1,com.google.gwt.safehtml.shared.SafeHtml arg2) {
StringBuilder sb = new java.lang.StringBuilder();
sb.append("<td class=\"");
sb.append(com.google.gwt.safehtml.shared.SafeHtmlUtils.htmlEscape(arg0));
sb.append("\" align=\"");
sb.append(com.google.gwt.safehtml.shared.SafeHtmlUtils.htmlEscape(arg1));
sb.append("\">");
sb.append(arg2.asString());
sb.append("</td>");
return new com.google.gwt.safehtml.shared.OnlyToBeUsedInGeneratedCodeStringBlessedAsSafeHtml(sb.toString());
}

public com.google.gwt.safehtml.shared.SafeHtml tdVerticalAlign(java.lang.String arg0,java.lang.String arg1,com.google.gwt.safehtml.shared.SafeHtml arg2) {
StringBuilder sb = new java.lang.StringBuilder();
sb.append("<td class=\"");
sb.append(com.google.gwt.safehtml.shared.SafeHtmlUtils.htmlEscape(arg0));
sb.append("\" valign=\"");
sb.append(com.google.gwt.safehtml.shared.SafeHtmlUtils.htmlEscape(arg1));
sb.append("\">");
sb.append(arg2.asString());
sb.append("</td>");
return new com.google.gwt.safehtml.shared.OnlyToBeUsedInGeneratedCodeStringBlessedAsSafeHtml(sb.toString());
}

public com.google.gwt.safehtml.shared.SafeHtml tfoot(com.google.gwt.safehtml.shared.SafeHtml arg0) {
StringBuilder sb = new java.lang.StringBuilder();
sb.append("<table><tfoot>");
sb.append(arg0.asString());
sb.append("</tfoot></table>");
return new com.google.gwt.safehtml.shared.OnlyToBeUsedInGeneratedCodeStringBlessedAsSafeHtml(sb.toString());
}

public com.google.gwt.safehtml.shared.SafeHtml thead(com.google.gwt.safehtml.shared.SafeHtml arg0) {
StringBuilder sb = new java.lang.StringBuilder();
sb.append("<table><thead>");
sb.append(arg0.asString());
sb.append("</thead></table>");
return new com.google.gwt.safehtml.shared.OnlyToBeUsedInGeneratedCodeStringBlessedAsSafeHtml(sb.toString());
}

public com.google.gwt.safehtml.shared.SafeHtml tr(java.lang.String arg0,com.google.gwt.safehtml.shared.SafeHtml arg1) {
StringBuilder sb = new java.lang.StringBuilder();
sb.append("<tr onclick=\"\" class=\"");
sb.append(com.google.gwt.safehtml.shared.SafeHtmlUtils.htmlEscape(arg0));
sb.append("\">");
sb.append(arg1.asString());
sb.append("</tr>");
return new com.google.gwt.safehtml.shared.OnlyToBeUsedInGeneratedCodeStringBlessedAsSafeHtml(sb.toString());
}
}
