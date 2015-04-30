package com.google.gwt.user.cellview.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ResourcePrototype;

public class CellTable_Resources_default_InlineClientBundleGenerator implements com.google.gwt.user.cellview.client.CellTable.Resources {
  private static CellTable_Resources_default_InlineClientBundleGenerator _instance0 = new CellTable_Resources_default_InlineClientBundleGenerator();
  private void cellTableFooterBackgroundInitializer() {
    cellTableFooterBackground = new com.google.gwt.resources.client.impl.ImageResourcePrototype(
      "cellTableFooterBackground",
      com.google.gwt.safehtml.shared.UriUtils.fromTrustedString(com.google.gwt.i18n.client.LocaleInfo.getCurrentLocale().isRTL() ?externalImage_rtl : externalImage),
      0, 0, 82, 23, false, false
    );
  }
  private static class cellTableFooterBackgroundInitializer {
    static {
      _instance0.cellTableFooterBackgroundInitializer();
    }
    static com.google.gwt.resources.client.ImageResource get() {
      return cellTableFooterBackground;
    }
  }
  public com.google.gwt.resources.client.ImageResource cellTableFooterBackground() {
    return cellTableFooterBackgroundInitializer.get();
  }
  private void cellTableHeaderBackgroundInitializer() {
    cellTableHeaderBackground = new com.google.gwt.resources.client.impl.ImageResourcePrototype(
      "cellTableHeaderBackground",
      com.google.gwt.safehtml.shared.UriUtils.fromTrustedString(com.google.gwt.i18n.client.LocaleInfo.getCurrentLocale().isRTL() ?externalImage_rtl0 : externalImage0),
      0, 0, 82, 23, false, false
    );
  }
  private static class cellTableHeaderBackgroundInitializer {
    static {
      _instance0.cellTableHeaderBackgroundInitializer();
    }
    static com.google.gwt.resources.client.ImageResource get() {
      return cellTableHeaderBackground;
    }
  }
  public com.google.gwt.resources.client.ImageResource cellTableHeaderBackground() {
    return cellTableHeaderBackgroundInitializer.get();
  }
  private void cellTableLoadingInitializer() {
    cellTableLoading = new com.google.gwt.resources.client.impl.ImageResourcePrototype(
      "cellTableLoading",
      com.google.gwt.safehtml.shared.UriUtils.fromTrustedString(com.google.gwt.i18n.client.LocaleInfo.getCurrentLocale().isRTL() ?externalImage_rtl1 : externalImage1),
      0, 0, 43, 11, true, false
    );
  }
  private static class cellTableLoadingInitializer {
    static {
      _instance0.cellTableLoadingInitializer();
    }
    static com.google.gwt.resources.client.ImageResource get() {
      return cellTableLoading;
    }
  }
  public com.google.gwt.resources.client.ImageResource cellTableLoading() {
    return cellTableLoadingInitializer.get();
  }
  private void cellTableSelectedBackgroundInitializer() {
    cellTableSelectedBackground = new com.google.gwt.resources.client.impl.ImageResourcePrototype(
      "cellTableSelectedBackground",
      com.google.gwt.safehtml.shared.UriUtils.fromTrustedString(com.google.gwt.i18n.client.LocaleInfo.getCurrentLocale().isRTL() ?externalImage_rtl2 : externalImage2),
      0, 0, 82, 26, false, false
    );
  }
  private static class cellTableSelectedBackgroundInitializer {
    static {
      _instance0.cellTableSelectedBackgroundInitializer();
    }
    static com.google.gwt.resources.client.ImageResource get() {
      return cellTableSelectedBackground;
    }
  }
  public com.google.gwt.resources.client.ImageResource cellTableSelectedBackground() {
    return cellTableSelectedBackgroundInitializer.get();
  }
  private void cellTableSortAscendingInitializer() {
    cellTableSortAscending = new com.google.gwt.resources.client.impl.ImageResourcePrototype(
      "cellTableSortAscending",
      com.google.gwt.safehtml.shared.UriUtils.fromTrustedString(com.google.gwt.i18n.client.LocaleInfo.getCurrentLocale().isRTL() ?externalImage_rtl3 : externalImage3),
      0, 0, 11, 7, false, false
    );
  }
  private static class cellTableSortAscendingInitializer {
    static {
      _instance0.cellTableSortAscendingInitializer();
    }
    static com.google.gwt.resources.client.ImageResource get() {
      return cellTableSortAscending;
    }
  }
  public com.google.gwt.resources.client.ImageResource cellTableSortAscending() {
    return cellTableSortAscendingInitializer.get();
  }
  private void cellTableSortDescendingInitializer() {
    cellTableSortDescending = new com.google.gwt.resources.client.impl.ImageResourcePrototype(
      "cellTableSortDescending",
      com.google.gwt.safehtml.shared.UriUtils.fromTrustedString(com.google.gwt.i18n.client.LocaleInfo.getCurrentLocale().isRTL() ?externalImage_rtl4 : externalImage4),
      0, 0, 11, 7, false, false
    );
  }
  private static class cellTableSortDescendingInitializer {
    static {
      _instance0.cellTableSortDescendingInitializer();
    }
    static com.google.gwt.resources.client.ImageResource get() {
      return cellTableSortDescending;
    }
  }
  public com.google.gwt.resources.client.ImageResource cellTableSortDescending() {
    return cellTableSortDescendingInitializer.get();
  }
  private void cellTableStyleInitializer() {
    cellTableStyle = new com.google.gwt.user.cellview.client.CellTable.Style() {
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
        return "cellTableStyle";
      }
      public String getText() {
        return com.google.gwt.i18n.client.LocaleInfo.getCurrentLocale().isRTL() ? ((".GPBYFDEED{border-top:" + ("2px"+ " " +"solid"+ " " +"#6f7277")  + ";padding:" + ("3px"+ " " +"15px")  + ";text-align:" + ("right")  + ";color:" + ("#4b4a4a")  + ";text-shadow:" + ("#ddf"+ " " +"1px"+ " " +"1px"+ " " +"0")  + ";overflow:" + ("hidden")  + ";}.GPBYFDEFD{border-bottom:" + ("2px"+ " " +"solid"+ " " +"#6f7277")  + ";padding:" + ("3px"+ " " +"15px")  + ";text-align:" + ("right")  + ";color:" + ("#4b4a4a")  + ";text-shadow:") + (("#ddf"+ " " +"1px"+ " " +"1px"+ " " +"0")  + ";overflow:" + ("hidden")  + ";}.GPBYFDEOC{padding:" + ("2px"+ " " +"15px")  + ";overflow:" + ("hidden")  + ";}.GPBYFDEDE{cursor:" + ("pointer")  + ";cursor:" + ("hand")  + ";}.GPBYFDEDE:hover{color:" + ("#6c6b6b")  + ";}.GPBYFDEPC{background:" + ("#fff")  + ";}.GPBYFDEAD{border:" + ("2px"+ " " +"solid"+ " " +"#fff")  + ";}.GPBYFDEPD{background:" + ("#f3f7fb")  + ";}.GPBYFDEAE{border:" + ("2px"+ " " +"solid"+ " " +"#f3f7fb") ) + (";}.GPBYFDEGD{background:" + ("#eee")  + ";}.GPBYFDEHD{border:" + ("2px"+ " " +"solid"+ " " +"#eee")  + ";}.GPBYFDEJD{background:" + ("#ffc")  + ";}.GPBYFDEKD{border:" + ("2px"+ " " +"solid"+ " " +"#ffc")  + ";}.GPBYFDEBE{background:" + ("#628cd5")  + ";color:" + ("white")  + ";height:" + ("auto")  + ";overflow:" + ("auto")  + ";}.GPBYFDECE{border:" + ("2px"+ " " +"solid"+ " " +"#628cd5")  + ";}.GPBYFDEID{border:" + ("2px"+ " " +"solid"+ " " +"#d7dde8")  + ";}.GPBYFDEOD{margin:") + (("30px")  + ";}")) : ((".GPBYFDEED{border-top:" + ("2px"+ " " +"solid"+ " " +"#6f7277")  + ";padding:" + ("3px"+ " " +"15px")  + ";text-align:" + ("left")  + ";color:" + ("#4b4a4a")  + ";text-shadow:" + ("#ddf"+ " " +"1px"+ " " +"1px"+ " " +"0")  + ";overflow:" + ("hidden")  + ";}.GPBYFDEFD{border-bottom:" + ("2px"+ " " +"solid"+ " " +"#6f7277")  + ";padding:" + ("3px"+ " " +"15px")  + ";text-align:" + ("left")  + ";color:" + ("#4b4a4a")  + ";text-shadow:") + (("#ddf"+ " " +"1px"+ " " +"1px"+ " " +"0")  + ";overflow:" + ("hidden")  + ";}.GPBYFDEOC{padding:" + ("2px"+ " " +"15px")  + ";overflow:" + ("hidden")  + ";}.GPBYFDEDE{cursor:" + ("pointer")  + ";cursor:" + ("hand")  + ";}.GPBYFDEDE:hover{color:" + ("#6c6b6b")  + ";}.GPBYFDEPC{background:" + ("#fff")  + ";}.GPBYFDEAD{border:" + ("2px"+ " " +"solid"+ " " +"#fff")  + ";}.GPBYFDEPD{background:" + ("#f3f7fb")  + ";}.GPBYFDEAE{border:" + ("2px"+ " " +"solid"+ " " +"#f3f7fb") ) + (";}.GPBYFDEGD{background:" + ("#eee")  + ";}.GPBYFDEHD{border:" + ("2px"+ " " +"solid"+ " " +"#eee")  + ";}.GPBYFDEJD{background:" + ("#ffc")  + ";}.GPBYFDEKD{border:" + ("2px"+ " " +"solid"+ " " +"#ffc")  + ";}.GPBYFDEBE{background:" + ("#628cd5")  + ";color:" + ("white")  + ";height:" + ("auto")  + ";overflow:" + ("auto")  + ";}.GPBYFDECE{border:" + ("2px"+ " " +"solid"+ " " +"#628cd5")  + ";}.GPBYFDEID{border:" + ("2px"+ " " +"solid"+ " " +"#d7dde8")  + ";}.GPBYFDEOD{margin:") + (("30px")  + ";}"));
      }
      public java.lang.String cellTableCell() {
        return "GPBYFDEOC";
      }
      public java.lang.String cellTableEvenRow() {
        return "GPBYFDEPC";
      }
      public java.lang.String cellTableEvenRowCell() {
        return "GPBYFDEAD";
      }
      public java.lang.String cellTableFirstColumn() {
        return "GPBYFDEBD";
      }
      public java.lang.String cellTableFirstColumnFooter() {
        return "GPBYFDECD";
      }
      public java.lang.String cellTableFirstColumnHeader() {
        return "GPBYFDEDD";
      }
      public java.lang.String cellTableFooter() {
        return "GPBYFDEED";
      }
      public java.lang.String cellTableHeader() {
        return "GPBYFDEFD";
      }
      public java.lang.String cellTableHoveredRow() {
        return "GPBYFDEGD";
      }
      public java.lang.String cellTableHoveredRowCell() {
        return "GPBYFDEHD";
      }
      public java.lang.String cellTableKeyboardSelectedCell() {
        return "GPBYFDEID";
      }
      public java.lang.String cellTableKeyboardSelectedRow() {
        return "GPBYFDEJD";
      }
      public java.lang.String cellTableKeyboardSelectedRowCell() {
        return "GPBYFDEKD";
      }
      public java.lang.String cellTableLastColumn() {
        return "GPBYFDELD";
      }
      public java.lang.String cellTableLastColumnFooter() {
        return "GPBYFDEMD";
      }
      public java.lang.String cellTableLastColumnHeader() {
        return "GPBYFDEND";
      }
      public java.lang.String cellTableLoading() {
        return "GPBYFDEOD";
      }
      public java.lang.String cellTableOddRow() {
        return "GPBYFDEPD";
      }
      public java.lang.String cellTableOddRowCell() {
        return "GPBYFDEAE";
      }
      public java.lang.String cellTableSelectedRow() {
        return "GPBYFDEBE";
      }
      public java.lang.String cellTableSelectedRowCell() {
        return "GPBYFDECE";
      }
      public java.lang.String cellTableSortableHeader() {
        return "GPBYFDEDE";
      }
      public java.lang.String cellTableSortedHeaderAscending() {
        return "GPBYFDEEE";
      }
      public java.lang.String cellTableSortedHeaderDescending() {
        return "GPBYFDEFE";
      }
      public java.lang.String cellTableWidget() {
        return "GPBYFDEGE";
      }
    }
    ;
  }
  private static class cellTableStyleInitializer {
    static {
      _instance0.cellTableStyleInitializer();
    }
    static com.google.gwt.user.cellview.client.CellTable.Style get() {
      return cellTableStyle;
    }
  }
  public com.google.gwt.user.cellview.client.CellTable.Style cellTableStyle() {
    return cellTableStyleInitializer.get();
  }
  private static java.util.HashMap<java.lang.String, com.google.gwt.resources.client.ResourcePrototype> resourceMap;
  private static final java.lang.String externalImage = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFIAAAAXCAYAAACYuRhEAAAAj0lEQVR42u3EWwrCQBQE0d7/ekQEUUQEEQXjgxiMISI+cAW5M/los4f2swtOge4vof32NB2aYaZD/elpOlTvnqZD+co0Hc7PTNPh+Mg0HYphpsP+nmk67NpE02HbJJoOm1vQdFjXiabD6ho0HZZV0HRYXIKmw7wMmg6zsqPpMD0FTYfJMNNhfOhoOoyKoOl+PTDH5dhvR3oAAAAASUVORK5CYII=";
  private static final java.lang.String externalImage_rtl = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFIAAAAXCAYAAACYuRhEAAAAj0lEQVR42u3EWwrCQBQE0d7/ekQEUUQEEQXjgxiMISI+cAW5M/los4f2swtOge4vof32NB2aYaZD/elpOlTvnqZD+co0Hc7PTNPh+Mg0HYphpsP+nmk67NpE02HbJJoOm1vQdFjXiabD6ho0HZZV0HRYXIKmw7wMmg6zsqPpMD0FTYfJMNNhfOhoOoyKoOl+PTDH5dhvR3oAAAAASUVORK5CYII=";
  private static final java.lang.String externalImage0 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFIAAAAXCAYAAACYuRhEAAAAj0lEQVR42u3EWwrCQBQE0d7/ekQEUUQEEQXjgxiMISI+cAW5M/los4f2swtOge4vof32NB2aYaZD/elpOlTvnqZD+co0Hc7PTNPh+Mg0HYphpsP+nmk67NpE02HbJJoOm1vQdFjXiabD6ho0HZZV0HRYXIKmw7wMmg6zsqPpMD0FTYfJMNNhfOhoOoyKoOl+PTDH5dhvR3oAAAAASUVORK5CYII=";
  private static final java.lang.String externalImage_rtl0 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFIAAAAXCAYAAACYuRhEAAAAj0lEQVR42u3EWwrCQBQE0d7/ekQEUUQEEQXjgxiMISI+cAW5M/los4f2swtOge4vof32NB2aYaZD/elpOlTvnqZD+co0Hc7PTNPh+Mg0HYphpsP+nmk67NpE02HbJJoOm1vQdFjXiabD6ho0HZZV0HRYXIKmw7wMmg6zsqPpMD0FTYfJMNNhfOhoOoyKoOl+PTDH5dhvR3oAAAAASUVORK5CYII=";
  private static final java.lang.String externalImage1 = "data:image/gif;base64,R0lGODlhKwALAPEAAP///0tKSqampktKSiH/C05FVFNDQVBFMi4wAwEAAAAh/hpDcmVhdGVkIHdpdGggYWpheGxvYWQuaW5mbwAh+QQJCgAAACwAAAAAKwALAAACMoSOCMuW2diD88UKG95W88uF4DaGWFmhZid93pq+pwxnLUnXh8ou+sSz+T64oCAyTBUAACH5BAkKAAAALAAAAAArAAsAAAI9xI4IyyAPYWOxmoTHrHzzmGHe94xkmJifyqFKQ0pwLLgHa82xrekkDrIBZRQab1jyfY7KTtPimixiUsevAAAh+QQJCgAAACwAAAAAKwALAAACPYSOCMswD2FjqZpqW9xv4g8KE7d54XmMpNSgqLoOpgvC60xjNonnyc7p+VKamKw1zDCMR8rp8pksYlKorgAAIfkECQoAAAAsAAAAACsACwAAAkCEjgjLltnYmJS6Bxt+sfq5ZUyoNJ9HHlEqdCfFrqn7DrE2m7Wdj/2y45FkQ13t5itKdshFExC8YCLOEBX6AhQAADsAAAAAAAAAAAA=";
  private static final java.lang.String externalImage_rtl1 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACsAAAALCAYAAADm8XT2AAAAMElEQVR42mNgAIJly5b9J4QZ8AC66SdGIT4L6ap/1LGjjh117KhjRx1Le/1DpQYDAInICKwfCLm7AAAAAElFTkSuQmCC";
  private static final java.lang.String externalImage2 = "data:image/png;base64,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";
  private static final java.lang.String externalImage_rtl2 = "data:image/png;base64,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";
  private static final java.lang.String externalImage3 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAsAAAAHCAYAAADebrddAAAAiklEQVR42mNgwALyKrumFRf3iDAQAvmVXVVAxf/zKjq341WYV95hk1fZ+R+MK8C4HqtCkLW5FZ2PQYpyK6AaKjv/5VV1OmIozq3s3AFR0AXFUNMrO5/lV7WKI6yv6mxCksSGDyTU13Mw5JV2qeaWd54FWn0BRAMlLgPZl/NAuBKMz+dWdF0H2hwCAPwcZIjfOFLHAAAAAElFTkSuQmCC";
  private static final java.lang.String externalImage_rtl3 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAsAAAAHCAYAAADebrddAAAAiklEQVR42mNgQAPFxT0ieZVd0xiIAXkVnduBiv/nV3ZVEVJYD8T/8yqhuLzDBrvCqk5HoIJ/IEW5IA0VYPoxyFkoCvOrWsWBip4hTO2CYqCGys4dcIUJ9fUcQMEDcKuRMUxzVWcTWDFQZ0huRdd1oOB5IL4MVHAZaP1lEDu3vPMskH0BROeVdqkCAJLDZIgWLbFCAAAAAElFTkSuQmCC";
  private static final java.lang.String externalImage4 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAsAAAAHCAYAAADebrddAAAAiklEQVR42mPIrewMya3oup5X2XkeiC/nVXRezgViEDu3vPMskH0BROeVdqkyJNTXcwAlDgDxfwxcAaWrOpsYYCC/qlUcKPgMLlnZBcWd/4E272BAB0DdjkDJf2AFFRBTgfTj4uIeEQZsAKigHmE6EJd32DDgA0DF20FOyK/sqmIgBEDWAhVPwyYHAJAqZIiNwsHKAAAAAElFTkSuQmCC";
  private static final java.lang.String externalImage_rtl4 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAsAAAAHCAYAAADebrddAAAAiElEQVR42mPIK+1SzS3vPJtb0XkBROdVdl4Gsi/ngXAlGJ/Prei6nlvZGcIAAnlVnU1Awf9ABf/BNCY+kFBfz8EAA0CdOyASXVAM1/wsv6pVnAEZFBf3iACtfwxSkIuw4R/QVkcGbCCvvMMGbnUFGNcz4AP5lV1VYGdUdG5nIAYAFU8DOQtdHAD5g2SIRShEoQAAAABJRU5ErkJggg==";
  private static com.google.gwt.resources.client.ImageResource cellTableFooterBackground;
  private static com.google.gwt.resources.client.ImageResource cellTableHeaderBackground;
  private static com.google.gwt.resources.client.ImageResource cellTableLoading;
  private static com.google.gwt.resources.client.ImageResource cellTableSelectedBackground;
  private static com.google.gwt.resources.client.ImageResource cellTableSortAscending;
  private static com.google.gwt.resources.client.ImageResource cellTableSortDescending;
  private static com.google.gwt.user.cellview.client.CellTable.Style cellTableStyle;
  
  public ResourcePrototype[] getResources() {
    return new ResourcePrototype[] {
      cellTableFooterBackground(), 
      cellTableHeaderBackground(), 
      cellTableLoading(), 
      cellTableSelectedBackground(), 
      cellTableSortAscending(), 
      cellTableSortDescending(), 
      cellTableStyle(), 
    };
  }
  public ResourcePrototype getResource(String name) {
    if (GWT.isScript()) {
      return getResourceNative(name);
    } else {
      if (resourceMap == null) {
        resourceMap = new java.util.HashMap<java.lang.String, com.google.gwt.resources.client.ResourcePrototype>();
        resourceMap.put("cellTableFooterBackground", cellTableFooterBackground());
        resourceMap.put("cellTableHeaderBackground", cellTableHeaderBackground());
        resourceMap.put("cellTableLoading", cellTableLoading());
        resourceMap.put("cellTableSelectedBackground", cellTableSelectedBackground());
        resourceMap.put("cellTableSortAscending", cellTableSortAscending());
        resourceMap.put("cellTableSortDescending", cellTableSortDescending());
        resourceMap.put("cellTableStyle", cellTableStyle());
      }
      return resourceMap.get(name);
    }
  }
  private native ResourcePrototype getResourceNative(String name) /*-{
    switch (name) {
      case 'cellTableFooterBackground': return this.@com.google.gwt.user.cellview.client.CellTable.Resources::cellTableFooterBackground()();
      case 'cellTableHeaderBackground': return this.@com.google.gwt.user.cellview.client.CellTable.Resources::cellTableHeaderBackground()();
      case 'cellTableLoading': return this.@com.google.gwt.user.cellview.client.CellTable.Resources::cellTableLoading()();
      case 'cellTableSelectedBackground': return this.@com.google.gwt.user.cellview.client.CellTable.Resources::cellTableSelectedBackground()();
      case 'cellTableSortAscending': return this.@com.google.gwt.user.cellview.client.CellTable.Resources::cellTableSortAscending()();
      case 'cellTableSortDescending': return this.@com.google.gwt.user.cellview.client.CellTable.Resources::cellTableSortDescending()();
      case 'cellTableStyle': return this.@com.google.gwt.user.cellview.client.CellTable.Resources::cellTableStyle()();
    }
    return null;
  }-*/;
}
