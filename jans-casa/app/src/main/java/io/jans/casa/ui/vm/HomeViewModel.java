package io.jans.casa.ui.vm;

import io.jans.casa.core.SessionContext;
import io.jans.casa.core.pojo.BrowserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.AfterCompose;;
import org.zkoss.bind.annotation.ContextParam;
import org.zkoss.bind.annotation.ContextType;
import org.zkoss.bind.annotation.Init;
import org.zkoss.json.JSONObject;
import org.zkoss.zk.au.out.AuInvoke;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.select.Selectors;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;

import java.time.ZoneOffset;
import java.util.Optional;

/**
 * This class is employed to store in session some user settings.
 * @author jgomer
 */
public class HomeViewModel {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @WireVariable
    private SessionContext sessionContext;

    @Init
    public void init() {
        Clients.response(new AuInvoke("sendBrowserData"));
    }

    @AfterCompose
    public void afterCompose(@ContextParam(ContextType.VIEW) Component view) {
        Selectors.wireEventListeners(view, this);
    }

    @Listen("onData=#message")
    public void notified(Event evt) {

        JSONObject jsonObject = Optional.ofNullable(evt.getData()).map(JSONObject.class::cast).orElse(null);
        if (jsonObject != null) {
            logger.trace("Browser data is {} ", jsonObject.toJSONString());

            updateOffset(jsonObject.get("offset"));
            updateScreenWidth(jsonObject.get("screenWidth"));

            boolean mobile = Optional.ofNullable(jsonObject.get("isMobile")).map(Boolean.class::cast).orElse(false);
            logger.trace("Detected browser is {} mobile", mobile ? "" : "not");
            updateBrowserInfo(jsonObject.get("name"), jsonObject.get("version"), mobile);
        }

        //reloads this page so the navigation flow proceeds (see HomeInitiator class)
        Executions.sendRedirect(null);

    }

    private void updateOffset(Object value) {

        try {
            if (sessionContext.getZoneOffset() == null) {
                int offset = (int) value;
                ZoneOffset zoffset = ZoneOffset.ofTotalSeconds(offset);
                sessionContext.setZoneOffset(zoffset);
                logger.trace("Time offset for session is {}", zoffset.toString());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    private void updateScreenWidth(Object width) {

        try {
            int w = (int) width;
            sessionContext.setScreenWidth(w);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    private void updateBrowserInfo(Object browserName, Object browserVersion, boolean mobile) {

        try {
            if (sessionContext.getBrowser() == null) {
                BrowserInfo binfo = new BrowserInfo();
                sessionContext.setBrowser(binfo);

                binfo.setMobile(mobile);
                binfo.setName(browserName.toString());

                String version = browserVersion.toString();
                int browserVer = version.indexOf(".");
                if (browserVer > 0) {
                    browserVer = Integer.parseInt(version.substring(0, browserVer));
                }
                binfo.setMainVersion(browserVer);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

}
