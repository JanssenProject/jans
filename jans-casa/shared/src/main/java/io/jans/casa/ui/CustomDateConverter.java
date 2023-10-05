package io.jans.casa.ui;

import io.jans.casa.misc.WebUtils;
import org.zkoss.bind.BindContext;
import org.zkoss.bind.Converter;
import org.zkoss.web.Attributes;
import org.zkoss.zk.ui.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;

/**
 * This class is a custom ZK converter employed to display dates appropriately according to user location.
 * It uses the time offset with respect to UTC time and a formatting pattern supplied to show dates. It does not implement
 * <code>String</code> to <code>Date</code> conversion.
 * @author jgomer
 */
public class CustomDateConverter implements Converter {

    /**
     * This method is called when conversion (<code>Date</code> to <code>String</code>) is taking placing in
     * <code>.zul</code> templates.
     * @param val An object representing a date, namely a java.util.Date or a long value (milliseconds from the epoch)
     * @param comp The UI component associated to this converstion
     * @param ctx Binding context. It holds the conversion arguments: "format" and "offset" are used here
     * @return A string with the date properly formatted or null if val parameter is not a valid date
     */
    public Object coerceToUi(Object val, Component comp, BindContext ctx) {

        long timeStamp = (val instanceof Date) ? ((Date) val).getTime() : (long) val;
        if (timeStamp > 0) {

            String format = (String) ctx.getConverterArg("format");
            Object offset = ctx.getConverterArg("offset");
            ZoneId zid;

            if (offset != null && ZoneId.class.isAssignableFrom(offset.getClass())) {
                zid = (ZoneId) offset;
            } else {      //This covers the weird case in which there is no offset set
                zid = ZoneOffset.UTC;
                if (format.contains("hh") || format.contains("HH") || format.contains("mm")) {
                    format += " '(GMT)'";
                }
            }
            Instant instant = Instant.ofEpochMilli(timeStamp);
            OffsetDateTime odt = OffsetDateTime.ofInstant(instant, zid);

            HttpServletRequest request = WebUtils.getServletRequest();
            Locale locale;
            try {
                locale = Optional.ofNullable(request.getSession(false).getAttribute(Attributes.PREFERRED_LOCALE))
                        .map(Locale.class::cast).orElseThrow(Exception::new);
            } catch (Exception e) {
                locale = Optional.ofNullable(request.getLocale()).orElse(WebUtils.DEFAULT_LOCALE);
            }
            return odt.format(DateTimeFormatter.ofPattern(format, locale));
        } else {
            return null;
        }

    }

    /**
     * This method is called when converting <code>String</code> to <code>Date</code>.
     * @param val  A date in string form
     * @param comp Associated component
     * @param ctx Bind context for associate Binding and extra parameter (e.g. format)
     * @return null (conversion not implemented)
     */
    public Object coerceToBean(Object val, Component comp, BindContext ctx) {
        return null;
    }

}
