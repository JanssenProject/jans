/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2.util;

import com.unboundid.util.StaticUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;
import java.util.TimeZone;

/**
 * Contains helper methods to convert between dates in ISO format and LDAP generalized time syntax.
 * <p>Examples of ISO-formated dates:</p>
 * <ul>
 *     <li>2011-12-03T10:15:30</li>
 *     <li>2011-12-03T10:15:30.4+01:00</li>
 * </ul>
 * <p>Equivalent dates in generalized time format:</p>
 * <ul>
 *     <li>20111203101530.000Z</li>
 *     <li>20111203111530.400Z</li>
 * </ul>
 */
/*
 * Created by jgomer on 2017-08-23.
 */
public class DateUtil {

    private DateUtil() {
    }

    public static Long ISOToMillis(String strDate) {

        TemporalAccessor ta;
        try {
            ta = ZonedDateTime.parse(strDate);
        } catch (Exception e) {
            try {
                LocalDateTime.parse(strDate);
                //Assume local zone...
                String zoneId = ZoneOffset.ofTotalSeconds(TimeZone.getDefault().getRawOffset() / 1000).toString();
                ta = ZonedDateTime.parse(strDate + zoneId);
            } catch (Exception e1) {
                return null;
            }
        }

        try {
            return Instant.from(ta).toEpochMilli();
        } catch (Exception e) {
            return null;
        }

    }

    /**
     * Converts a string representation of a date (expected to follow the pattern of DateTime XML schema data type) to a
     * string representation of a date in LDAP generalized time syntax (see RFC 4517 section 3.3.13).
     * <p><code>xsd:dateTime</code> is equivalent to ISO-8601 format, namely, <code>yyyy-MM-dd'T'HH:mm:ss.SSSZZ</code></p>
     *
     * @param strDate A string date in ISO format.
     * @return A String representing a date in generalized time syntax. If the date passed as parameter did not adhere to
     * xsd:dateTime, the returned value is null
     */
    public static String ISOToGeneralizedStringDate(String strDate) {
        return Optional.ofNullable(ISOToMillis(strDate)).map(StaticUtils::encodeGeneralizedTime).orElse(null);
    }

    /**
     * Converts a string representing a date (in the LDAP generalized time syntax) to an ISO-8601 formatted string date.
     *
     * @param strDate A string date in generalized time syntax (see RFC 4517 section 3.3.13)
     * @return A string representation of a date in ISO format. If the date passed as parameter did not adhere to generalized
     * time syntax, null is returned.
     */
    public static String generalizedToISOStringDate(String strDate) {

        try {
            return millisToISOString(StaticUtils.decodeGeneralizedTime(strDate).getTime());
        } catch (Exception e) {
            return null;
        }

    }

    /**
     * Returns a string representation of a date in ISO format based on a number of milliseconds elapsed from "the epoch",
     * namely January 1, 1970, 00:00:00 GMT.
     *
     * @param millis Number of milliseconds
     * @return An ISO-formatted string date
     */
    public static String millisToISOString(long millis) {
        //Useful for SCIM-client
        return Instant.ofEpochMilli(millis).toString();
    }


    /**
     * Takes an ISO date and converts to one in UTC+0 time zone (but he timezone suffix/offset is supressed in the result)
     * @param value A string representing a date in ISO-8601, eg 2007-12-03T10:15:30+01:00
     * @return A string representing the same input date (eg 2007-12-03T09:15:30)
     */
    public static String gluuCouchbaseISODate(String value) {

        try {
            String gluuISODate = ZonedDateTime.parse(value).format(DateTimeFormatter.ISO_INSTANT);
            return gluuISODate.substring(0, gluuISODate.length() - 1);  // Drop Z
            //What? ask yurem...
            //https://github.com/GluuFederation/oxCore/commit/ed24e0d4387076b0089a86246c6ac82fcac14c4a
        } catch (Exception e) {
            try {
                LocalDateTime.parse(value);
                //already in required format
                return value;
            } catch (Exception e1) {
                return null;
            }
        }

    }

}
