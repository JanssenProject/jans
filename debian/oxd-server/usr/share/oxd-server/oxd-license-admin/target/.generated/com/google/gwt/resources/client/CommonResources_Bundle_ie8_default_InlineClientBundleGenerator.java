package com.google.gwt.resources.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ResourcePrototype;

public class CommonResources_Bundle_ie8_default_InlineClientBundleGenerator implements com.google.gwt.resources.client.CommonResources.Bundle {
  private static CommonResources_Bundle_ie8_default_InlineClientBundleGenerator _instance0 = new CommonResources_Bundle_ie8_default_InlineClientBundleGenerator();
  private void inlineBlockStyleInitializer() {
    inlineBlockStyle = new com.google.gwt.resources.client.CommonResources.InlineBlockStyle() {
      private boolean injected;
      public boolean ensureInjected() {
        if (!injected) {
          injected = true;
          com.google.gwt.dom.client.StyleInjector.inject(getText());
          return true;
        }
        return false;
      }
      public String getName() {
        return "inlineBlockStyle";
      }
      public String getText() {
        return (".GPBYFDEH{position:" + ("relative")  + ";display:" + ("inline-block")  + ";}");
      }
      public java.lang.String inlineBlock() {
        return "GPBYFDEH";
      }
    }
    ;
  }
  private static class inlineBlockStyleInitializer {
    static {
      _instance0.inlineBlockStyleInitializer();
    }
    static com.google.gwt.resources.client.CommonResources.InlineBlockStyle get() {
      return inlineBlockStyle;
    }
  }
  public com.google.gwt.resources.client.CommonResources.InlineBlockStyle inlineBlockStyle() {
    return inlineBlockStyleInitializer.get();
  }
  private static java.util.HashMap<java.lang.String, com.google.gwt.resources.client.ResourcePrototype> resourceMap;
  private static com.google.gwt.resources.client.CommonResources.InlineBlockStyle inlineBlockStyle;
  
  public ResourcePrototype[] getResources() {
    return new ResourcePrototype[] {
      inlineBlockStyle(), 
    };
  }
  public ResourcePrototype getResource(String name) {
    if (GWT.isScript()) {
      return getResourceNative(name);
    } else {
      if (resourceMap == null) {
        resourceMap = new java.util.HashMap<java.lang.String, com.google.gwt.resources.client.ResourcePrototype>();
        resourceMap.put("inlineBlockStyle", inlineBlockStyle());
      }
      return resourceMap.get(name);
    }
  }
  private native ResourcePrototype getResourceNative(String name) /*-{
    switch (name) {
      case 'inlineBlockStyle': return this.@com.google.gwt.resources.client.CommonResources.Bundle::inlineBlockStyle()();
    }
    return null;
  }-*/;
}
