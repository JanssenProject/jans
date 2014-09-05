/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.idgen.ws.rs;


import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.python.core.PyObject;
import org.xdi.oxauth.model.common.IdType;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.util.ServerUtil;
import org.xdi.service.PythonService;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 24/06/2013
 */

@Scope(ScopeType.STATELESS)
@Name("idGenService")
@AutoCreate
public class IdGenService implements IdGenerator {

    public static final String PYTHON_CLASS_NAME = "PythonExternalIdGenerator";

    @Logger
    private Log log;
    @In
    private PythonService pythonService;
    @In
    private InumGenerator inumGenerator;

    public static IdGenService instance() {
        return ServerUtil.instance(IdGenService.class);
    }

    public String generateId(IdType p_idType, String p_idPrefix) {
        return generateId(p_idType.getType(), p_idPrefix);
    }

    @Override
    public String generateId(String p_idType, String p_idPrefix) {
        final String idGenerationScript = ConfigurationFactory.getIdGenerationScript();
        final IdGenerator pythonGenerator = createPythonGenerator(idGenerationScript);
        return pythonGenerator.generateId(p_idType, p_idPrefix);
    }

    public IdGenerator createPythonGenerator(String p_pythonScript) {
        try {
            if (StringUtils.isNotBlank(p_pythonScript)) {
                InputStream bis = null;
                try {
                    bis = new ByteArrayInputStream(p_pythonScript.getBytes(Util.UTF8_STRING_ENCODING));
                    final IdGenerator result = pythonService.loadPythonScript(bis, PYTHON_CLASS_NAME, IdGenerator.class,
                            new PyObject[]{});
                    if (result == null) {
                        log.error("Python ID Generator script does not implement IdGenerator interface or script is corrupted.");
                    }
                    return result;
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                } finally {
                    IOUtils.closeQuietly(bis);
                }
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        log.error("Failed to prepare python external ID Generator");
        log.info("Using fallback INumGenerator class.");
        // use fallback inum id generator
        return inumGenerator;
    }
}
