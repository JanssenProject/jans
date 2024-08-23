package io.jans.casa.ui.vm;

import io.jans.casa.core.SessionContext;
import io.jans.casa.core.pojo.BrowserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.*;
import org.zkoss.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.select.Selectors;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;

import java.util.Optional;
import java.time.ZoneOffset;

/**
 * This class is employed to store in session some user settings.
 */
public class HomeViewModel {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @WireVariable
    private SessionContext sessionContext;

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
            
            updateBrowserInfo(jsonObject.get("name"), jsonObject.get("major"), jsonObject.get("mobile"));
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

    private void updateBrowserInfo(Object browserName, Object browserVersion, Object isMobile) {

        try {
            if (sessionContext.getBrowser() == null) {
                BrowserInfo binfo = new BrowserInfo();
                sessionContext.setBrowser(binfo);

                binfo.setName(browserName.toString());
                binfo.setMainVersion(Integer.valueOf(browserVersion.toString()));
                binfo.setMobile(Boolean.parseBoolean​(isMobile.toString()));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

}
