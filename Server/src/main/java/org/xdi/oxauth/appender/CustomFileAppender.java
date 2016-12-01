package org.xdi.oxauth.appender;

import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.xdi.oxauth.util.ServerUtil;

/**
 * Created by eugeniuparvan on 11/29/16.
 */
public class CustomFileAppender extends DailyRollingFileAppender {

    private boolean appendMacAddress;

    @Override
    protected void subAppend(LoggingEvent event) {
        String modifiedMessage;
        if (appendMacAddress)
            modifiedMessage = String.format("MAC Address: %s. %s", ServerUtil.getMACAddressOrNull(), event.getMessage());
        else
            modifiedMessage = event.getMessage().toString();

        LoggingEvent modifiedEvent = new LoggingEvent(event.getFQNOfLoggerClass(), event.getLogger(), event.getTimeStamp(), event.getLevel(), modifiedMessage,
                event.getThreadName(), event.getThrowableInformation(), event.getNDC(), event.getLocationInformation(),
                event.getProperties());

        super.subAppend(modifiedEvent);
    }

    public boolean isAppendMacAddress() {
        return appendMacAddress;
    }

    public void setAppendMacAddress(boolean appendMacAddress) {
        this.appendMacAddress = appendMacAddress;
    }
}