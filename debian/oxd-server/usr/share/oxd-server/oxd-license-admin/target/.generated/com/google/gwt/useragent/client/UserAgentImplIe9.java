package com.google.gwt.useragent.client;

public class UserAgentImplIe9 implements com.google.gwt.useragent.client.UserAgent {
  
  public native String getRuntimeValue() /*-{
    var ua = navigator.userAgent.toLowerCase();
    var makeVersion = function(result) {
      return (parseInt(result[1]) * 1000) + parseInt(result[2]);
    };
    if ((function() { 
      return (ua.indexOf('webkit') != -1);
})()) return 'safari';
    if ((function() { 
      return (ua.indexOf('msie') != -1 && ($doc.documentMode >= 10));
})()) return 'ie10';
    if ((function() { 
      return (ua.indexOf('msie') != -1 && ($doc.documentMode >= 9));
})()) return 'ie9';
    if ((function() { 
      return (ua.indexOf('msie') != -1 && ($doc.documentMode >= 8));
})()) return 'ie8';
    if ((function() { 
      return (ua.indexOf('gecko') != -1);
})()) return 'gecko1_8';
    return 'unknown';
  }-*/;
  
  
  public String getCompileTimeValue() {
    return "ie9";
  }
}
