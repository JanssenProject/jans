package io.jans.ca.plugin.adminui.utils;

import com.google.common.base.Joiner;
import io.jans.ca.plugin.adminui.model.config.LicenseConfiguration;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.*;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.slf4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collection;
import java.util.Locale;

public class CommonUtils {
    @Inject
    Logger log;
    public static final DateTimeFormatter LS_DATE_FORMAT = DateTimeFormatter.ofPattern("E, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);


    public static String joinAndUrlEncode(Collection<String> list) throws UnsupportedEncodingException {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return encode(Joiner.on(" ").join(list));
    }

    public static String encode(String str) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, "UTF-8");
    }

    public static String getFormattedDate() {
        ZonedDateTime currentDateTime = ZonedDateTime.now();
        ZonedDateTime gmtTime = currentDateTime.withZoneSameInstant(ZoneId.of("GMT"));
        DateTimeFormatter currentTimeFormatter = LS_DATE_FORMAT;
        return gmtTime.format(currentTimeFormatter);
    }
}