package org.xdi.oxd.server;

import org.apache.commons.lang.StringUtils;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.common.ErrorResponse;

import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.util.List;

import static junit.framework.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/08/2013
 */

public class TestUtils {

    private TestUtils() {
    }

    public static void notEmpty(String str) {
        assertTrue(StringUtils.isNotBlank(str));
    }

    public static void notEmpty(List<String> str) {
        assertTrue(str != null && !str.isEmpty() && StringUtils.isNotBlank(str.get(0)));
    }

    public static ErrorResponse asError(WebApplicationException e) throws IOException {
        final Object entity = e.getResponse().getEntity();
        System.out.println(entity);
        return CoreUtils.createJsonMapper().readValue((String) entity, ErrorResponse.class);
    }
}
