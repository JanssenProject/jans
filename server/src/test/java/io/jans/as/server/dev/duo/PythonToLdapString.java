/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.dev.duo;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jans.as.persistence.model.configuration.CustomProperty;
import io.jans.as.persistence.model.configuration.IDPAuthConf;
import org.apache.commons.io.IOUtils;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 28/12/2012
 */

public class PythonToLdapString /* extends BaseComponentTest */ {
    public static final String SCRIPT_FIELD_NAME = "script.__$__customAuthenticationScript__$__";
    // @Override
    // public void beforeClass() {
    //
    // }
    //
    // @Override
    // public void afterClass() {
    //
    // }

    // VIEW:
    // u:\own\java\opendj-2.4.4\OpenDJ\bat\ldapsearch.bat -h localhost -p 1636
    // -Z -X -D "cn=directory manager" -w secret -b "ou=configuration,o=jans"
    // "inum=@!1111!0002!4907"
    // MODIFY:
    // u:\own\java\opendj-2.4.4\OpenDJ\bat\ldapmodify.bat -h localhost -p 1636
    // -Z -X -D "cn=directory manager" -w secret -f
    // U:\own\project\oxAuth\Server\src\test\java\org\xdi\oxauth\dev\duo\script.ldif

    @Test
    public void test() throws IOException {
        final File f = new File("U:\\own\\project\\oxAuth\\Server\\integrations\\duo\\ExternalAuthenticator.py");
        final String pythonScript = IOUtils.toString(new FileInputStream(f));
        System.out.println("Python script: \n" + pythonScript);

        final List<CustomProperty> fields = new ArrayList<CustomProperty>();
        fields.add(createAttribute("property.duo_host", "api-fa928e64.duosecurity.com"));
        fields.add(createAttribute("property.duo_ikey", "DIT2906CETIMKHE1QND8"));
        fields.add(createAttribute("property.duo_skey", "cjNn9R4QvQV0R2Mynw2CauCUesojuR3cPGDoVygM"));
        fields.add(createAttribute("property.duo_akey", "6f88ca3dea7ac88a514cdef7c18021678ccaf0e1"));
        fields.add(createAttribute(SCRIPT_FIELD_NAME, pythonScript));

        final IDPAuthConf c = new IDPAuthConf();
        c.setType("customAuthentication");
        c.setName("duo");
        c.setEnabled(true);
        c.setLevel(1);
        c.setPriority(1);
        c.setFields(fields);

        final String json = getJSONString(c);
        System.out.println("JSON: \n" + json);

        final IDPAuthConf fromJson = fromJSON(json);
        for (CustomProperty attr : fromJson.getFields()) {
            if (attr.getName().equals(SCRIPT_FIELD_NAME)) {
                System.out.println("Script from json: \n" + attr.getValues().get(0));
            }
        }
    }

    private CustomProperty createAttribute(String name, String value) {
        final List<String> v = new ArrayList<>();
        v.add(value);

        final CustomProperty result = new CustomProperty();
        result.setName(name);
        result.setValues(v);
        return result;
    }

    private String getJSONString(IDPAuthConf conf) throws IOException {
        StringWriter sw = new StringWriter();
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(sw, conf);
        return sw.toString();
    }

    private IDPAuthConf fromJSON(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, IDPAuthConf.class);
    }
}
